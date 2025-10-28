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
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.logging.Logger

inline fun <reified RequestBodyType : SimulationApiMessage, reified ResponseBodyType : SimulationApiMessage> Route.redirectSimulationPostRequest(
    processManager: NonsProcessManager,
    methodName: String,
    requestBodyReplacement: RequestBodyType? = null,
    logger: Logger = KtorSimpleLogger("API_V1/post/$methodName"),
) {
    post("{id}/$methodName") {
        redirectSimulationRequestHandler<RequestBodyType, ResponseBodyType>(
            processManager,
            requestBodyReplacement,
            logger
        )
    }
}

inline fun <reified RequestBodyType : SimulationApiMessage, reified ResponseBodyType : SimulationApiMessage> Route.redirectSimulationGetRequest(
    processManager: NonsProcessManager,
    methodName: String,
    requestBodyReplacement: RequestBodyType? = null,
    logger: Logger = KtorSimpleLogger("API_V1/get/$methodName"),
) {
    get("{id}/$methodName") {
        redirectSimulationRequestHandler<RequestBodyType, ResponseBodyType>(
            processManager,
            requestBodyReplacement,
            logger
        )
    }
}

inline fun <reified RequestBodyType : SimulationApiMessage, reified ResponseBodyType : SimulationApiMessage> Route.redirectSimulationDeleteRequest(
    processManager: NonsProcessManager,
    methodName: String,
    requestBodyReplacement: RequestBodyType? = null,
    logger: Logger = KtorSimpleLogger("API_V1/delete/$methodName"),
) {
    delete("{id}/$methodName") {
        redirectSimulationRequestHandler<RequestBodyType, ResponseBodyType>(
            processManager,
            requestBodyReplacement,
            logger
        )
    }
}

suspend inline fun <reified RequestBodyType : SimulationApiMessage, reified ResponseBodyType : SimulationApiMessage> RoutingContext.redirectSimulationRequestHandler(
    processManager: NonsProcessManager,
    requestBodyReplacement: RequestBodyType? = null,
    logger: Logger,
) {
    val processId: ProcessId =
        call.parameters["id"]?.toInt()
            ?: run {
                call.respond(HttpStatusCode.BadRequest, "Simulation id is not a number")
                return
            }

    val body: RequestBodyType = requestBodyReplacement ?: try {
                call.receive<RequestBodyType>()
            } catch (e: Exception) {
                logger.warn("Failed to parse JSON body: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, ErrorResponseData("invalid json"))
                return
            }


    if (!processManager.checkId(processId)) {
        call.respond(HttpStatusCode.BadRequest, ErrorResponseData("Simulation is not found"))
        return
    }

    when (val response = processManager.message(processId, body)) {
        !is ResponseBodyType if response !is ErrorResponseData -> {
            call.respond(HttpStatusCode.InternalServerError, ErrorResponseData("Internal conversion error"))
        }

        is EmptyMessage -> {
            call.respond(HttpStatusCode.OK)
        }

        is ResponseBodyType -> {
            call.respond<ResponseBodyType>(response)
        }

        is ErrorResponseData -> {
            call.respond<ErrorResponseData>(HttpStatusCode.BadRequest, response)
        }

        else -> {
            logger.error("Unreachable branch in response handling")
            call.respond(HttpStatusCode.InternalServerError, ErrorResponseData("Internal error"))
        }
    }
}
