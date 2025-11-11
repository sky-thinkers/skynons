package com.skythinkers.skynons.auth

abstract class AuthException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

class AuthFailedException(
    message: String? = null,
    cause: Throwable? = null,
) : AuthException(
    message = message,
    cause = cause
)

class AccountException(
    message: String? = null,
    cause: Throwable? = null,
) : AuthException(
    message = message,
    cause = cause
)
