package com.skythinkers.skynons

import com.skythinkers.skynons.api.*
import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.KtorSimpleLogger
import kotlin.io.path.*
import kotlinx.serialization.Serializable

@Serializable data class ErrorResponse(val err: String)

@Serializable data class AddHostRequest(val name: String)

fun Route.ApiV1(nonsPath: String) {
    val processManager = NonsProcessManager(nonsPath)
    val log = KtorSimpleLogger("V1 API")
    val client = HttpClient(Jetty) { install(ContentNegotiation) { json() } }
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
    post("/{id}/add_host") {
        val port: Port =
                call.parameters["id"]?.toInt()
                        ?: run {
                            call.respond(HttpStatusCode.BadRequest, "Simulation id is not a number")
                            return@post
                        }

        val body: AddHostRequest =
                try {
                    call.receive<AddHostRequest>()
                } catch (e: Exception) {
                    log.warn("Failed to parse JSON body: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid json"))
                    return@post
                }

        if (!processManager.checkPort(port)) {
            call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Can not find simulation with given id")
            )
        }
        val targetUrl = "http://127.0.0.1:$port/add_host/"

        try {
            log.info("Forwarding request to $targetUrl with body name='${body.name}'")
            val response: HttpResponse =
                    client.post(targetUrl) {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
            call.respond(response)
        } catch (e: Exception) {
            log.error("Error forwarding to $targetUrl: ${e.message}")
            call.respond(HttpStatusCode.BadRequest, mapOf("err" to "cannot reach upstream"))
        }
    }
}
