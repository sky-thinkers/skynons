package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.api.ApiMessage
import com.skythinkers.skynons.api.EmptyMessage
import com.skythinkers.skynons.api.ErrorResponseData
import com.skythinkers.skynons.routing.NonsProcessManager
import com.skythinkers.skynons.routing.Port
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.util.logging.KtorSimpleLogger

inline fun <reified RequestBodyType : ApiMessage, reified ResponseBodyType : ApiMessage> Route.redirectRequest(
    processManager: NonsProcessManager,
    methodName: String,
) {
    val logger = KtorSimpleLogger("V1 API")
    post("{id}/$methodName") {
        val port: Port =
            call.parameters["id"]?.toInt()
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Simulation id is not a number")
                    return@post
                }

        val body: RequestBodyType =
            if (RequestBodyType::class != EmptyMessage::class) {
                try {
                    call.receive<RequestBodyType>()
                } catch (e: Exception) {
                    logger.warn("Failed to parse JSON body: ${e.message}")
                    call.respond(HttpStatusCode.BadRequest, ErrorResponseData("invalid json"))
                    return@post
                }
            } else EmptyMessage as RequestBodyType


        if (!processManager.checkPort(port)) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponseData("Simulation is not found"))
            return@post
        }

        val response = processManager.message(port, body)
        when (response) {
            !is ResponseBodyType if response !is ErrorResponseData -> {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponseData("Internal conversion error"))
            }

            is EmptyMessage -> {
                call.respond(HttpStatusCode.OK)
            }

            is ErrorResponseData -> {
                call.respond(HttpStatusCode.BadRequest, response)
            }

            else -> {
                call.respond(response)
            }
        }
    }
}
