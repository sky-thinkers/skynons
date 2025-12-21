package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.api.Connection
import com.skythinkers.skynons.api.EmptyMessage
import com.skythinkers.skynons.api.ErrorResponseData
import com.skythinkers.skynons.api.Host
import com.skythinkers.skynons.api.Link
import com.skythinkers.skynons.api.RemoveObject
import com.skythinkers.skynons.api.RemovedObjectList
import com.skythinkers.skynons.api.SaveSimulationRequest
import com.skythinkers.skynons.api.SimpleSimulationResult
import com.skythinkers.skynons.api.SimulationResultRequest
import com.skythinkers.skynons.api.SimulationState
import com.skythinkers.skynons.api.SimulationStateRequest
import com.skythinkers.skynons.api.Switch
import com.skythinkers.skynons.auth.UserSession
import com.skythinkers.skynons.auth.uid
import com.skythinkers.skynons.history.HistoryManager
import com.skythinkers.skynons.nons.NonsProcessManager
import com.skythinkers.skynons.nons.ProcessId
import com.skythinkers.skynons.nons.expectResponse
import com.skythinkers.skynons.nons.resultOrRespondError
import com.skythinkers.skynons.routing.ContinuationManager
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import java.nio.file.Files
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteRecursively
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

fun Route.addHost(processManager: NonsProcessManager) {
    redirectSimulationPostRequest<Host, EmptyMessage>(processManager, "add_host")
}

fun Route.addSwitch(processManager: NonsProcessManager) {
    redirectSimulationPostRequest<Switch, EmptyMessage>(processManager, "add_switch")
}

fun Route.addLink(processManager: NonsProcessManager) {
    redirectSimulationPostRequest<Link, EmptyMessage>(processManager, "add_link")
}

fun Route.addConnection(processManager: NonsProcessManager) {
    redirectSimulationPostRequest<Connection, EmptyMessage>(processManager, "add_connection")
}

fun Route.getState(processManager: NonsProcessManager) {
    redirectSimulationGetRequest<SimulationStateRequest, SimulationState>(
        processManager, "state",
        requestBodyReplacement = { SimulationStateRequest }
    )
}

fun Route.removeObject(processManager: NonsProcessManager) {
    redirectSimulationDeleteRequest<RemoveObject, RemovedObjectList>(processManager, "remove_object")
}


fun Route.simulate(
    processManager: NonsProcessManager,
    historyManager: HistoryManager,
    continuationManager: ContinuationManager,
) {
    val logger = KtorSimpleLogger("API/V1/simulate")
    val coroutineScope = CoroutineScope(EmptyCoroutineContext)
    authenticate(UserSession.USER_SESSION, optional = true) {
        post("{id}/simulate") {
            val tempDir = Files.createTempDirectory("simulation-output")
            val token = continuationManager.newToken(call)
            val session = call.principal<UserSession>(UserSession.USER_SESSION)

            val processId: ProcessId = call.callProcessId(processManager, logger) { return@post }

            val job = coroutineScope.launch {
                try {
                    val configName = "config.yaml"
                    val configPath = tempDir.resolve(configName)

                    processManager.expectResponse<EmptyMessage>(
                        processId,
                        SaveSimulationRequest(configPath.absolutePathString()),
                        logger
                    ).resultOrRespondError(token) ?: return@launch

                    processManager.expectResponse<EmptyMessage>(
                        processId,
                        SimulationResultRequest(tempDir.absolutePathString()),
                        logger
                    ).resultOrRespondError(token) ?: return@launch

                    fun readFile(subPath: String) = tempDir.resolve(subPath).let {
                        if (it.isRegularFile()) it.readText()
                        else {
                            logger.warn("Failed to read result file $it")
                            throw SkynonsApiException("Simulation failed")
                        }
                    }
                    logger.trace("out dir contents: {}", tempDir.listDirectoryEntries().joinToString())

                    val config = readFile(configName)
                    val res = SimpleSimulationResult(
                        cwnd = readFile("cwnd.svg"),
                        packetReordering = readFile("reordering.svg"),
                        rate = readFile("rate.svg"),
                        rtt = readFile("rtt.svg"),
                    )
                    if (session != null) {
                        historyManager.storeEntry(session.uid, config, res)
                            .onFailure {
                                logger.warn("Failed to store history entry", it)
                            }
                    }
                    logger.trace("Simulation completed")

                    logger.trace("Responding simulation result")
                    token.respond(HttpStatusCode.OK, res)
                } catch (e: SkynonsApiException) {
                    logger.warn("API error", e)
                    token.respond(HttpStatusCode.BadRequest, ErrorResponseData(e.message ?: "Unknown error"))
                } catch (e: Throwable) {
                    logger.error("Simulation error", e)
                    token.respond(HttpStatusCode.InternalServerError, ErrorResponseData("Unknown error"))
                } finally {
                    @OptIn(ExperimentalPathApi::class)
                    tempDir.deleteRecursively()
                }
            }

            select {
                job.onJoin
                @OptIn(ExperimentalCoroutinesApi::class)
                onTimeout(continuationManager.conversationTimeout) {
                    if (job.isActive) {
                        token.toContinuation()
                    }
                }
            }
        }
    }
}
