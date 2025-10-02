package com.skythinkers.skynons

import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable data class ErrorResponse(val err: String)

fun Route.ApiV1(nonsPath: String) {
    val processManager = NonsProcessManager(nonsPath)
    CreateSimulation(processManager)
    AddHost(processManager)
}
