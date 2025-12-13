package com.skythinkers.skynons.model.history

import com.skythinkers.skynons.api.HistoryEntryId
import com.skythinkers.skynons.api.UserId
import com.skythinkers.skynons.storage.BlobRef
import kotlin.time.Instant

class HistoryEntryData(
    val owner: UserId,
    val timestamp: Instant,
    val configRef: BlobRef,
    val resultRef: BlobRef?,
)

class ExtractedHistoryEntryData(
    val id: HistoryEntryId,
    val timestamp: Instant,
    val configRef: BlobRef,
    val resultRef: BlobRef?,
)
