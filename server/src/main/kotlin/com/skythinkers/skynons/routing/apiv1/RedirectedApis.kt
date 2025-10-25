package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.api.Connection
import com.skythinkers.skynons.api.EmptyMessage
import com.skythinkers.skynons.api.Host
import com.skythinkers.skynons.api.Link
import com.skythinkers.skynons.api.RemoveObject
import com.skythinkers.skynons.api.SimulationState
import com.skythinkers.skynons.api.Switch
import com.skythinkers.skynons.routing.NonsProcessManager
import io.ktor.server.routing.Route

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
    redirectSimulationGetRequest<EmptyMessage, SimulationState>(processManager, "state")
}

fun Route.removeObject(processManager: NonsProcessManager) {
    redirectSimulationDeleteRequest<RemoveObject, EmptyMessage>(processManager, "remove_object")
}

fun Route.simulate(processManager: NonsProcessManager) {
    redirectSimulationPostRequest<EmptyMessage, Connection>(processManager, "simulate")
}
