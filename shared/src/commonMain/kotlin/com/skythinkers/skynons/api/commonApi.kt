package com.skythinkers.skynons.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias SimulationId = String

typealias ObjectId = String

typealias SvgData = String

@Serializable
sealed interface ApiMessage

@Serializable
@SerialName("ErrorResponseData")
data class ErrorResponseData(val err: String) : ApiMessage

data object EmptyMessage : ApiMessage

@Serializable
@SerialName("SimpleSimulationResult")
data class SimpleSimulationResult(
    val cwnd: SvgData,
    val packetReordering: SvgData,
    val rate: SvgData,
    val rtt: SvgData,
) : ApiMessage

typealias SpeedString = String

typealias SizeString = String

@Serializable
@SerialName("RemoveObject")
data class RemoveObject(
    val id: ObjectId
) : ApiMessage

@Serializable
@SerialName("CreateSimulationResponseData")
data class CreateSimulationResponseData(
    val id: SimulationId,
) : ApiMessage

@Serializable
@SerialName("Host")
data class Host(
    val name: ObjectId,
) : ApiMessage

@Serializable
@SerialName("Switch")
data class Switch(
    val name: ObjectId,
) : ApiMessage

@Serializable
@SerialName("Link")
data class Link(
    val name: ObjectId,
    @SerialName("from_id") val fromId: ObjectId,
    @SerialName("to_id") val toId: ObjectId,
    val speed: SpeedString,
) : ApiMessage

@Serializable
@SerialName("Connection")
data class Connection(
    val name: ObjectId,
    @SerialName("sender_id") val senderId: ObjectId,
    @SerialName("receiver_id") val receiverId: SpeedString,
    @SerialName("data_to_send") val sizeToSend: SizeString,
) : ApiMessage

@Serializable
@SerialName("SimulationState")
data class SimulationState(
    val hosts: List<Host>,
    val switches: List<Switch>,
    val links: List<Link>,
    val connections: List<Connection>,
    val result: SimpleSimulationResult? = null,
) : ApiMessage
