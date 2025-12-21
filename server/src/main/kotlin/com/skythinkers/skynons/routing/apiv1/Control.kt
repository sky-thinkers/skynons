package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.nons.NonsProcessManager
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.util.logging.KtorSimpleLogger

fun Route.stopProcess(processManager: NonsProcessManager) {
    val logger = KtorSimpleLogger("APIv1/stop")
    post("{id}/stop") {
        val processId = call.callProcessId(processManager, logger) { return@post }
        processManager.killProcess(processId)
        call.respond(HttpStatusCode.OK)
    }
}

fun Route.suspendProcess(processManager: NonsProcessManager) {
    val logger = KtorSimpleLogger("APIv1/suspend")
    post("{id}/suspend") {
        val processId = call.callProcessId(processManager, logger) { return@post }
        processManager.suspendProcess(processId)
        call.respond(HttpStatusCode.OK)
    }
}
