package com.skythinkers.skynons.auth

import com.auth0.jwt.JWTVerifier
import com.skythinkers.skynons.api.LoginRequest
import com.skythinkers.skynons.api.PasswordUpdateRequest
import com.skythinkers.skynons.api.RegistrationRequest
import com.skythinkers.skynons.api.ShortUserInfo
import com.skythinkers.skynons.api.UserId

interface AccountManager {
    val jwtVerifier: JWTVerifier

    suspend fun authenticateUser(loginRequest: LoginRequest): Result<Pair<ShortUserInfo, UserSession>>

    suspend fun registerUser(registrationRequest: RegistrationRequest): Result<Pair<ShortUserInfo, UserSession>>

    suspend fun updateUserPassword(userId: UserId, updateRequest: PasswordUpdateRequest): Result<Unit>

    suspend fun validateSession(session: UserSession): Result<Boolean>

    suspend fun invalidateSession(session: UserSession): Result<Unit>

    suspend fun getShortUserInfo(session: UserSession): Result<ShortUserInfo>

    suspend fun cleanupLoop()
}
