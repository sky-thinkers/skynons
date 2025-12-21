package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.api.CreateSimulationResponseData
import com.skythinkers.skynons.api.CreateSimulationWithConfigRequest
import com.skythinkers.skynons.api.EmptyMessage
import com.skythinkers.skynons.api.ErrorResponseData
import com.skythinkers.skynons.api.RestoreSimulationRequest
import com.skythinkers.skynons.api.SimulationApiMessage
import com.skythinkers.skynons.api.SimulationState
import com.skythinkers.skynons.auth.UserSession
import com.skythinkers.skynons.auth.uid
import com.skythinkers.skynons.history.HistoryManager
import com.skythinkers.skynons.nons.NonsProcessManager
import com.skythinkers.skynons.nons.expectResponse
import com.skythinkers.skynons.nons.resultOrRespondError
import com.skythinkers.skynons.routing.handleHistoryError
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.post
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

fun Route.createSimulation(processManager: NonsProcessManager) {
    val logger = KtorSimpleLogger("API/V1/create_simulation")
    post("/create_simulation") {
        createImpl<EmptyMessage>(processManager, logger) {
            null
        }
    }
}

fun Route.createSimulationWithConfig(processManager: NonsProcessManager) {
    val logger = KtorSimpleLogger("API/V1/create_with_config")
    post("/create_with_config") {
        createImpl<SimulationState>(processManager, logger) {
            call.receive<CreateSimulationWithConfigRequest>().config
        }
    }
}

fun Route.restoreFromHistory(processManager: NonsProcessManager, historyManager: HistoryManager) {
    val logger = KtorSimpleLogger("API/V1/restore_from_history")
    authenticate(UserSession.USER_SESSION) {
        post("/restore/{id}") {
            createImpl<SimulationState>(processManager, logger) {
                val session: UserSession = call.principal<UserSession>(UserSession.USER_SESSION)!!
                val id = call.parameters["id"]?.toLongOrNull() ?: run {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponseData("id must be long")
                    )
                    return@post
                }

                val historyEntry = historyManager.getEntryById(session.uid, id)
                    .getOrElse { e ->
                        handleHistoryError(logger, e) { return@post }
                    }

                historyEntry.config
            }
        }
    }
}

private suspend inline fun <reified R : SimulationApiMessage>RoutingContext.createImpl(
    processManager: NonsProcessManager,
    logger: Logger,
    getConfigImpl: () -> String?,
) {
    val config = getConfigImpl()
    val tmpConfig = if (config != null) withContext(Dispatchers.IO) {
        val path = Files.createTempFile("restore-config", ".yaml")
        path.writeText(config)
        path
    } else null
    try {
        val procId = processManager.createSimulation()
        if (procId == null) {
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponseData("Can not create simulation")
            )
        } else {
            if (tmpConfig != null) {
                val isError = processManager.expectResponse<R>(
                    procId,
                    RestoreSimulationRequest(tmpConfig.absolutePathString()),
                    logger
                ).resultOrRespondError(call) == null
                if (isError) {
                    processManager.killProcess(procId)
                    return
                }
            }
            call.respond(HttpStatusCode.OK, CreateSimulationResponseData(procId))
        }
    } catch (e: Exception) {
        call.respond(HttpStatusCode.InternalServerError, ErrorResponseData(e.toString()))
    } finally {
        tmpConfig?.deleteIfExists()
    }
}
