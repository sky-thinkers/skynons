package com.skythinkers.skynons.routing

import com.skythinkers.skynons.api.ErrorResponseData
import com.skythinkers.skynons.api.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.reflect.TypeInfo
import io.ktor.util.reflect.typeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

typealias TokenId = String


class ContinuationManager(val conversationTimeout: Long, private val scope: CoroutineScope) {
    private val log = KtorSimpleLogger("ContinuationManager")
    private val threadLocalRandom = ThreadLocal<SecureRandom>.withInitial { SecureRandom.getInstanceStrong() }
    private val random get() = threadLocalRandom.get()

    private val tokenStore = ConcurrentHashMap<TokenId, Token>()

    private val cleaner = scope.launch {
        cleanerThread()
    }

    suspend fun tryConsumeToken(call: RoutingCall, tokenId: TokenId) {
        while (true) {
            val token = tokenStore[tokenId] ?: run {
                call.respond(HttpStatusCode.NotFound, ErrorResponseData("No such continuation token"))
                return
            }
            if (token.isReady()) {
                if (!tokenStore.remove(tokenId, token)) {
                    continue
                }
                token.consume(call)
                return
            } else {
                call.response.headers.append(Headers.CONTINUATION, Headers.CONTINUATION_KEEP)
                call.respond(HttpStatusCode.OK)
                return
            }
        }
    }

    fun newToken(call: RoutingCall): Token = Token(call)

    private fun registerToken(token: Token): TokenId {
        val id = ByteArray(32).also { random.nextBytes(it) }.toHexString()
        tokenStore[id] = token
        return id
    }

    inner class Token(private val call: WeakReference<RoutingCall>) {
        constructor(call: RoutingCall) : this(WeakReference(call))

        @Volatile
        private var response: ResponseWrapper? = null
        @Volatile
        var timestamp: Instant = Clock.System.now()

        suspend inline fun <reified T> respond(result: T) {
            respond(HttpStatusCode.OK, result)
        }

        suspend inline fun <reified T> respond(status: HttpStatusCode, result: T) {
            respond(status, result, if (result == null) null else typeInfo<T>())
        }

        suspend fun respond(status: HttpStatusCode, result: Any?, typeInfo: TypeInfo?) {
            val responseSent = try {
                val call = call.get()
                if (typeInfo == null) {
                    call?.respond(status)
                } else {
                    call?.respond(status, result, typeInfo)
                } != null
            } catch (e: Exception) {
                log.warn("Failed to respond", e)
                true
            }

            if (!responseSent) {
                log.trace("Set continuation result")
                response = ResponseWrapper(result, typeInfo, status)
            }
        }

        suspend fun toContinuation() {
            val call = call.get() ?: run {
                val e = IllegalStateException("Call expired")
                log.warn("Failed to convert to continuation", e)
                throw e
            }
            this.call.clear()
            call.response.headers.append(Headers.CONTINUATION, registerToken(this))
            call.respond(HttpStatusCode.OK)
            log.trace("Call converted to a continuation")
        }

        suspend fun consume(call: RoutingCall) {
            val response = response!!
            if (response.typeInfo == null) {
                call.respond(response.status)
            } else {
                call.respond(response.status, response.result, response.typeInfo)
            }
        }

        fun isReady(): Boolean = response != null
    }

    private suspend fun cleanerThread() {
        while (currentCoroutineContext().isActive) {
            delay(1.minutes)
            val cleanDate = Clock.System.now() - 2.minutes
            val forgetDate = Clock.System.now() - 4.hours
            tokenStore.filterValues {
                it.isReady() && it.timestamp < cleanDate || it.timestamp < forgetDate
            }.keys.forEach {
                tokenStore.remove(it)
            }
        }
    }

    private fun close() {
        scope.cancel()
    }

    private class ResponseWrapper(val result: Any?, val typeInfo: TypeInfo?, val status: HttpStatusCode)
}