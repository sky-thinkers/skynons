package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.api.EmptyMessage
import com.skythinkers.skynons.api.Switch
import com.skythinkers.skynons.routing.NonsProcessManager
import io.ktor.server.routing.Route

fun Route.addSwitch(processManager: NonsProcessManager) {
    redirectRequest<Switch, EmptyMessage>(processManager, "add_switch")
}
