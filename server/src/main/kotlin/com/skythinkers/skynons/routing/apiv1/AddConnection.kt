package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.api.Connection
import com.skythinkers.skynons.routing.NonsProcessManager
import io.ktor.server.routing.Route

fun Route.addConnection(processManager: NonsProcessManager) {
    redirectRequest<Connection, Unit>(processManager, "add_connection")
}
