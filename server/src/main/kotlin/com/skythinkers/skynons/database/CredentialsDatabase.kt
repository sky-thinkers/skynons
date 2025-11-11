package com.skythinkers.skynons.database

import com.skythinkers.skynons.api.ShortUserInfo
import com.skythinkers.skynons.api.UserId
import com.skythinkers.skynons.model.user.GeneratedToken
import com.skythinkers.skynons.model.user.UserCredentials
import com.skythinkers.skynons.model.user.UserCredentialsRef
import com.skythinkers.skynons.model.user.UserRegistrationInfo
import com.skythinkers.skynons.model.user.UserUpdatePasswordInfo
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface CredentialsDatabase {
    suspend fun <T> registerUser(
        info: UserRegistrationInfo,
        tokenGenerator: (uid: UserId) -> GeneratedToken<T>,
    ): DatabaseResult<Pair<T, ShortUserInfo>>

    suspend fun getUserCredentialsByLogin(login: String): DatabaseResult<UserCredentials>

    suspend fun getUserCredentialsByUserId(uid: UserId): DatabaseResult<UserCredentials>

    suspend fun updateUserPassword(updateInfo: UserUpdatePasswordInfo): DatabaseResult<Unit>

    suspend fun registerToken(
        tokenHash: ByteArray,
        expirationDate: Instant,
        forCredentials: UserCredentialsRef,
    ): DatabaseResult<Unit>

    suspend fun invalidateToken(tokenHash: ByteArray): DatabaseResult<Unit>

    suspend fun validateToken(tokenHash: ByteArray, now: Instant = Clock.System.now()): DatabaseResult<Boolean>

    suspend fun removeExpiredTokens(byDate: Instant = Clock.System.now()): DatabaseResult<Unit>
}
