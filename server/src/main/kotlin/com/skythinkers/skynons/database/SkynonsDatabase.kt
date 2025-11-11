package com.skythinkers.skynons.database

import com.skythinkers.skynons.api.ShortUserInfo
import com.skythinkers.skynons.api.UserId
import com.skythinkers.skynons.storage.BlobRef
import kotlin.time.Clock
import kotlin.time.Instant

interface SkynonsDatabase {
    suspend fun getUserInfoByUserId(uid: UserId): DatabaseResult<ShortUserInfo>
    suspend fun getUserInfoByLogin(login: String): DatabaseResult<ShortUserInfo>

    suspend fun ownsBlob(user: UserId, blob: BlobRef): DatabaseResult<Boolean>
    suspend fun accessBlob(user: UserId, blob: BlobRef, now: Instant = Clock.System.now()): DatabaseResult<Boolean>
    suspend fun allocateBlob(user: UserId, blob: BlobRef, now: Instant = Clock.System.now()): DatabaseResult<Unit>
}
