package com.skythinkers.skynons.model.user

import com.skythinkers.skynons.api.UserId

/**
 * For internal use only. Not serializable for security reasons
 */
class UserCredentials(
    val uid: UserId,
    val login: String,
    val salt: ByteArray,
    val passwordHash: ByteArray
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
)
