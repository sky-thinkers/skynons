package com.skythinkers.skynons.api

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class SimulationServerMock {
    var myHosts = emptyList<Host>()
    var mySwitches = emptyList<Switch>()
    var myLinks = emptyList<Link>()
    var myConnections = emptyList<Connection>()

    private fun deviceExists(name: String): Boolean {
        return myHosts.any { it.name == name } ||
                mySwitches.any { it.name == name } ||
                myLinks.any { it.name == name } ||
                myConnections.any { it.name == name }
    }

    private fun dataHolderExists(name: String): Boolean {
        return myHosts.any { it.name == name } ||
                mySwitches.any { it.name == name }
    }

    fun addHost(host: Host): ApiResult<Unit>? {
        if (deviceExists(host.name)) {
            return null
        }
        myHosts += host
        return ApiResult.Success(Unit)
    }

    fun addSwitch(switch: Switch): ApiResult<Unit>? {
        if (deviceExists(switch.name)) {
            return null
        }
        mySwitches += switch
        return ApiResult.Success(Unit)
    }

    fun addLink(link: Link): ApiResult<Unit>? {
        if (deviceExists(link.name)) {
            return null
        }
        if (!dataHolderExists(link.fromId)) {
            return null
        }
        if (!dataHolderExists(link.toId)) {
            return null
        }
        myLinks += link
        return ApiResult.Success(Unit)
    }

    fun addConnection(connection: Connection): ApiResult<Unit>? {
        if (deviceExists(connection.name)) {
            return null
        }
        if (!dataHolderExists(connection.senderId)) {
            return null
        }
        if (!dataHolderExists(connection.receiverId)) {
            return null
        }
        myConnections += connection
        return ApiResult.Success(Unit)
    }

    fun removeObject(objectId: ObjectId): List<ObjectId> {
        if (!deviceExists(objectId)) {
            throw Exception("There is no object with this id")
        }

        val result = mutableListOf(objectId)

        for (host in myHosts) {
            if (host.name == objectId) {
                myHosts -= host
                for (link in myLinks) {
                    if (link.fromId == host.name || link.toId == host.name) {
                        result += link.name
                        myLinks -= link
                    }
                }
                for (connection in myConnections) {
                    if (connection.senderId == host.name || connection.receiverId == host.name) {
                        result += connection.name
                        myConnections -= connection
                    }
                }
            }
        }

        for (switch in mySwitches) {
            if (switch.name == objectId) {
                mySwitches -= switch
                for (link in myLinks) {
                    if (link.fromId == switch.name || link.toId == switch.name) {
                        result += link.name
                        myLinks -= link
                    }
                }
                for (connection in myConnections) {
                    if (connection.senderId == switch.name || connection.receiverId == switch.name) {
                        result += connection.name
                        myConnections -= connection
                    }
                }
            }
        }

        for (link in myLinks) {
            if (link.name == objectId) {
                myLinks -= link
            }
        }

        for (connection in myConnections) {
            if (connection.name == objectId) {
                myConnections -= connection
            }
        }

        return result.toList()
    }

    fun simulate(): SimpleSimulationResult {
        throw Exception("Maybe will be implemented later")
    }
}

class SkynonsClientApiMock : SkynonsClientApi {
    private var simulations = emptyList<SimulationServerMock>()

    override suspend fun simulateConfig(config: String): ApiResult<SimpleSimulationResult> =
        ApiResult.ServerError("Unsupported feature")

    override suspend fun createSimulation(): ApiResult<SimulationId> {
        val result = ApiResult.Success(simulations.size.toString())
        simulations += SimulationServerMock()
        return result
    }

    override suspend fun createSimulationWithConfig(config: String): ApiResult<SimulationId> =
        ApiResult.ServerError("Unsupported feature")

    override suspend fun restoreSimulationFromHistoryEntry(entryId: HistoryEntryId): ApiResult<SimulationId> =
        ApiResult.ServerError("Unsupported feature")

    override suspend fun addHost(simulationId: SimulationId, name: ObjectId): ApiResult<Unit> {
        val id = simulationId.toIntOrNull() ?: return ApiResult.ClientError("Simulation id must be a number")

        return simulations.getOrNull(id)?.addHost(Host(name)) ?: ApiResult.ServerError("Unrecognized exception")
    }

    override suspend fun addSwitch(simulationId: SimulationId, name: ObjectId): ApiResult<Unit> {
        val id = simulationId.toIntOrNull() ?: return ApiResult.ClientError("Simulation id must be a number")

        return simulations.getOrNull(id)?.addSwitch(Switch(name)) ?: ApiResult.ServerError("Unrecognized exception")
    }

    override suspend fun addLink(
        simulationId: SimulationId,
        name: ObjectId,
        fromId: ObjectId,
        toId: ObjectId,
        speed: SpeedString,
    ): ApiResult<Unit> {
        val id = simulationId.toIntOrNull() ?: return ApiResult.ClientError("Simulation id must be a number")

        return simulations.getOrNull(id)?.addLink(Link(name, fromId, toId, speed))
            ?: ApiResult.ServerError("Unrecognized exception")
    }

    override suspend fun addConnection(
        simulationId: SimulationId,
        name: ObjectId,
        senderId: ObjectId,
        receiverId: ObjectId,
        sizeToSend: SizeString,
    ): ApiResult<Unit> {
        val id = simulationId.toIntOrNull() ?: return ApiResult.ClientError("Simulation id must be a number")

        return simulations.getOrNull(id)?.addConnection(Connection(name, senderId, receiverId, sizeToSend))
            ?: ApiResult.ServerError("Unrecognized exception")
    }

    override suspend fun removeObject(simulationId: SimulationId, objectId: ObjectId): ApiResult<List<ObjectId>> {
        val id = simulationId.toIntOrNull() ?: return ApiResult.ClientError("Simulation id must be a number")

        return runCatching {
            ApiResult.Success(simulations[id].removeObject(objectId))
        }.getOrElse { e ->
            ApiResult.ServerError(e.message ?: "Unrecognized exception")
        }
    }

    override suspend fun state(simulationId: SimulationId): ApiResult<SimulationState> {
        val id = simulationId.toIntOrNull() ?: return ApiResult.ClientError("Simulation id must be a number")

        return runCatching {
            val sim = simulations[id]
            val state = SimulationState(
                hosts = sim.myHosts,
                switches = sim.mySwitches,
                links = sim.myLinks,
                connections = sim.myConnections
            )
            ApiResult.Success(state)
        }.getOrElse { e ->
            ApiResult.ServerError(e.message ?: "Unrecognized exception")
        }
    }

    override suspend fun simulate(simulationId: SimulationId): ApiResult<SimpleSimulationResult> {
        val id = simulationId.toIntOrNull() ?: return ApiResult.ClientError("Simulation id must be a number")

        return runCatching {
            val sim = simulations[id]
            ApiResult.Success(sim.simulate())
        }.getOrElse { e ->
            ApiResult.ServerError(e.message ?: "Unrecognized exception")
        }
    }

    override suspend fun authenticate(
        login: String,
        password: String,
    ): ApiResult<ShortUserInfo> = ApiResult.Success(ShortUserInfo(1L, login))

    override suspend fun register(
        login: String,
        password: String,
    ): ApiResult<ShortUserInfo> = ApiResult.Success(ShortUserInfo(1L, login))

    override suspend fun updatePassword(
        oldPassword: String,
        newPassword: String,
    ): ApiResult<Unit> = ApiResult.ServerError("Unsupported feature")

    override suspend fun logout(): ApiResult<Unit> = ApiResult.ServerError("Unsupported feature")

    override suspend fun shortUserInfo(
        oldPassword: String,
        newPassword: String,
    ): ApiResult<ShortUserInfo> = ApiResult.ServerError("Unsupported feature")

    @OptIn(ExperimentalTime::class)
    override suspend fun listHistory(
        beforeEntryId: HistoryEntryId?,
        limit: Int?,
    ): ApiResult<HistoryEntryList> = ApiResult.Success(HistoryEntryList(listOf(
        HistoryEntry(0, Instant.fromEpochSeconds(0), ""),
        HistoryEntry(1, Instant.fromEpochSeconds(0), ""),
        HistoryEntry(2, Instant.fromEpochSeconds(0), ""),
        HistoryEntry(3, Instant.fromEpochSeconds(0), "")
    ).filter { it.id < (beforeEntryId ?: (it.id + 1)) }))

    override suspend fun getHistoryEntry(entryId: HistoryEntryId): ApiResult<HistoryEntry> =
        ApiResult.ServerError("Unsupported feature")
}