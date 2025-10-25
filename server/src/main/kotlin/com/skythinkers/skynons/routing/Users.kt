package com.skythinkers.skynons.routing

import com.skythinkers.skynons.api.EmptyMessage
import com.skythinkers.skynons.api.ErrorResponseData
import com.skythinkers.skynons.api.LoginRequest
import com.skythinkers.skynons.api.PasswordUpdateRequest
import com.skythinkers.skynons.api.RegistrationRequest
import com.skythinkers.skynons.auth.AccountManager
import com.skythinkers.skynons.auth.UserSession
import com.skythinkers.skynons.auth.uid
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.sessions
import io.ktor.util.logging.KtorSimpleLogger

fun Route.users(accountManager: AccountManager) {
    val logger = KtorSimpleLogger("API/users")

    post("/authenticate") {
        val request = call.receive<LoginRequest>()

        val (info, session) = accountManager.authenticateUser(request)
            .getOrElse {
                logger.debug("authentication failure", it)
                call.respond(HttpStatusCode.Unauthorized, ErrorResponseData("Unauthorized"))
                return@post
            }

        call.sessions.set(UserSession.USER_SESSION, session)

        call.respond(info)
    }

    post("/register") {
        val request = call.receive<RegistrationRequest>()

        val (info, session) = accountManager.registerUser(request)
            .getOrElse {
                logger.debug("registration failure", it)
                call.respond(HttpStatusCode.Unauthorized, ErrorResponseData("Unauthorized"))
                return@post
            }

        call.sessions.set(UserSession.USER_SESSION, session)

        call.respond(info)
    }

    authenticate(UserSession.USER_SESSION) {
        post("/updatePassword") {
            val session: UserSession = call.principal<UserSession>(UserSession.USER_SESSION)!!
            val request = call.receive<PasswordUpdateRequest>()

            accountManager.updateUserPassword(session.uid, request)
                .getOrElse {
                    logger.debug("update password failure", it)
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponseData("Unauthorized"))
                    return@post
                }

            call.respond(EmptyMessage)
        }

        post("/logout") {
            val session: UserSession = call.principal<UserSession>(UserSession.USER_SESSION)!!

            accountManager.invalidateSession(session)
                .getOrElse {
                    logger.debug("Token invalidation failed", it)
                    call.respond(HttpStatusCode.BadRequest, ErrorResponseData("Token invalidation failed"))
                    return@post
                }
            call.sessions.clear(UserSession.USER_SESSION)

            call.respond(EmptyMessage)
        }

        get("/shortInfo") {
            val session: UserSession = call.principal<UserSession>(UserSession.USER_SESSION)!!

            val info = accountManager.getShortUserInfo(session)
                .getOrElse {
                    logger.debug("Unknown error while trying to get short user info", it)
                    call.respond(HttpStatusCode.InternalServerError, ErrorResponseData("Server is malfunctional"))
                    return@get
                }

            call.respond(info)
        }
    }
}