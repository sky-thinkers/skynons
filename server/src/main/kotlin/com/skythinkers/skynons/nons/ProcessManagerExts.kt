package com.skythinkers.skynons.nons

import com.skythinkers.skynons.api.ErrorResponseData
import com.skythinkers.skynons.api.SimulationApiMessage
import com.skythinkers.skynons.routing.ContinuationManager
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingCall
import io.ktor.util.logging.Logger

suspend inline fun <reified ResponseBodyType : SimulationApiMessage> NonsProcessManager.expectResponse(
    processId: ProcessId,
    request: SimulationApiMessage,
    logger: Logger,
): BackendResponse<ResponseBodyType> {
    return when (val response = message(processId, request)) {
        !is ResponseBodyType if response !is ErrorResponseData -> {
            logger.error("Unexpected response from backend: $response (expected ${ResponseBodyType::class.java.name})")
            BackendResponse.Illegal(ErrorResponseData("Internal conversion error"))
        }

        is ResponseBodyType -> {
            logger.trace("Received well-typed response")
            BackendResponse.WellFormed(response)
        }

        is ErrorResponseData -> {
            logger.trace("Received error response")
            BackendResponse.Error(response)
        }

        else -> {
            logger.error("Unreachable branch in response handling")
            BackendResponse.Illegal(ErrorResponseData("Internal error"))
        }
    }
}

sealed interface BackendResponse<out T : SimulationApiMessage> {
    data class WellFormed<out T : SimulationApiMessage>(val res: T) : BackendResponse<T>
    data class Error(val res: ErrorResponseData) : BackendResponse<Nothing>
    data class Illegal(val res: ErrorResponseData) : BackendResponse<Nothing>
}

suspend fun <T : SimulationApiMessage> BackendResponse<T>.resultOrRespondError(call: RoutingCall): T? = when (this) {
    is BackendResponse.Illegal -> {
        call.respond(HttpStatusCode.InternalServerError, res)
        null
    }

    is BackendResponse.Error -> {
        call.respond(HttpStatusCode.BadRequest, res)
        null
    }

    is BackendResponse.WellFormed -> {
        res
    }
}

suspend fun <T : SimulationApiMessage> BackendResponse<T>.resultOrRespondError(token: ContinuationManager.Token): T? = when (this) {
    is BackendResponse.Illegal -> {
        token.respond(HttpStatusCode.InternalServerError, res)
        null
    }

    is BackendResponse.Error -> {
        token.respond(HttpStatusCode.BadRequest, res)
        null
    }

    is BackendResponse.WellFormed -> {
        res
    }
}
