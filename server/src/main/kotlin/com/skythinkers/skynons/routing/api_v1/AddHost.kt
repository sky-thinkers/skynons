package com.skythinkers.skynons

import com.skythinkers.skynons.api.*
import io.ktor.client.*
import io.ktor.client.engine.jetty.jakarta.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.serialization.Serializable

@Serializable data class AddHostRequest(val name: String)

fun Route.AddHost(processManager: NonsProcessManager) {
    val logger = KtorSimpleLogger("V1 API")
    val client = HttpClient(Jetty) { install(ContentNegotiation) { json() } }
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
                    logger.warn("Failed to parse JSON body: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid json"))
                    return@post
                }

        // if (!processManager.checkPort(port)) {
        //     call.respond(
        //             HttpStatusCode.BadRequest,
        //             ErrorResponse("Can not find simulation with given id")
        //     )
        //     return@post
        // }
        // val targetUrl = "http://127.0.0.1:$port/api/v1/create_simulation"
        val targetUrl = "http://localhost:8090/api/v1/$port/add_host"

        try {
            logger.info("Forwarding request to $targetUrl with body name='${body.name}'")
            val response: HttpResponse =
                    client.post(targetUrl) {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
            call.respond(response)
        } catch (e: Exception) {
            logger.error("Error forwarding to $targetUrl: ${e.message}")
            call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse("Cannot reach local simulation server")
            )
        }
    }
}
