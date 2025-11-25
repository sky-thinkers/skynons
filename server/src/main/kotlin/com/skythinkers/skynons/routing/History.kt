package com.skythinkers.skynons.routing

import com.skythinkers.skynons.api.ErrorResponseData
import com.skythinkers.skynons.api.HistoryEntryList
import com.skythinkers.skynons.auth.UserSession
import com.skythinkers.skynons.auth.uid
import com.skythinkers.skynons.history.HistoryEntryNotFoundException
import com.skythinkers.skynons.history.HistoryManager
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.logging.Logger

fun Route.history(historyManager: HistoryManager) {
    val logger = KtorSimpleLogger("API/history")

    authenticate(UserSession.USER_SESSION) {
        get("/list") {
            val session: UserSession = call.principal<UserSession>(UserSession.USER_SESSION)!!
            val beforeId = call.parameters["beforeId"]?.let {
                it.toLongOrNull() ?: run {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponseData("when present, beforeId must be long")
                    )
                    return@get
                }
            }
            val limit = limit { return@get }

            val res = if (beforeId == null) {
                historyManager.listLastEntries(session.uid, limit)
            } else {
                historyManager.listEntriesBefore(session.uid, beforeId, limit)
            }
                .getOrElse {
                    handleHistoryError(logger, it) { return@get }
                }


            call.respond(HistoryEntryList(res))
        }

        get("/entry/{id}") {
            val session: UserSession = call.principal<UserSession>(UserSession.USER_SESSION)!!
            val id = call.parameters["id"]?.toLongOrNull() ?: run {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponseData("id must be long")
                )
                return@get
            }

            val res = historyManager.getEntryById(session.uid, id)
                .getOrElse {
                    handleHistoryError(logger, it) { return@get }
                }


            call.respond(res)
        }
    }
}

private suspend inline fun RoutingContext.limit(ret: () -> Nothing): Int {
    return call.parameters["limit"]?.let { str ->
        str.toIntOrNull()?.takeIf { it in 1..HistoryManager.DEFAULT_LIMIT } ?: run {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponseData("when present, limit must be int and be in range 1..${HistoryManager.DEFAULT_LIMIT}")
            )
            ret()
        }
    } ?: HistoryManager.DEFAULT_LIMIT
}

private suspend inline fun RoutingContext.handleHistoryError(
    logger: Logger,
    e: Throwable,
    ret: () -> Nothing,
): Nothing {
    when (e) {
        is HistoryEntryNotFoundException -> {
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponseData("No such element")
            )
        }

        else -> {
            logger.error("failed to list entries", e)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponseData("Internal error")
            )
        }
    }

    ret()
}
