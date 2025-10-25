package com.skythinkers.skynons.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias UserId = Long

@Serializable
sealed interface UserApiMessage : ApiMessage

@Serializable
@SerialName("RegistrationRequest")
data class RegistrationRequest(
    val login: String,
    val password: String,
) : UserApiMessage

@Serializable
@SerialName("ShortUserInfo")
data class ShortUserInfo(
    val uid: UserId,
    val login: String,
) : UserApiMessage
