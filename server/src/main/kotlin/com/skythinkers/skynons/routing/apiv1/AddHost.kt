package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.api.AddHostRequestData
import com.skythinkers.skynons.api.ErrorResponseData
import com.skythinkers.skynons.routing.NonsProcessManager
import com.skythinkers.skynons.routing.Port
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.util.logging.KtorSimpleLogger


fun Route.addHost(processManager: NonsProcessManager) {
    val logger = KtorSimpleLogger("V1 API")
    val client = HttpClient(OkHttp) { install(ContentNegotiation) { json() } }
    post("/{id}/add_host") {
        val port: Port =
                call.parameters["id"]?.toInt()
                        ?: run {
                            call.respond(HttpStatusCode.BadRequest, "Simulation id is not a number")
                            return@post
                        }

        val body =
                try {
                    call.receive<AddHostRequestData>()
                } catch (e: Exception) {
                    logger.warn("Failed to parse JSON body: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, ErrorResponseData("invalid json"))
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
            call.respond(status = response.status, message = response.body<Map<String, String>>())
        } catch (e: Exception) {
            logger.error("Error forwarding to $targetUrl: ${e.stackTraceToString()}")
            call.respond(
                    HttpStatusCode.InternalServerError,
                ErrorResponseData("Cannot reach local simulation server")
            )
        }
    }
}
