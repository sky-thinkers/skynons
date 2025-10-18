package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.api.Switch
import com.skythinkers.skynons.routing.NonsProcessManager
import io.ktor.server.routing.Route

fun Route.addSwitch(processManager: NonsProcessManager) {
    redirectRequest<Switch, Unit>(processManager, "add_switch")
}
