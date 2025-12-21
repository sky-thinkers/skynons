package com.skythinkers.skynons.nons

import com.skythinkers.skynons.api.SaveSimulationRequest
import com.skythinkers.skynons.api.SimulationApiMessage
import com.skythinkers.skynons.api.SimulationStateRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.logging.Logger
import io.ktor.websocket.Frame
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException
import kotlin.io.path.absolutePathString
import kotlin.time.Clock
import kotlin.time.Instant

class ActiveNonsProcess(
    val port: Int,
    private val process: Process,
    private val client: HttpClient,
    private val scope: CoroutineScope,
    private val log: Logger = KtorSimpleLogger("NonsProcess/$port"),
) : NonsProcess {
    private val stopSemaphore: Semaphore = Semaphore(1, 1)
    private val messageQueue = Channel<Msg>(Channel.BUFFERED)

    @Volatile
    private var lastActivity = Clock.System.now()

    private var job: Job = scope.launch {
        runCatching {
            while (process.isAlive && isActive) {
                client.webSocket("ws://localhost:$port/pipe") {
                    log.trace("enter websocket loop")
                    while (!stopSemaphore.tryAcquire() && isActive) {
                        val msgRes = messageQueue.receiveCatching()
                        lastActivity = Clock.System.now()
                        if (msgRes.isFailure || stopSemaphore.tryAcquire() || !isActive) break
                        val msg = msgRes.getOrNull() ?: break
                        log.trace("Passing message of type {}", msg.msg::class.java.name)

                        sendSerialized(msg.msg)
                        log.trace("Awaiting response")

                        val response = if (msg.msg is SimulationStateRequest) {
                            runCatching { Json.decodeFromString<SimulationApiMessage>(receiveDeserialized<String>()) }
                        } else {
                            runCatching { receiveDeserialized<SimulationApiMessage>() }
                        }

                        log.trace(
                            "Got response of type {}",
                            (response.getOrNull() ?: response.exceptionOrNull()!!)::class.java.name
                        )
                        launch {
                            msg.cont.resumeWith(response)
                        }
                    }
                    log.trace("exit websocket loop")
                    send(Frame.Close("END_SIMULATION".toByteArray()))
                    delay(1000)
                    process.destroy()
                    process.waitFor(1, TimeUnit.MILLISECONDS)
                }
            }
            if (process.isAlive) {
                process.destroy()
            }
        }.onFailure {
            stop()
        }
    }

    private val overlord = scope.launch(Dispatchers.IO) {
        process.waitFor()
        if (process.exitValue() != 0) {
            val stdout = runCatching { process.inputStream.bufferedReader().use { it.readText() } }.getOrElse { it.toString() }
            log.error(
                "Process terminated with non-zero exit code ${process.exitValue()}. stderr+stdout:\n" + stdout)
        } else {
            val stdout = runCatching { process.inputStream.bufferedReader().use { it.readText() } }.getOrElse { it.toString() }
            log.trace(
                "Process terminated with exit code 0. stderr+stdout:\n$stdout"
            )
        }
    }

    fun isRunning() = job.isActive

    override fun stopIfTooOld(minPermittedLastActivityTimestamp: Instant): Boolean {
        return if (lastActivity < minPermittedLastActivityTimestamp) {
            stop()
            true
        } else {
            false
        }
    }

    override fun hibernateIfTooOld(minPermittedLastActivityTimestamp: Instant, dir: Path): SuspendedNonsProcess? {
        return if (lastActivity < minPermittedLastActivityTimestamp) {
            runBlocking {
                suspend(dir)
            }
        } else {
            null
        }
    }

    override fun stop() {
        runCatching { stopSemaphore.release() }.onFailure { return }
        messageQueue.close()
        process.destroy()
        while (true) {
            val msg = messageQueue.tryReceive()
            msg.getOrNull()?.cont?.cancel() ?: break
        }
    }

    suspend fun message(message: SimulationApiMessage): SimulationApiMessage = try {
        suspendCancellableCoroutine { cont ->
            messageQueue.trySendBlocking(Msg(message, cont)).onFailure {
                cont.resumeWithException(
                    it ?: IllegalStateException("Cannot send message. Is NoNs process still alive?")
                )
            }
            log.trace("Suspended on message of type {}", message::class.java.name)
        }
    } finally {
        log.trace("Resumed on message of type {}", message::class.java.name)
    }

    suspend fun suspend(dir: Path): SuspendedNonsProcess {
        val hibernateFile = withContext(Dispatchers.IO) {
            Files.createTempFile(dir, "simulation", ".yml")
        }
        message(SaveSimulationRequest(hibernateFile.absolutePathString()))
        stop()
        return SuspendedNonsProcess(hibernateFile)
    }

    private data class Msg(val msg: SimulationApiMessage, val cont: CancellableContinuation<SimulationApiMessage>)
}
