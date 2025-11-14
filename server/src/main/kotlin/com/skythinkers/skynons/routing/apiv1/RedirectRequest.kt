package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.api.EmptyMessage
import com.skythinkers.skynons.api.ErrorResponseData
import com.skythinkers.skynons.api.SimulationApiMessage
import com.skythinkers.skynons.nons.NonsProcessManager
import com.skythinkers.skynons.nons.ProcessId
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.logging.Logger

inline fun <reified RequestBodyType : SimulationApiMessage, reified ResponseBodyType : SimulationApiMessage> Route.redirectSimulationPostRequest(
    processManager: NonsProcessManager,
    methodName: String,
    crossinline requestBodyReplacement: () -> RequestBodyType? = { null },
    crossinline responseBodyReplacement: (call: RoutingCall, request: RequestBodyType) -> ResponseBodyType = { _, _ ->
        error("No response replacement")
    },
    logger: Logger = KtorSimpleLogger("API_V1/post/$methodName"),
) {
    post("{id}/$methodName") {
        redirectSimulationRequestHandler<RequestBodyType, ResponseBodyType>(
            processManager,
            requestBodyReplacement,
            responseBodyReplacement,
            logger
        )
    }
}

inline fun <reified RequestBodyType : SimulationApiMessage, reified ResponseBodyType : SimulationApiMessage> Route.redirectSimulationGetRequest(
    processManager: NonsProcessManager,
    methodName: String,
    crossinline requestBodyReplacement: () -> RequestBodyType? = { null },
    crossinline responseBodyReplacement: (call: RoutingCall, request: RequestBodyType) -> ResponseBodyType = { _, _ ->
        error("No response replacement")
    },
    logger: Logger = KtorSimpleLogger("API_V1/get/$methodName"),
) {
    get("{id}/$methodName") {
        redirectSimulationRequestHandler<RequestBodyType, ResponseBodyType>(
            processManager,
            requestBodyReplacement,
            responseBodyReplacement,
            logger
        )
    }
}

inline fun <reified RequestBodyType : SimulationApiMessage, reified ResponseBodyType : SimulationApiMessage> Route.redirectSimulationDeleteRequest(
    processManager: NonsProcessManager,
    methodName: String,
    crossinline requestBodyReplacement: () -> RequestBodyType? = { null },
    crossinline responseBodyReplacement: (call: RoutingCall, request: RequestBodyType) -> ResponseBodyType = { _, _ ->
        error("No response replacement")
    },
    logger: Logger = KtorSimpleLogger("API_V1/delete/$methodName"),
) {
    delete("{id}/$methodName") {
        redirectSimulationRequestHandler<RequestBodyType, ResponseBodyType>(
            processManager,
            requestBodyReplacement,
            responseBodyReplacement,
            logger
        )
    }
}

suspend inline fun <reified RequestBodyType : SimulationApiMessage, reified ResponseBodyType : SimulationApiMessage> RoutingContext.redirectSimulationRequestHandler(
    processManager: NonsProcessManager,
    requestBodyReplacement: () -> RequestBodyType?,
    responseBodyReplacement: suspend (call: RoutingCall, request: RequestBodyType) -> ResponseBodyType,
    logger: Logger,
) {
    try {
        val processId: ProcessId =
            call.parameters["id"]?.toInt()
                ?: run {
                    call.respond(HttpStatusCode.BadRequest, "Simulation id is not a number")
                    return
                }

        logger.trace("Receiving request body")
        val requestBody: RequestBodyType = requestBodyReplacement() ?: try {
            call.receive<RequestBodyType>()
        } catch (e: Exception) {
            logger.warn("Failed to parse JSON body: ${e.message}")
            call.respond(HttpStatusCode.BadRequest, ErrorResponseData("invalid json"))
            return
        }

        if (!processManager.checkId(processId)) {
            logger.trace("Process $processId wasn't found")
            call.respond(HttpStatusCode.BadRequest, ErrorResponseData("Simulation is not found"))
            return
        }

        when (val response = processManager.message(processId, requestBody)) {
            !is ResponseBodyType if response is EmptyMessage -> {
                logger.trace("Using response replacement")
                val replacement = responseBodyReplacement(call, requestBody)
                call.respond(replacement)
            }

            !is ResponseBodyType if response !is ErrorResponseData -> {
                logger.error("Unexpected response from backend: $response")
                call.respond(HttpStatusCode.InternalServerError, ErrorResponseData("Internal conversion error"))
            }

            is EmptyMessage -> {
                logger.trace("Received empty response")
                call.respond(HttpStatusCode.OK)
            }

            is ResponseBodyType -> {
                logger.trace("Received well-typed response")
                call.respond<ResponseBodyType>(response)
            }

            is ErrorResponseData -> {
                logger.trace("Received error response")
                call.respond<ErrorResponseData>(HttpStatusCode.BadRequest, response)
            }

            else -> {
                logger.error("Unreachable branch in response handling")
                call.respond(HttpStatusCode.InternalServerError, ErrorResponseData("Internal error"))
            }
        }
    } catch (e: SkynonsApiException) {
        logger.debug("API error", e)
        call.respond(HttpStatusCode.BadRequest, ErrorResponseData(e.message ?: "Unknown error"))
    }
}
