package com.skythinkers.skynons.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

typealias HistoryEntryId = Long

@Serializable
sealed interface HistoryApiMessage : ApiMessage

@Serializable
@SerialName("HistoryEntry")
data class HistoryEntry(
    val id: HistoryEntryId,
    val timestamp: Instant,
    val config: String,
    val result: SimpleSimulationResult? = null,
) : HistoryApiMessage

@Serializable
@SerialName("HistoryBeforeEntryRequest")
data class HistoryBeforeEntryRequest(
    val entryId: HistoryEntryId,
) : HistoryApiMessage

@Serializable
@SerialName("HistoryEntryList")
data class HistoryEntryList(
    val entries: List<HistoryEntry>,
) : HistoryApiMessage
