package com.skythinkers.skynons.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ApiMessage

@Serializable
@SerialName("ErrorResponseData")
data class ErrorResponseData(val err: String) : ApiMessage, SimulationApiMessage, UserApiMessage, HistoryApiMessage

@Serializable
@SerialName("Empty")
data object EmptyMessage : ApiMessage, SimulationApiMessage, UserApiMessage, HistoryApiMessage
