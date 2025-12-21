package com.skythinkers.skynons.history

import com.skythinkers.skynons.api.HistoryEntry
import com.skythinkers.skynons.api.HistoryEntryId
import com.skythinkers.skynons.api.SimpleSimulationResult
import com.skythinkers.skynons.api.UserId
import java.lang.Exception
import kotlin.time.Clock
import kotlin.time.Instant

class HistoryEntryNotFoundException(msg: String, cause: Throwable) : Exception(msg, cause)

interface HistoryManager {
    suspend fun storeEntry(
        owner: UserId,
        config: String,
        result: SimpleSimulationResult?,
        now: Instant = Clock.System.now(),
    ): Result<HistoryEntryId>

    suspend fun listLastEntries(
        owner: UserId,
        limit: Int,
        withResults: Boolean,
    ): Result<List<HistoryEntry>>

    suspend fun listEntriesBefore(
        owner: UserId,
        beforeId: HistoryEntryId,
        limit: Int,
        withResults: Boolean,
    ): Result<List<HistoryEntry>>

    suspend fun getEntryById(owner: UserId, id: HistoryEntryId): Result<HistoryEntry>

    companion object {
        const val DEFAULT_LIMIT: Int = 50
    }
}
