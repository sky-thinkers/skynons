package com.skythinkers.skynons

import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.io.path.*

fun Application.configureRouting() {
    val nonsPath =
            System.getenv("NONS_PATH")?.takeUnless(String::isEmpty) ?: "../backend/build/nons"
    routing {
        route("api") {
            route("v0") { ApiV0(nonsPath) }
            route("v1") { ApiV1(nonsPath) }
        }
    }
}
