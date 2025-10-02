package com.skythinkers.skynons

import com.skythinkers.skynons.api.*
import io.ktor.client.request.post
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.CreateSimulation(processManager: NonsProcessManager) {
    post("/create_simulation") {
        try {
            val port = processManager.createSimulation()
            if (port == null) {
                call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("Can not create simulation")
                )
            }
            call.respond(HttpStatusCode.OK, CreateSimulationResponseData(port.toString()))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.toString()))
        }
    }
}
