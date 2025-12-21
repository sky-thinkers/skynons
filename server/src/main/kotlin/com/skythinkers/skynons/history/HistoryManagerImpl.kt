package com.skythinkers.skynons.history

import com.skythinkers.skynons.api.HistoryEntry
import com.skythinkers.skynons.api.HistoryEntryId
import com.skythinkers.skynons.api.SimpleSimulationResult
import com.skythinkers.skynons.api.UserId
import com.skythinkers.skynons.database.DatabaseResult
import com.skythinkers.skynons.database.SkynonsDatabase
import com.skythinkers.skynons.database.asException
import com.skythinkers.skynons.database.asResult
import com.skythinkers.skynons.model.history.ExtractedHistoryEntryData
import com.skythinkers.skynons.model.history.HistoryEntryData
import com.skythinkers.skynons.storage.BlobStorage
import com.skythinkers.skynons.storage.readText
import com.skythinkers.skynons.storage.writeText
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.serialization.json.Json
import kotlin.time.Instant

class HistoryManagerImpl(
    private val database: SkynonsDatabase,
    private val blobStorage: BlobStorage,
    private val serializer: Json,
) : HistoryManager {
    private val logger = KtorSimpleLogger("HistoryManagerImpl")

    override suspend fun storeEntry(
        owner: UserId,
        config: String,
        result: SimpleSimulationResult?,
        now: Instant,
    ): Result<HistoryEntryId> {
        val configRef = blobStorage.writeText(owner, config)
            .getOrElse { return errorRes("Failed to store config", it) }
        val simulationResultRef = result?.run {
            blobStorage.writeText(owner, serializer.encodeToString(result))
                .getOrElse { return errorRes("Failed to store simulation result", it) }
        }

        val entryId = database.storeHistoryEntry(
            HistoryEntryData(
                owner,
                now,
                configRef,
                simulationResultRef
            )
        ).asResult().getOrElse { return errorRes("Failed to store entry", it) }

        return Result.success(entryId)
    }

    override suspend fun listLastEntries(
        owner: UserId,
        limit: Int,
        withResults: Boolean,
    ): Result<List<HistoryEntry>> {
        return when (val res = database.listHistoryLastEntries(owner, limit)) {
            is DatabaseResult.Success<List<ExtractedHistoryEntryData>> -> mapExtractedData(owner, res.res, withResults)
            !is DatabaseResult.Error -> throw AssertionError("unreachable")
            is DatabaseResult.NotFound -> Result.failure(HistoryEntryNotFoundException("No such entry", res.asException()))
            else -> errorRes("Unknown error", res.asException())
        }
    }

    override suspend fun listEntriesBefore(
        owner: UserId,
        beforeId: HistoryEntryId,
        limit: Int,
        withResults: Boolean,
    ): Result<List<HistoryEntry>> {
        return when (val res = database.listHistoryBeforeEntryId(owner, beforeId, limit)) {
            is DatabaseResult.Success<List<ExtractedHistoryEntryData>> -> mapExtractedData(owner, res.res, withResults)
            !is DatabaseResult.Error -> throw AssertionError("unreachable")
            is DatabaseResult.NotFound -> Result.failure(HistoryEntryNotFoundException("No such entry", res.asException()))
            else -> errorRes("Unknown error", res.asException())
        }
    }

    override suspend fun getEntryById(owner: UserId, id: HistoryEntryId): Result<HistoryEntry> {
        return when (val res = database.getHistoryEntryById(owner, id)) {
            is DatabaseResult.Success<ExtractedHistoryEntryData> -> mapExtractedData(owner, res.res, withResults = true)
            !is DatabaseResult.Error -> throw AssertionError("unreachable")
            is DatabaseResult.NotFound -> Result.failure(HistoryEntryNotFoundException("No such entry", res.asException()))
            else -> errorRes("Unknown error", res.asException())
        }
    }

    private suspend fun mapExtractedData(
        owner: UserId,
        extractedData: ExtractedHistoryEntryData,
        withResults: Boolean,
    ): Result<HistoryEntry> {
        val config = blobStorage.readText(owner, extractedData.configRef)
            .getOrElse { return errorRes("Failed to read config blob", it) }
        val results = if (withResults) {
            extractedData.resultRef?.let { blobStorage.readText(owner, it) }
                ?.mapCatching { serializer.decodeFromString<SimpleSimulationResult>(it) }
                ?.getOrElse { return errorRes("Failed to read results blob", it) }
        } else null

        return Result.success(
            HistoryEntry(
                id = extractedData.id,
                timestamp = extractedData.timestamp,
                config = config,
                result = results
            )
        )
    }

    private suspend fun mapExtractedData(
        owner: UserId,
        list: List<ExtractedHistoryEntryData>,
        withResults: Boolean,
    ): Result<List<HistoryEntry>> {
        return list.map {
            mapExtractedData(owner, it, withResults)
                .getOrElse { return Result.failure(it) }
        }.let { Result.success(it) }
    }

    private fun errorRes(message: String, e: Throwable): Result<Nothing> {
        val err = IllegalStateException(message, e)
        logger.warn("Failure in underling component", err)
        return Result.failure(err)
    }
}