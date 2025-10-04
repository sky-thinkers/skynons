package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.api.AddSwitchRequestData
import com.skythinkers.skynons.routing.NonsProcessManager
import io.ktor.server.routing.Route

fun Route.addSwitch(processManager: NonsProcessManager) {
    redirectRequest<AddSwitchRequestData, Unit>(processManager, "add_switch")
}
