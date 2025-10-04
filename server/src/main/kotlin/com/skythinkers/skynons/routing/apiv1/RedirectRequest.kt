package com.skythinkers.skynons.routing.apiv1

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
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.util.logging.KtorSimpleLogger

inline fun <reified RequestBodyType : Any, reified ResponseBodyType : Any> Route.redirectRequest(
        processManager: NonsProcessManager,
        methodName: String
) {
    val logger = KtorSimpleLogger("V1 API")
    val client = HttpClient(OkHttp) { install(ContentNegotiation) { json() } }
    post("{id}/$methodName") {
        val port: Port =
                call.parameters["id"]?.toInt()
                        ?: run {
                            call.respond(HttpStatusCode.BadRequest, "Simulation id is not a number")
                            return@post
                        }

        val body: RequestBodyType? =
                if (RequestBodyType::class != Unit::class) {
                    try {
                        call.receive<RequestBodyType>()
                    } catch (e: Exception) {
                        logger.warn("Failed to parse JSON body: ${e.message}")
                        call.respond(HttpStatusCode.BadRequest, ErrorResponseData("invalid json"))
                        return@post
                    }
                } else null

        // TODO: uncomment if when real backend will be implemented
        // if (!processManager.checkPort(port)) {
        //     call.respond(
        //             HttpStatusCode.BadRequest,
        //             ErrorResponse("Can not find simulation with given id")
        //     )
        //     return@post
        // }
        // val targetUrl = "http://127.0.0.1:$port/api/v1/$methodName"

        // TODO: remove this line when real backend will be implemented
        val targetUrl = "http://localhost:8091/api/v1/$port/$methodName"

        logger.info("Forwarding request to $targetUrl with body '${body?:""}'")
        val response: HttpResponse =
                try {
                    client.post(targetUrl) {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                } catch (e: Exception) {
                    logger.error("Error forwarding to $targetUrl: ${e.stackTraceToString()}")
                    call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponseData("Cannot reach local simulation server")
                    )
                    return@post
                }
        if (ResponseBodyType::class != Unit::class) {
            try {
                call.respond(status = response.status, response.body<ResponseBodyType>())
            } catch (e: Exception) {
                logger.error("Error convering backend responce: ${e.stackTraceToString()}")
                call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponseData("Can not convert backend responce to given type")
                )
            }
        } else {
            call.respond(response.status)
        }
    }
}
