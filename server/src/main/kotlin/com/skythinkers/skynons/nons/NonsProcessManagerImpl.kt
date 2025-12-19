package com.skythinkers.skynons.nons

import com.skythinkers.skynons.api.SimulationApiMessage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.ServerSocket
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

class NonsProcessManagerImpl(private val nonsPath: String, private val scope: CoroutineScope) : NonsProcessManager {
    private val threadLocalRandom = ThreadLocal.withInitial { SecureRandom.getInstanceStrong() }
    private val client = HttpClient(OkHttp) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }
    private val processIdToNonsProcess = ConcurrentHashMap<ProcessId, NonsProcess>()

    private val random: SecureRandom get() = threadLocalRandom.get()

    private val cleaner = scope.launch {
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

    override suspend fun createSimulation(): ProcessId? {
        val process = tryStartBackend() ?: return null
        val externalId = ByteArray(32).also { random.nextBytes(it) }.toHexString()
        processIdToNonsProcess[externalId] = process
        return externalId
    }

    private suspend fun tryStartBackend(): NonsProcess? {
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
            return NonsProcess(port, process, client, scope)
        }

        return null
    }

    override fun checkId(id: ProcessId): Boolean {
        return processIdToNonsProcess.containsKey(id)
    }

    override suspend fun message(id: ProcessId, message: SimulationApiMessage): SimulationApiMessage {
        return processIdToNonsProcess.getValue(id).message(message)
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
