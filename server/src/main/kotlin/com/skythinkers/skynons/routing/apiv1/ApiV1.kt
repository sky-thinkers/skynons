package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.history.HistoryManager
import com.skythinkers.skynons.nons.NonsProcessManager
import com.skythinkers.skynons.routing.ContinuationManager
import io.ktor.server.routing.Route

fun Route.apiV1(
    processManager: NonsProcessManager,
    historyManager: HistoryManager,
    continuationManager: ContinuationManager,
) {
    createSimulation(processManager)
    createSimulationWithConfig(processManager)
    restoreFromHistory(processManager, historyManager)
    addHost(processManager)
    addSwitch(processManager)
    addLink(processManager)
    addConnection(processManager)
    getState(processManager)
    removeObject(processManager)
    simulate(processManager, historyManager, continuationManager)
    stopProcess(processManager)
    suspendProcess(processManager)
}
