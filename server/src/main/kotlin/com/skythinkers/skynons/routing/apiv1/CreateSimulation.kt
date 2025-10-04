package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.api.CreateSimulationResponseData
import com.skythinkers.skynons.api.ErrorResponseData
import com.skythinkers.skynons.routing.NonsProcessManager
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.createSimulation(processManager: NonsProcessManager) {
    post("/create_simulation") {
        try {
            val port = processManager.createSimulation()
            if (port == null) {
                call.respond(
                        HttpStatusCode.InternalServerError,
                    ErrorResponseData("Can not create simulation")
                )
            }
            call.respond(HttpStatusCode.OK, CreateSimulationResponseData(port.toString()))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, ErrorResponseData(e.toString()))
        }
    }
}
