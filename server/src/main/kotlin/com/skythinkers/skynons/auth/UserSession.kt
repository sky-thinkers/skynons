package com.skythinkers.skynons.auth

import com.auth0.jwt.JWT
import com.skythinkers.skynons.api.UserId
import kotlinx.serialization.Serializable

@Serializable
data class UserSession(val token: String) {
    companion object {
        const val USER_ID_CLAIM_NAME: String = "uid"
        const val USER_SESSION: String = "user_session"
    }
}

val UserSession.uid: UserId get() = JWT.decode(token).getClaim(UserSession.USER_ID_CLAIM_NAME).asLong()
    ?: throw SecurityException("Cannot decode user id from token")