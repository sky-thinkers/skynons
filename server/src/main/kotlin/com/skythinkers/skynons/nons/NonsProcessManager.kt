package com.skythinkers.skynons.nons

import com.skythinkers.skynons.api.SimulationApiMessage

typealias ProcessId = Int

interface NonsProcessManager : AutoCloseable {
    suspend fun createSimulation(): ProcessId?
    fun checkId(id: ProcessId): Boolean
    suspend fun message(id: ProcessId, message: SimulationApiMessage): SimulationApiMessage
}