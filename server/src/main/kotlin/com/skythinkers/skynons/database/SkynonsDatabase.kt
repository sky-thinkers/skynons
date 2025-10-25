package com.skythinkers.skynons.database

import com.skythinkers.skynons.api.ShortUserInfo
import com.skythinkers.skynons.api.UserId

interface SkynonsDatabase {
    suspend fun getUserInfoByUserId(uid: UserId): DatabaseResult<ShortUserInfo>
    suspend fun getUserInfoByLogin(login: String): DatabaseResult<ShortUserInfo>
}
