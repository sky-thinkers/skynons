package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.routing.NonsProcessManager
import io.ktor.server.routing.Route
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

fun Route.apiV1(nonsPath: String) {
    val processManager = NonsProcessManager(nonsPath, CoroutineScope(Dispatchers.IO))
    createSimulation(processManager)
    addHost(processManager)
    addSwitch(processManager)
    addLink(processManager)
    addConnection(processManager)
    simulate(processManager)
}
