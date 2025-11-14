package com.skythinkers.skynons.database

import com.skythinkers.skynons.database.DatabaseResult.Success

sealed interface DatabaseResult<out T> {
    sealed interface Error : DatabaseResult<Nothing> {
        val message: String
        val cause: Throwable? get() = null
    }

    data class Success<out T>(val res: T) : DatabaseResult<T>

    data class NotFound(override val message: String) : Error

    data class ConnectionError(override val message: String, override val cause: Throwable? = null) : Error

    data class GeneralError(override val message: String, override val cause: Throwable? = null) : Error

    data class ConstraintViolation(val constraint: Constraint, override val cause: Throwable? = null) : Error {
        override val message: String
            get() = "Constraint violation: $constraint"
    }

}

fun <T> DatabaseResult<T>.asResult(): Result<T> = when (this) {
    is Success<T> -> Result.success(res)
    is DatabaseResult.Error -> Result.failure(asException())
}

fun DatabaseResult.Error.asException(): DatabaseException = DatabaseException(this)

val <T> DatabaseResult<T>.isSuccess: Boolean
    get() = when (this) {
        is Success<T> -> true
        is DatabaseResult.Error -> false
    }

val <T> DatabaseResult<T>.isFailure: Boolean
    get() = when (this) {
        is Success<T> -> false
        is DatabaseResult.Error -> true
    }

class DatabaseException(val error: DatabaseResult.Error) : Exception(error.message, error.cause)
