@file:OptIn(ExperimentalContracts::class)

package com.skythinkers.skynons.api

import com.skythinkers.skynons.data.SvgData
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

interface SkynonsClientApi {
    suspend fun simulate(config: String): ApiResult<SimpleSimulationResult>
}

data class SimpleSimulationResult(
    val cwnd: SvgData,
    val packetReordering: SvgData,
    val rate: SvgData,
    val rtt: SvgData,
) {
    @Deprecated("Use val cwnd instead", replaceWith = ReplaceWith("cwnd"))
    val cvnd: SvgData get() = cwnd
}

sealed interface ApiResult<out T> {
    data class Success<out T>(val result: T) : ApiResult<T>

    sealed interface Error : ApiResult<Nothing> {
        val message: String
    }

    data class MalformedResponseError(override val message: String) : Error

    data class ClientError(override val message: String) : Error

    data class ServerError(override val message: String) : Error
}

class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

fun <T> ApiResult<T>.resultOrThrow(): T {
    contract {
        returns() implies (this@resultOrThrow is ApiResult.Success<*>)
    }

    return when (this) {
        is ApiResult.Success<T> -> result
        is ApiResult.Error -> throw ApiException(message)
    }
}

fun <T> ApiResult<T>.resultOrNull(): T? {
    contract {
        returnsNotNull() implies (this@resultOrNull is ApiResult.Success<*>)
    }

    return when (this) {
        is ApiResult.Success<T> -> result
        is ApiResult.Error -> null
    }
}

fun ApiResult<*>.errorOrNull(): ApiResult.Error? {
    contract {
        returnsNotNull() implies (this@errorOrNull is ApiResult.Error)
    }

    return when (this) {
        is ApiResult.Success<*> -> null
        is ApiResult.Error -> this
    }
}

fun ApiResult<*>.isSuccess(): Boolean {
    contract {
        returns(true) implies (this@isSuccess is ApiResult.Success<*>)
        returns(false) implies (this@isSuccess is ApiResult.Error)
    }
    return this is ApiResult.Success<*>
}

fun ApiResult<*>.isFailure(): Boolean {
    contract {
        returns(true) implies (this@isFailure is ApiResult.Success<*>)
        returns(false) implies (this@isFailure is ApiResult.Error)
    }
    return this is ApiResult.Error
}
