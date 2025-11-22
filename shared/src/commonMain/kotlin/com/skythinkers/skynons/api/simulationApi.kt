package com.skythinkers.skynons.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias SimulationId = String

typealias ObjectId = String

typealias SvgData = String

sealed interface NetworkObject

@Serializable
sealed interface SimulationApiMessage : ApiMessage

@Serializable
@SerialName("SimulationResultRequest")
data class SimulationResultRequest(
    @SerialName("output_dir") val outputDir: String,
) : SimulationApiMessage

@Serializable
@SerialName("SimpleSimulationResult")
data class SimpleSimulationResult(
    val cwnd: SvgData,
    val packetReordering: SvgData,
    val rate: SvgData,
    val rtt: SvgData,
) : SimulationApiMessage

typealias SpeedString = String

typealias SizeString = String

@Serializable
@SerialName("RemoveObject")
data class RemoveObject(
    val id: ObjectId
) : SimulationApiMessage

@Serializable
@SerialName("RemovedObjectList")
data class RemovedObjectList(
    val ids: List<ObjectId>
) : SimulationApiMessage

@Serializable
@SerialName("CreateSimulationResponseData")
data class CreateSimulationResponseData(
    val id: SimulationId,
) : SimulationApiMessage

@Serializable
@SerialName("Host")
data class Host(
    val name: ObjectId,
) : SimulationApiMessage, NetworkObject

@Serializable
@SerialName("Switch")
data class Switch(
    val name: ObjectId,
) : SimulationApiMessage, NetworkObject

@Serializable
@SerialName("Link")
data class Link(
    val name: ObjectId,
    @SerialName("from_id") val fromId: ObjectId,
    @SerialName("to_id") val toId: ObjectId,
    val speed: SpeedString,
) : SimulationApiMessage, NetworkObject

@Serializable
@SerialName("Connection")
data class Connection(
    val name: ObjectId,
    @SerialName("sender_id") val senderId: ObjectId,
    @SerialName("receiver_id") val receiverId: SpeedString,
    @SerialName("data_to_send") val sizeToSend: SizeString,
) : SimulationApiMessage, NetworkObject

@Serializable
@SerialName("SimulationStateRequest")
data object SimulationStateRequest : SimulationApiMessage

@Serializable
@SerialName("SimulationState")
data class SimulationState(
    val hosts: List<Host>,
    val switches: List<Switch>,
    val links: List<Link>,
    val connections: List<Connection>,
    val result: SimpleSimulationResult? = null,
) : SimulationApiMessage

@Serializable
@SerialName("SaveSimulationRequest")
data class SaveSimulationRequest(
    @SerialName("output_dir") val outputDir: String,
) : SimulationApiMessage

@Serializable
@SerialName("RestoreSimulationRequest")
data class RestoreSimulationRequest(
    @SerialName("config_path") val configPath: String,
) : SimulationApiMessage
