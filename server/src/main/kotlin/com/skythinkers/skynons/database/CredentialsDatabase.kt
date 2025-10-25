package com.skythinkers.skynons.database

import com.skythinkers.skynons.api.ShortUserInfo
import com.skythinkers.skynons.api.UserId
import com.skythinkers.skynons.model.user.UserCredentials
import com.skythinkers.skynons.model.user.UserRegistrationInfo
import com.skythinkers.skynons.model.user.UserUpdatePasswordInfo
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface CredentialsDatabase {
    suspend fun registerUser(info: UserRegistrationInfo): DatabaseResult<ShortUserInfo>

    suspend fun getUserCredentialsByLogin(login: String): DatabaseResult<UserCredentials>

    suspend fun getUserCredentialsByUserId(uid: UserId): DatabaseResult<UserCredentials>

    suspend fun updateUserPassword(updateInfo: UserUpdatePasswordInfo): DatabaseResult<Unit>

    suspend fun registerToken(tokenHash: ByteArray, expirationDate: Instant): DatabaseResult<Unit>

    suspend fun invalidateToken(tokenHash: ByteArray): DatabaseResult<Unit>

    suspend fun getTokenExpirationDate(tokenHash: ByteArray): DatabaseResult<Instant?>

    suspend fun removeExpiredTokens(byDate: Instant = Clock.System.now()): DatabaseResult<Unit>
}
