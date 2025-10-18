package com.skythinkers.skynons.routing.apiv1

import com.skythinkers.skynons.api.Link
import com.skythinkers.skynons.routing.NonsProcessManager
import io.ktor.server.routing.Route

fun Route.addLink(processManager: NonsProcessManager) {
    redirectRequest<Link, Unit>(processManager, "add_link")
}
