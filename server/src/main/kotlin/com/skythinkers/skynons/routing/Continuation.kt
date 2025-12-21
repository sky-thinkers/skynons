package com.skythinkers.skynons.routing

import com.skythinkers.skynons.api.ErrorResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.cont(continuationManager: ContinuationManager) {
    post("continuation") {
        val token = call.request.headers["X-Continuation"] ?: run {
            call.respond(HttpStatusCode.BadRequest, ErrorResponseData("No token"))
            return@post
        }

        continuationManager.tryConsumeToken(call, token)
    }
}