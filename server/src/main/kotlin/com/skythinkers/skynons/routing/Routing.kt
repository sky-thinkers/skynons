package com.skythinkers.skynons.routing

import com.skythinkers.skynons.routing.apiv1.apiV1
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    val nonsPath =
            System.getenv("NONS_PATH")?.takeUnless(String::isEmpty) ?: "../backend/build/nons"
    routing {
        route("api") {
            route("v0") { apiV0(nonsPath) }
            route("v1") { apiV1(nonsPath) }
        }
    }
}
