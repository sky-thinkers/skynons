package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.nons.NonsProcessManager
import io.ktor.server.routing.Route

fun Route.apiV1(processManager: NonsProcessManager) {
    createSimulation(processManager)
    addHost(processManager)
    addSwitch(processManager)
    addLink(processManager)
    addConnection(processManager)
    getState(processManager)
    removeObject(processManager)
    simulate(processManager)
}
