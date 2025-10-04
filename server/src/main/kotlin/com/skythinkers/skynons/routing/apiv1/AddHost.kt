package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.api.AddHostRequestData
import com.skythinkers.skynons.routing.NonsProcessManager
import io.ktor.server.routing.Route

fun Route.addHost(processManager: NonsProcessManager) {
    RedirectRequest<AddHostRequestData, Unit>(processManager, "add_host")
}
