package com.skythinkers.skynons.model.user

import com.skythinkers.skynons.api.UserId
import kotlin.time.Instant

/**
 * For internal use only. Not serializable for security reasons
 */
class UserCredentials(
    val uid: UserId,
    val login: String,
    val salt: ByteArray,
    val passwordHash: ByteArray,
    val version: Long,
)

/**
 * For internal use only. Not serializable for security reasons
 */
class UserCredentialsRef(
    val uid: UserId,
    val version: Long,
)

/**
 * For internal use only. Not serializable for security reasons
 */
class UserRegistrationInfo(
    val login: String,
    val salt: ByteArray,
    val passwordHash: ByteArray,
)

/**
 * For internal use only. Not serializable for security reasons
 */
class UserUpdatePasswordInfo(
    val uid: UserId,
    val salt: ByteArray,
    val passwordHash: ByteArray,
    val previousVersion: Long,
)

/**
 * For internal use only. Not serializable for security reasons
 */
class GeneratedToken<T>(
    val token: T,
    val hash: ByteArray,
    val expiresAt: Instant,
)

fun UserCredentials.toRef(): UserCredentialsRef = UserCredentialsRef(uid = uid, version = version)
