package com.skythinkers.skynons.nons

import com.skythinkers.skynons.api.SimulationApiMessage

typealias ProcessId = String

interface NonsProcessManager : AutoCloseable {
    suspend fun createSimulation(): ProcessId?
    fun checkId(id: ProcessId): Boolean
    suspend fun message(id: ProcessId, message: SimulationApiMessage): SimulationApiMessage
    fun killProcess(id: ProcessId)
    suspend fun suspendProcess(id: ProcessId)
}
