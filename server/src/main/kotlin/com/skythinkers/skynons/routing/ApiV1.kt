package com.skythinkers.skynons

import com.skythinkers.skynons.api.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.io.path.*

fun Route.ApiV1(nonsPath: String) {
    val processManager = NonsProcessManager(nonsPath)
    post("/create_simulation") {
        try {
            val port = processManager.createSimulation()
            if (port == null) {
                call.respond(HttpStatusCode.InternalServerError, "Can not create simulation")
            }
            call.respond(HttpStatusCode.OK, CreateSimulationResponseData(port.toString()))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, e.toString())
        }
    }
}
