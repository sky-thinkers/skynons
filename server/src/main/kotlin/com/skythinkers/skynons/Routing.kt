package com.skythinkers.skynons

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        route("/api/v0") {
            post("/simulate") {
                val text = call.receiveText()
                call.respondText(text)
            }
        }
    }
}
