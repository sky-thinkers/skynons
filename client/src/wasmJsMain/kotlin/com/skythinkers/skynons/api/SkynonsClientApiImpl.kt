package com.skythinkers.skynons.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.js.Js
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.contentType
import io.ktor.utils.io.readBuffer
import io.ktor.utils.io.readText

class SkynonsClientApiImpl(
    private val apiAddress: String = LOCAL_API_ADDRESS,
    private val httpClient: HttpClient = HttpClient(Js),
) : SkynonsClientApi {

    override suspend fun simulate(config: String): ApiResult<SimpleSimulationResult> {
        val response = httpClient.post(apiAddress + SIMULATE_ENDPOINT) {
            setBody(config)
            contentType(ContentType.Application.Yaml)
        }

        suspend fun PartData.readText(): String = when (this) {
            is PartData.BinaryChannelItem -> this.provider().readBuffer().readText()
            is PartData.BinaryItem -> this.provider().readText()
            is PartData.FileItem -> this.provider().readBuffer().readText()
            is PartData.FormItem -> this.value
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                var cwnd: SvgData? = null
                var packetReordering: SvgData? = null
                var rate: SvgData? = null
                var rtt: SvgData? = null
                val data = response.body<MultiPartData>()
                var part = data.readPart()
                while (part != null) {
                    when (part.name) {
                        "cwnd.svg" -> cwnd = (part.readText())
                        "packet_reordering.svg" -> packetReordering = (part.readText())
                        "rate.svg" -> rate = (part.readText())
                        "rtt.svg" -> rtt = (part.readText())
                    }
                    part.dispose()
                    part = data.readPart()
                }
                ApiResult.Success(
                    SimpleSimulationResult(
                        cwnd ?: return ApiResult.MalformedResponseError("Missing cwnd.svg in response"),
                        packetReordering
                            ?: return ApiResult.MalformedResponseError("Missing packet_reordering.svg in response"),
                        rate ?: return ApiResult.MalformedResponseError("Missing rate.svg in response"),
                        rtt ?: return ApiResult.MalformedResponseError("Missing rtt.svg in response"),
                    )
                )
            }

            HttpStatusCode.BadRequest -> ApiResult.ClientError(response.bodyAsText())

            HttpStatusCode.InternalServerError -> ApiResult.ServerError(response.bodyAsText())

            else -> ApiResult.MalformedResponseError("Unexpected server response code ${response.status}")
        }
    }

    override suspend fun createSimulation(): ApiResult<SimulationId> =
        ApiResult.ClientError("Not implemented")

    override suspend fun addHost(
        simulationId: SimulationId,
        name: ObjectId,
    ): ApiResult<Unit> = ApiResult.ClientError("Not implemented")


    override suspend fun addSwitch(
        simulationId: SimulationId,
        name: ObjectId,
    ): ApiResult<Unit> = ApiResult.ClientError("Not implemented")

    override suspend fun addLink(
        fromId: ObjectId,
        toId: ObjectId,
        speed: SpeedString,
    ): ApiResult<Unit> = ApiResult.ClientError("Not implemented")

    override suspend fun addConnection(
        senderId: ObjectId,
        receiverId: ObjectId,
        sizeToSend: SizeString,
    ): ApiResult<Unit> = ApiResult.ClientError("Not implemented")

    override suspend fun simulate(): ApiResult<SimpleSimulationResult> =
        ApiResult.ClientError("Not implemented")

    companion object {
        private const val LOCAL_API_ADDRESS = "http://localhost:8090"
        private const val API_V0_PREFIX = "/api/v0"
        private const val API_V1_PREFIX = "/api/v1"
        private const val SIMULATE_ENDPOINT = "$API_V0_PREFIX/simulate"
    }
}
