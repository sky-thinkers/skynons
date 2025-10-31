package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.api.Connection
import com.skythinkers.skynons.api.EmptyMessage
import com.skythinkers.skynons.api.Host
import com.skythinkers.skynons.api.Link
import com.skythinkers.skynons.api.RemoveObject
import com.skythinkers.skynons.api.RemovedObjectList
import com.skythinkers.skynons.api.SimpleSimulationResult
import com.skythinkers.skynons.api.SimulationResultRequest
import com.skythinkers.skynons.api.SimulationState
import com.skythinkers.skynons.api.SimulationStateRequest
import com.skythinkers.skynons.api.Switch
import com.skythinkers.skynons.nons.NonsProcessManager
import io.ktor.server.routing.Route
import io.ktor.util.logging.KtorSimpleLogger
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteRecursively
import kotlin.io.path.isRegularFile
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

fun Route.simulate(processManager: NonsProcessManager) {
    val logger = KtorSimpleLogger("API/V1/simulate")
    redirectSimulationPostRequest<SimulationResultRequest, SimpleSimulationResult>(
        processManager,
        "simulate",
        logger = logger,
        requestBodyReplacement = {
            val tempDir = Files.createTempDirectory("simulation-output")
            SimulationResultRequest(tempDir.absolutePathString())
        },
        responseBodyReplacement = { request ->
            val dataDir = Path(request.outputDir)
            fun readFile(subPath: String) = dataDir.resolve(subPath).let {
                if (it.isRegularFile()) it.readText()
                else {
                    logger.warn("Failed to read result file $it")
                    throw SkynonsApiException("Simulation failed")
                }
            }

            val res = SimpleSimulationResult(
                cwnd = readFile("cwnd.svg"),
                packetReordering = readFile("reordering.svg"),
                rate = readFile("rate.svg"),
                rtt = readFile("rtt.svg"),
            )
            @OptIn(ExperimentalPathApi::class)
            dataDir.deleteRecursively()
            res
        }
    )
}
