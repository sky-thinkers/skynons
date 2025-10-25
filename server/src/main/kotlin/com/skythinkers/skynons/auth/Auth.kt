package com.skythinkers.skynons.auth

import com.skythinkers.skynons.api.ErrorResponseData
import com.skythinkers.skynons.database.CredentialsDatabase
import com.skythinkers.skynons.database.SkynonsDatabase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authentication
import io.ktor.server.auth.session
import io.ktor.server.response.respond
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import kotlin.io.encoding.Base64

fun Application.createAuthManager(database: SkynonsDatabase, credentialsDatabase: CredentialsDatabase): AccountManager {
    val authConfig = AuthConfig(
        issuer = environment.config.property("auth.issuer").getString(),
        audience = environment.config.property("auth.audience").getString(),
        secret = Base64.decode(environment.config.property("auth.secret").getString()),
        pepper = Base64.decode(environment.config.property("auth.pepper").getString()),
        database = database,
        credentialsDatabase = credentialsDatabase
    )

    install(Sessions) {
        cookie<UserSession>(UserSession.USER_SESSION) {
            cookie.path = "/"
            cookie.maxAgeInSeconds = authConfig.tokenValidityPeriod.inWholeSeconds
        }
    }

    val accountManager = AccountManagerImpl(authConfig)

    authentication {
        session<UserSession>(UserSession.USER_SESSION) {
            validate { session ->
                val credential =
                    runCatching { accountManager.jwtVerifier.verify(session.token) }.getOrNull()

                when {
                    credential == null -> null

                    !credential.audience.contains(authConfig.audience) -> {
                        null
                    }

                    accountManager.validateSession(session).getOrNull() == true -> {
                        session
                    }

                    else -> {
                        null
                    }
                }
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponseData("Unauthorized"))
            }
        }
    }

    return accountManager
}
