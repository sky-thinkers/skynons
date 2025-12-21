package com.skythinkers.skynons.routing

import com.skythinkers.skynons.auth.AccountManager
import com.skythinkers.skynons.history.HistoryManager
import com.skythinkers.skynons.nons.NonsProcessManager
import com.skythinkers.skynons.routing.apiv1.apiV1
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting(
    nonsPath: String,
    processManager: NonsProcessManager,
    accountManager: AccountManager,
    historyManager: HistoryManager,
    continuationManager: ContinuationManager,
) {
    routing {
        route("api") {
            route("v0") { apiV0(nonsPath) }
            route("v1") {
                route("users") {
                    users(accountManager)
                }
                route("history") {
                    history(historyManager)
                }
                apiV1(processManager, historyManager, continuationManager)
                cont(continuationManager)
            }
        }
    }
}
