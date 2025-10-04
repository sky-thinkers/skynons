package com.skythinkers.skynons.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias SimulationId = String

typealias ObjectId = String

typealias SvgData = String

@Serializable data class ErrorResponseData(val err: String)

@Serializable
data class SimpleSimulationResult(
        val cwnd: SvgData,
        val packetReordering: SvgData,
        val rate: SvgData,
        val rtt: SvgData,
)

typealias SpeedString = String

typealias SizeString = String

@Serializable
data class CreateSimulationResponseData(
        val id: SimulationId,
)

@Serializable
data class AddHostRequestData(
        val name: ObjectId,
)

@Serializable
data class AddSwitchRequestData(
        val name: ObjectId,
)

@Serializable
data class AddLinkRequestData(
        val name: ObjectId,
        @SerialName("from_id") val fromId: ObjectId,
        @SerialName("to_id") val toId: ObjectId,
        val speed: SpeedString,
)

@Serializable
data class AddConnectionRequestData(
        val name: ObjectId,
        @SerialName("sender_id") val senderId: ObjectId,
        @SerialName("receiver_id") val receiverId: SpeedString,
        @SerialName("data_to_send") val sizeToSend: SizeString,
)
