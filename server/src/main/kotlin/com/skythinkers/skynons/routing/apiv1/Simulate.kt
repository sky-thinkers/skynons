package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.api.AddConnectionRequestData
import com.skythinkers.skynons.routing.NonsProcessManager
import io.ktor.server.routing.Route

fun Route.simulate(processManager: NonsProcessManager) {
    RedirectRequest<Unit, AddConnectionRequestData>(processManager, "simulate")
}
