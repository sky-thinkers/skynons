package com.skythinkers.skynons.routing

import com.skythinkers.skynons.api.ApiMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

class NonsProcess(
    val port: Port,
    private val process: Process,
    private val client: HttpClient,
    private val scope: CoroutineScope,
) {
    private val stopSemaphore: Semaphore = Semaphore(1, 1)
    private val messageQueue = Channel<Msg>(Channel.BUFFERED)

    private var job: Job = scope.launch {
        while (process.isAlive && isActive) {
            client.webSocket("ws://localhost:$$port/pipe") {
                while (!stopSemaphore.tryAcquire() && isActive) {
                    val msgRes = messageQueue.receiveCatching()
                    if (msgRes.isFailure || stopSemaphore.tryAcquire() || !isActive) break
                    val msg = msgRes.getOrNull() ?: break

                    sendSerialized(msg.msg)
                    val response = runCatching { receiveDeserialized<ApiMessage>() }
                    msg.cont.resumeWith(response)
                }
                send(Frame.Close("END_SIMULATION".toByteArray()))
                delay(1000)
                process.destroy()
                process.waitFor(1, TimeUnit.MILLISECONDS)
            }
        }
        if (process.isAlive) {
            process.destroy()
        }
    }

    fun isRunning() = job.isActive

    fun stop() {
        runCatching { stopSemaphore.release() }.onFailure { return }
        messageQueue.close()
        process.destroy()
        while (true) {
            val msg = messageQueue.tryReceive()
            msg.getOrNull()?.cont?.cancel() ?: break
        }
    }

    suspend fun message(message: ApiMessage): ApiMessage {
        return suspendCancellableCoroutine { cont ->
            messageQueue.trySendBlocking(Msg(message, cont)).onFailure {
                cont.resumeWithException(
                    it ?: IllegalStateException("Cannot send message. Is NoNs process still alive?")
                )
            }
        }
    }

    private data class Msg(val msg: ApiMessage, val cont: CancellableContinuation<ApiMessage>)
}