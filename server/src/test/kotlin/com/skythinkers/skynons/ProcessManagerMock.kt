package com.skythinkers.skynons

import com.skythinkers.skynons.api.Connection
import com.skythinkers.skynons.api.CreateSimulationResponseData
import com.skythinkers.skynons.api.CreateSimulationWithConfigRequest
import com.skythinkers.skynons.api.EmptyMessage
import com.skythinkers.skynons.api.ErrorResponseData
import com.skythinkers.skynons.api.Host
import com.skythinkers.skynons.api.Link
import com.skythinkers.skynons.api.RemoveObject
import com.skythinkers.skynons.api.RemovedObjectList
import com.skythinkers.skynons.api.RestoreSimulationRequest
import com.skythinkers.skynons.api.SaveSimulationRequest
import com.skythinkers.skynons.api.SimpleSimulationResult
import com.skythinkers.skynons.api.SimulationApiMessage
import com.skythinkers.skynons.api.SimulationResultRequest
import com.skythinkers.skynons.api.SimulationState
import com.skythinkers.skynons.api.SimulationStateRequest
import com.skythinkers.skynons.api.Switch
import com.skythinkers.skynons.nons.NonsProcessManager
import com.skythinkers.skynons.nons.ProcessId
import kotlin.io.path.Path
import kotlin.io.path.writeText

class ProcessManagerMock : NonsProcessManager {
    private val simulations = mutableMapOf<ProcessId, Simulation>()
    private var nextId = 0
    private var isClosed = false

    override suspend fun createSimulation(): ProcessId {
        ensureActive()
        val id = nextId++
        simulations[id.toString()] = Simulation()
        return id.toString()
    }

    override fun checkId(id: ProcessId): Boolean {
        ensureActive()
        return id in simulations
    }

    override suspend fun message(
        id: ProcessId,
        message: SimulationApiMessage,
    ): SimulationApiMessage {
        ensureActive()
        return simulations.getValue(id).message(message)
    }

    override fun close() {
        isClosed = true
        simulations.clear()
    }

    override fun killProcess(id: ProcessId) {
        simulations.remove(id)
    }

    private fun ensureActive() {
        require(!isClosed)
    }

    /**
     * Very simple impl, does not resolve object dependencies in any way.
     */
    private class Simulation {
        val hosts = mutableSetOf<String>()
        val switches = mutableSetOf<String>()
        val links = mutableMapOf<String, Link>()
        val connections = mutableMapOf<String, Connection>()

        fun message(
            message: SimulationApiMessage,
        ): SimulationApiMessage {
            return when (message) {
                is Host -> {
                    if (hasId(message.name)) {
                        ErrorResponseData("Already has id `${message.name}`")
                    } else {
                        hosts.add(message.name)
                        EmptyMessage
                    }
                }

                is Switch -> {
                    if (hasId(message.name)) {
                        ErrorResponseData("Already has id ${message.name}")
                    } else {
                        hosts.add(message.name)
                        EmptyMessage
                    }
                }

                is Link -> {
                    if (hasId(message.name)) {
                        ErrorResponseData("Already has id `${message.name}`")
                    } else {
                        links[message.name] = message
                        EmptyMessage
                    }
                }

                is Connection -> {
                    if (hasId(message.name)) {
                        ErrorResponseData("Already has id ${message.name}")
                    } else {
                        connections[message.name] = message
                        EmptyMessage
                    }
                }

                is RemoveObject -> {
                    if (hosts.remove(message.id) || switches.remove(message.id) || links.remove(message.id) != null || connections.remove(
                            message.id
                        ) != null
                    ) {
                        RemovedObjectList(listOf(message.id))
                    } else {
                        ErrorResponseData("No such id `${message.id}`")
                    }
                }

                is SimulationResultRequest -> {
                    val basePath = Path(message.outputDir)
                    basePath.resolve("cwnd.svg").writeText("")
                    basePath.resolve("reordering.svg").writeText("")
                    basePath.resolve("rate.svg").writeText("")
                    basePath.resolve("rtt.svg").writeText("")
                    EmptyMessage
                }

                SimulationStateRequest -> {
                    SimulationState(
                        hosts = hosts.map { Host(it) },
                        switches = switches.map { Switch(it) },
                        links = links.values.toList(),
                        connections = connections.values.toList(),
                    )
                }

                is SaveSimulationRequest -> {
                    Path(message.outputPath).writeText("")
                    EmptyMessage
                }

                is RestoreSimulationRequest, is CreateSimulationWithConfigRequest -> {
                    ErrorResponseData("Restore are unsupported in mock")
                }

                is CreateSimulationResponseData, EmptyMessage, is ErrorResponseData, is SimpleSimulationResult, is SimulationState, is RemovedObjectList -> {
                    ErrorResponseData("Illegal message `$message`")
                }
            }
        }

        private fun hasId(id: String): Boolean =
            id in hosts || id in switches || id in links || id in connections
    }
}
