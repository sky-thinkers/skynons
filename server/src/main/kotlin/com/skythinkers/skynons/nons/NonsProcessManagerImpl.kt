package com.skythinkers.skynons.nons

import com.skythinkers.skynons.api.SimulationApiMessage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.ServerSocket
import java.nio.file.Files
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class NonsProcessManagerImpl(private val nonsPath: String, private val scope: CoroutineScope) : NonsProcessManager {
    private val threadLocalRandom = ThreadLocal.withInitial { SecureRandom.getInstanceStrong() }
    private val client = HttpClient(OkHttp) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }
    private val processIdToNonsProcess = ConcurrentHashMap<ProcessId, NonsProcess>()
    private val tempDir = Files.createTempDirectory("sim-hibernate")

    private val random: SecureRandom get() = threadLocalRandom.get()

    private val cleaner = scope.launch(Dispatchers.IO) {
        while (isActive) {
            delay(2.hours)
            val stopTimestamp = Clock.System.now() - 4.hours
            processIdToNonsProcess.filterValues {
                it.stopIfTooOld(stopTimestamp)
            }.forEach {
                processIdToNonsProcess.remove(it.key, it.value)
            }
        }
    }

    private val suspender = scope.launch(Dispatchers.IO) {
        while (isActive) {
            delay(1.minutes)
            val suspendTimestamp = Clock.System.now() - 2.minutes
            processIdToNonsProcess.replaceAll { _, process ->
                process.hibernateIfTooOld(suspendTimestamp, tempDir) ?: process
            }
        }
    }

    override suspend fun createSimulation(): ProcessId? {
        val process = tryStartBackend() ?: return null
        val externalId = ByteArray(32).also { random.nextBytes(it) }.toHexString()
        processIdToNonsProcess[externalId] = process
        return externalId
    }

    private suspend fun tryStartBackend(): ActiveNonsProcess? {
        repeat(5) {
            val port = findFreePort() ?: return null
            val process =
                ProcessBuilder(nonsPath, "--server-port", port.toString()).redirectErrorStream(
                    true
                ).start()
            delay(100) // let server start
            if (!process.isAlive) {
                log.error(process.inputStream.bufferedReader().use { it.readText() })
                return@repeat
            }
            return ActiveNonsProcess(port, process, client, scope)
        }

        return null
    }

    override fun checkId(id: ProcessId): Boolean {
        return processIdToNonsProcess.containsKey(id)
    }

    override tailrec suspend fun message(id: ProcessId, message: SimulationApiMessage): SimulationApiMessage {
        return when (val process = processIdToNonsProcess.getValue(id)) {
            is ActiveNonsProcess -> process.message(message)
            is SuspendedNonsProcess -> {
                val newProcess = tryStartBackend() ?: error("Failed to create new process to resume simulation")
                process.resumeInto(newProcess)
                if (!processIdToNonsProcess.replace(id, process, newProcess)) {
                    log.warn("Concurrent process resume")
                    newProcess.stop()
                    return message(id, message)
                }
                newProcess.message(message)
            }
        }
    }

    override suspend fun suspendProcess(id: ProcessId) {
        val process = processIdToNonsProcess.getValue(id)
        if (process is ActiveNonsProcess) {
            processIdToNonsProcess.replace(id, process, process.suspend(tempDir))
        }
    }

    private fun findFreePort(): Int? =
        runCatching { ServerSocket(0).use { socket -> socket.localPort } }.getOrNull()

    override fun close() {
        processIdToNonsProcess.values.forEach { process -> process.stop() }
        processIdToNonsProcess.clear()
        scope.cancel()
    }

    override fun killProcess(id: ProcessId) {
        val process = processIdToNonsProcess.remove(id)
        process?.stop()
    }

    companion object {
        private val log = KtorSimpleLogger("NonsProcessManager")
    }
}
