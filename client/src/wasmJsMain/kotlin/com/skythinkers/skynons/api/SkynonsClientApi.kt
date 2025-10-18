@file:OptIn(ExperimentalContracts::class)

package com.skythinkers.skynons.api

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

interface SkynonsClientApi {
    // v0
    suspend fun simulateConfig(config: String): ApiResult<SimpleSimulationResult>

    // v1
    suspend fun createSimulation(): ApiResult<SimulationId>
    suspend fun addHost(simulationId: SimulationId, name: ObjectId): ApiResult<Unit>
    suspend fun addSwitch(simulationId: SimulationId, name: ObjectId): ApiResult<Unit>
    suspend fun addLink(
        simulationId: SimulationId,
        name: ObjectId,
        fromId: ObjectId,
        toId: ObjectId,
        speed: SpeedString,
    ): ApiResult<Unit>

    suspend fun addConnection(
        simulationId: SimulationId,
        name: ObjectId,
        senderId: ObjectId,
        receiverId: ObjectId,
        sizeToSend: SizeString,
    ): ApiResult<Unit>

    suspend fun removeObject(simulationId: SimulationId, objectId: ObjectId): ApiResult<Unit>

    suspend fun state(simulationId: SimulationId): ApiResult<SimulationState>

    suspend fun simulate(simulationId: SimulationId): ApiResult<SimpleSimulationResult>
}

sealed interface ApiResult<out T> {
    data class Success<out T>(val result: T) : ApiResult<T>

    sealed interface Error : ApiResult<Nothing> {
        val message: String
    }

    data class MalformedResponseError(override val message: String) : Error

    data class ClientError(override val message: String) : Error

    data class ServerError(override val message: String) : Error

    data class NotFound(override val message: String) : Error
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
