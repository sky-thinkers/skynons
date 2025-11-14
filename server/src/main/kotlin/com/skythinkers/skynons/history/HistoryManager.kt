package com.skythinkers.skynons.history

import com.skythinkers.skynons.api.HistoryEntry
import com.skythinkers.skynons.api.HistoryEntryId
import com.skythinkers.skynons.api.SimpleSimulationResult
import com.skythinkers.skynons.api.UserId
import kotlin.time.Clock
import kotlin.time.Instant

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
    ): Result<List<HistoryEntry>>

    suspend fun listEntriesBefore(
        owner: UserId,
        beforeId: HistoryEntryId,
        limit: Int,
    ): Result<List<HistoryEntry>>

    suspend fun getEntryById(owner: UserId, id: HistoryEntryId): Result<HistoryEntry>

    companion object {
        const val DEFAULT_LIMIT: Int = 50
    }
}
