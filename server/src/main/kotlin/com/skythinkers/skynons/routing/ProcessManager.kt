package com.skythinkers.skynons.routing

import com.skythinkers.skynons.api.ApiMessage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap

typealias Port = Int

class NonsProcessManager(private val nonsPath: String, private val scope: CoroutineScope) : AutoCloseable {
    private val client = HttpClient(OkHttp) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }
    private val portToNonsProcess = ConcurrentHashMap<Port, NonsProcess>()

    suspend fun createSimulation(): Port? {
        val process = tryStartBackend() ?: return null

        portToNonsProcess[process.port] = process
        return process.port
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

    fun checkPort(port: Port): Boolean {
        return portToNonsProcess.containsKey(port)
    }

    suspend fun message(port: Port, message: ApiMessage): ApiMessage {
        return portToNonsProcess.getValue(port).message(message)
    }

    private fun findFreePort(): Port? =
        runCatching { ServerSocket(0).use { socket -> socket.localPort } }.getOrNull()

    override fun close() {
        portToNonsProcess.values.forEach { process -> process.stop() }
        portToNonsProcess.clear()
    }

    companion object {
        private val log = KtorSimpleLogger("NonsProcessManager")
    }
}
