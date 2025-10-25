package com.skythinkers.skynons.api

class SimulationServerMock {
    var myHosts = emptyList<Host>()
    var mySwitches = emptyList<Switch>()
    var myLinks = emptyList<Link>()
    var myConnections = emptyList<Connection>()

    private fun deviceExists(name: String): Boolean {
        return myHosts.map { it.name }.contains(name) ||
                mySwitches.map { it.name }.contains(name) ||
                myLinks.map { it.name }.contains(name) ||
                myConnections.map { it.name }.contains(name)
    }

    private fun dataHolderExists(name: String): Boolean {
        return myHosts.map { it.name }.contains(name) ||
                mySwitches.map { it.name }.contains(name)
    }

    fun addHost(host: Host) {
        if (deviceExists(host.name)) {
            throw Exception("Device with this id already exists")
        }
        myHosts += host
    }

    fun addSwitch(switch: Switch) {
        if (deviceExists(switch.name)) {
            throw Exception("Device with this id already exists")
        }
        mySwitches += switch
    }

    fun addLink(link: Link) {
        if (deviceExists(link.name)) {
            throw Exception("Device with this id already exists")
        }
        if (!dataHolderExists(link.fromId)) {
            throw Exception("There is no sender with this id")
        }
        if (!dataHolderExists(link.toId)) {
            throw Exception("There is no receiver with this id")
        }
        myLinks += link
    }

    fun addConnection(connection: Connection) {
        if (deviceExists(connection.name)) {
            throw Exception("Device with this id already exists")
        }
        if (!dataHolderExists(connection.senderId)) {
            throw Exception("There is no sender with this id")
        }
        if (!dataHolderExists(connection.receiverId)) {
            throw Exception("There is no receiver with this id")
        }
        myConnections += connection
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

    override suspend fun simulateConfig(config: String): ApiResult<SimpleSimulationResult> {
        TODO("V0 feature")
    }

    override suspend fun createSimulation(): ApiResult<SimulationId> {
        val result = ApiResult.Success(simulations.size.toString())
        simulations += SimulationServerMock()
        return result
    }

    override suspend fun addHost(simulationId: SimulationId, name: ObjectId): ApiResult<Unit> {
        val id = simulationId.toIntOrNull() ?: return ApiResult.ClientError("Simulation id must be a number")

        try {
            simulations[id].addHost(Host(name))
        } catch (e: Exception) {
            return ApiResult.ServerError(e.message ?: "Unrecognized exception")
        }
        return ApiResult.Success(Unit)
    }

    override suspend fun addSwitch(simulationId: SimulationId, name: ObjectId): ApiResult<Unit> {
        val id = simulationId.toIntOrNull() ?: return ApiResult.ClientError("Simulation id must be a number")

        try {
            simulations[id].addSwitch(Switch(name))
        } catch (e: Exception) {
            return ApiResult.ServerError(e.message ?: "Unrecognized exception")
        }
        return ApiResult.Success(Unit)
    }

    override suspend fun addLink(
        simulationId: SimulationId,
        name: ObjectId,
        fromId: ObjectId,
        toId: ObjectId,
        speed: SpeedString,
    ): ApiResult<Unit> {
        val id = simulationId.toIntOrNull() ?: return ApiResult.ClientError("Simulation id must be a number")

        try {
            simulations[id].addLink(Link(name, fromId, toId, speed))
        } catch (e: Exception) {
            return ApiResult.ServerError(e.message ?: "Unrecognized exception")
        }
        return ApiResult.Success(Unit)
    }

    override suspend fun addConnection(
        simulationId: SimulationId,
        name: ObjectId,
        senderId: ObjectId,
        receiverId: ObjectId,
        sizeToSend: SizeString,
    ): ApiResult<Unit> {
        val id = simulationId.toIntOrNull() ?: return ApiResult.ClientError("Simulation id must be a number")

        try {
            simulations[id].addConnection(Connection(name, senderId, receiverId, sizeToSend))
        } catch (e: Exception) {
            return ApiResult.ServerError(e.message ?: "Unrecognized exception")
        }
        return ApiResult.Success(Unit)
    }

    override suspend fun removeObject(simulationId: SimulationId, objectId: ObjectId): ApiResult<List<ObjectId>> {
        val id = simulationId.toIntOrNull() ?: return ApiResult.ClientError("Simulation id must be a number")

        return try {
            ApiResult.Success(simulations[id].removeObject(objectId))
        } catch (e: Exception) {
            ApiResult.ServerError(e.message ?: "Unrecognized exception")
        }
    }

    override suspend fun state(simulationId: SimulationId): ApiResult<SimulationState> {
        val id = simulationId.toIntOrNull() ?: return ApiResult.ClientError("Simulation id must be a number")

        try {
            val sim = simulations[id]
            val state = SimulationState(
                hosts = sim.myHosts,
                switches = sim.mySwitches,
                links = sim.myLinks,
                connections = sim.myConnections
            )
            return ApiResult.Success(state)
        } catch (e: Exception) {
            return ApiResult.ServerError(e.message ?: "Unrecognized exception")
        }
    }

    override suspend fun simulate(simulationId: SimulationId): ApiResult<SimpleSimulationResult> {
        val id = simulationId.toIntOrNull() ?: return ApiResult.ClientError("Simulation id must be a number")

        return try {
            val sim = simulations[id]
            ApiResult.Success(sim.simulate())
        } catch (e: Exception) {
            ApiResult.ServerError(e.message ?: "Unrecognized exception")
        }
    }
}