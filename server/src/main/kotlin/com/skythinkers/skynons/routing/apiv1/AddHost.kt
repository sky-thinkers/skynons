package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.api.EmptyMessage
import com.skythinkers.skynons.api.Host
import com.skythinkers.skynons.routing.NonsProcessManager
import io.ktor.server.routing.Route

fun Route.addHost(processManager: NonsProcessManager) {
    redirectRequest<Host, EmptyMessage>(processManager, "add_host")
}
