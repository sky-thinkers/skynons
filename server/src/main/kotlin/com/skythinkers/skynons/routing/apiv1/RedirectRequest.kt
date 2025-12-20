package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.api.EmptyMessage
import com.skythinkers.skynons.api.ErrorResponseData
import com.skythinkers.skynons.api.SimulationApiMessage
import com.skythinkers.skynons.nons.NonsProcessManager
import com.skythinkers.skynons.nons.ProcessId
import com.skythinkers.skynons.nons.expectResponse
import com.skythinkers.skynons.nons.resultOrRespondError
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
import kotlin.reflect.typeOf

inline fun <reified RequestBodyType : SimulationApiMessage, reified ResponseBodyType : SimulationApiMessage> Route.redirectSimulationPostRequest(
    processManager: NonsProcessManager,
    methodName: String,
    crossinline requestBodyReplacement: () -> RequestBodyType? = { null },
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
    crossinline requestBodyReplacement: () -> RequestBodyType? = { null },
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
    crossinline requestBodyReplacement: () -> RequestBodyType? = { null },
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
    requestBodyReplacement: () -> RequestBodyType?,
    logger: Logger,
) {
    val processId: ProcessId = call.callProcessId(processManager, logger) { return }

    logger.trace("Receiving request body")
    val requestBody: RequestBodyType = requestBodyReplacement() ?: try {
        call.receive<RequestBodyType>()
    } catch (e: Exception) {
        logger.warn("Failed to parse JSON body: ${e.message}")
        call.respond(HttpStatusCode.BadRequest, ErrorResponseData("invalid json"))
        return
    }

    val response =
        processManager.expectResponse<ResponseBodyType>(processId, requestBody, logger)
            .resultOrRespondError(call) ?: return

    if (typeOf<ResponseBodyType>() == typeOf<EmptyMessage>()) {
        logger.trace("Forwarding empty response")
        call.respond(HttpStatusCode.OK)

    } else {
        call.respond(HttpStatusCode.OK, response)
    }
}

suspend inline fun RoutingCall.callProcessId(
    processManager: NonsProcessManager,
    logger: Logger,
    ret: () -> Nothing,
): ProcessId {

    val processId: ProcessId = pathParameters["id"]!!

    if (!processManager.checkId(processId)) {
        logger.trace("Process $processId wasn't found")
        respond(HttpStatusCode.BadRequest, ErrorResponseData("Simulation is not found"))
        ret()
    }

    return processId
}
