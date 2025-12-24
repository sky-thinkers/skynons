package com.skythinkers.skynons.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readBuffer
import io.ktor.utils.io.readText
import kotlinx.coroutines.delay

class SkynonsClientApiImpl(
    private val apiAddress: String = LOCAL_API_ADDRESS,
    private val httpClient: HttpClient = HttpClient(Js) {
        install(ContentNegotiation) {
            json()
        }
    },
) : SkynonsClientApi {

    override suspend fun simulateConfig(config: String): ApiResult<SimpleSimulationResult> = callWrapper {
        val response = httpClient.post(apiAddress + SIMULATE_V0_ENDPOINT) {
            setBody(config)
            contentType(ContentType.Application.Yaml)
        }

        suspend fun PartData.readText(): String = when (this) {
            is PartData.BinaryChannelItem -> this.provider().readBuffer().readText()
            is PartData.BinaryItem -> this.provider().readText()
            is PartData.FileItem -> this.provider().readBuffer().readText()
            is PartData.FormItem -> this.value
        }

        return handleResponseDefault(response) {
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
    }

    override suspend fun createSimulation(): ApiResult<SimulationId> = callWrapper {
        val response = httpClient.post(apiAddress + CREATE_SIMULATION_ENDPOINT)

        return handleResponseDefault(response) { response ->
            val response = runCatching { response.body<CreateSimulationResponseData>() }
                .getOrElse { return ApiResult.MalformedResponseError("Unexpected server response: $it") }

            ApiResult.Success(response.id)
        }
    }

    override suspend fun createSimulationWithConfig(config: String): ApiResult<SimulationId> = callWrapper {
        val response = httpClient.post(apiAddress + CREATE_SIMULATION_WITH_CONFIG_ENDPOINT) {
            setBody(CreateSimulationWithConfigRequest(config))
            contentType(ContentType.Application.Json)
        }

        return handleResponseDefault(response) { response ->
            val response = runCatching { response.body<CreateSimulationResponseData>() }
                .getOrElse { return ApiResult.MalformedResponseError("Unexpected server response: $it") }

            ApiResult.Success(response.id)
        }
    }

    override suspend fun restoreSimulationFromHistoryEntry(entryId: HistoryEntryId): ApiResult<SimulationId> =
        callWrapper {
            val response = httpClient.post(apiAddress + restoreFromHistoryEntryEndpoint(entryId))

            return handleResponseDefault(response) { response ->
                val response = runCatching { response.body<CreateSimulationResponseData>() }
                    .getOrElse { return ApiResult.MalformedResponseError("Unexpected server response: $it") }

                ApiResult.Success(response.id)
            }
        }

    override suspend fun addHost(
        simulationId: SimulationId,
        name: ObjectId,
    ): ApiResult<Unit> = callWrapper {
        val response = httpClient.post(apiAddress + addHostEndpoint(simulationId)) {
            setBody(Host(name))
            contentType(ContentType.Application.Json)
        }

        return handleResponseDefault(response) { response ->
            ApiResult.Success(Unit)
        }
    }


    override suspend fun addSwitch(
        simulationId: SimulationId,
        name: ObjectId,
    ): ApiResult<Unit> = callWrapper {
        val response = httpClient.post(apiAddress + addSwitchEndpoint(simulationId)) {
            setBody(Switch(name))
            contentType(ContentType.Application.Json)
        }

        return handleResponseDefault(response) { response ->
            ApiResult.Success(Unit)
        }
    }

    override suspend fun addLink(
        simulationId: SimulationId,
        name: ObjectId,
        fromId: ObjectId,
        toId: ObjectId,
        speed: SpeedString,
    ): ApiResult<Unit> = callWrapper {
        val response = httpClient.post(apiAddress + addLinkEndpoint(simulationId)) {
            setBody(
                Link(
                    name,
                    fromId,
                    toId,
                    speed,
                )
            )
            contentType(ContentType.Application.Json)
        }

        return handleResponseDefault(response) { response ->
            ApiResult.Success(Unit)
        }
    }

    override suspend fun addConnection(
        simulationId: SimulationId,
        name: ObjectId,
        senderId: ObjectId,
        receiverId: ObjectId,
        sizeToSend: SizeString,
    ): ApiResult<Unit> = callWrapper {
        val response = httpClient.post(apiAddress + addConnectionEndpoint(simulationId)) {
            setBody(
                Connection(
                    name,
                    senderId,
                    receiverId,
                    sizeToSend,
                )
            )
            contentType(ContentType.Application.Json)
        }

        return handleResponseDefault(response) { response ->
            ApiResult.Success(Unit)
        }
    }

    override suspend fun removeObject(simulationId: SimulationId, objectId: ObjectId): ApiResult<List<ObjectId>> =
        callWrapper {
            val response = httpClient.delete(apiAddress + removeObjectEndpoint(simulationId)) {
                setBody(RemoveObject(objectId))
                contentType(ContentType.Application.Json)
            }
            return handleResponseDefault(response) { response ->
                val response = runCatching { response.body<RemovedObjectList>() }
                    .getOrElse { return ApiResult.MalformedResponseError("Unexpected server response: $it") }

                ApiResult.Success(response.ids)
            }
        }

    override suspend fun state(simulationId: SimulationId): ApiResult<SimulationState> = callWrapper {
        val response = httpClient.get(apiAddress + stateEndpoint(simulationId))

        return handleResponseDefault(response) { response ->
            val response = runCatching { response.body<SimulationState>() }
                .getOrElse { return ApiResult.MalformedResponseError("Unexpected server response: $it") }

            ApiResult.Success(response)
        }
    }

    override suspend fun simulate(simulationId: SimulationId): ApiResult<SimpleSimulationResult> = callWrapper {
        val response = httpClient.post(apiAddress + simulateEndpoint(simulationId))

        return handleResponseDefault(response) { response ->
            val response = runCatching { response.body<SimpleSimulationResult>() }
                .getOrElse { return ApiResult.MalformedResponseError("Unexpected server response: $it") }

            ApiResult.Success(response)
        }
    }

    override suspend fun stopSimulation(simulationId: SimulationId): ApiResult<Unit> = callWrapper {
        val response = httpClient.post(apiAddress + stopEndpoint(simulationId))

        return handleResponseDefault(response) { response ->
            ApiResult.Success(Unit)
        }
    }

    override suspend fun suspendSimulation(simulationId: SimulationId): ApiResult<Unit> = callWrapper {
        val response = httpClient.post(apiAddress + suspendEndpoint(simulationId))

        return handleResponseDefault(response) { response ->
            ApiResult.Success(Unit)
        }
    }

    override suspend fun authenticate(
        login: String,
        password: String,
    ): ApiResult<ShortUserInfo> = callWrapper {
        val response = httpClient.post(apiAddress + AUTHENTICATE_ENDPOINT) {
            setBody(LoginRequest(login = login, password = password))
            contentType(ContentType.Application.Json)
        }

        return handleResponseDefault(response) { response ->
            val response = runCatching { response.body<ShortUserInfo>() }
                .getOrElse { return ApiResult.MalformedResponseError("Unexpected server response: $it") }

            ApiResult.Success(response)
        }
    }

    override suspend fun register(
        login: String,
        password: String,
    ): ApiResult<ShortUserInfo> = callWrapper {
        val response = httpClient.post(apiAddress + REGISTER_ENDPOINT) {
            setBody(RegistrationRequest(login = login, password = password))
            contentType(ContentType.Application.Json)
        }

        return handleResponseDefault(response) { response ->
            val response = runCatching { response.body<ShortUserInfo>() }
                .getOrElse { return ApiResult.MalformedResponseError("Unexpected server response: $it") }

            ApiResult.Success(response)
        }
    }

    override suspend fun updatePassword(
        oldPassword: String,
        newPassword: String,
    ): ApiResult<Unit> = callWrapper {
        val response = httpClient.post(apiAddress + UPDATE_PASSWORD_ENDPOINT) {
            setBody(PasswordUpdateRequest(oldPassword = oldPassword, newPassword = newPassword))
            contentType(ContentType.Application.Json)
        }

        return handleResponseDefault(response) { response ->
            ApiResult.Success(Unit)
        }
    }

    override suspend fun logout(): ApiResult<Unit> = callWrapper {
        val response = httpClient.post(apiAddress + LOGOUT_ENDPOINT)

        return handleResponseDefault(response) { response ->
            ApiResult.Success(Unit)
        }
    }

    override suspend fun shortUserInfo(): ApiResult<ShortUserInfo> = callWrapper {
        val response = httpClient.get(apiAddress + SHORT_INFO_ENDPOINT)

        return handleResponseDefault(response) { response ->
            val response = runCatching { response.body<ShortUserInfo>() }
                .getOrElse { return ApiResult.MalformedResponseError("Unexpected server response: $it") }

            ApiResult.Success(response)
        }
    }

    override suspend fun listHistory(
        beforeEntryId: HistoryEntryId?,
        limit: Int?,
        withResults: Boolean,
    ): ApiResult<HistoryEntryList> = callWrapper {
        val response = httpClient.get(apiAddress + LIST_HISTORY_ENDPOINT) {
            parameters {
                if (beforeEntryId != null) {
                    this["beforeId"] = beforeEntryId.toString()
                }
                if (limit != null) {
                    this["limit"] = limit.toString()
                }
                if (withResults) {
                    this["withResults"] = "true"
                }
            }
        }

        return handleResponseDefault(response) { response ->
            val response = runCatching { response.body<HistoryEntryList>() }
                .getOrElse { return ApiResult.MalformedResponseError("Unexpected server response: $it") }

            ApiResult.Success(response)
        }
    }

    override suspend fun getHistoryEntry(entryId: HistoryEntryId): ApiResult<HistoryEntry> = callWrapper {
        val response = httpClient.get(apiAddress + getHistoryEntryEndpoint(entryId))

        return handleResponseDefault(response) { response ->
            val response = runCatching { response.body<HistoryEntry>() }
                .getOrElse { return ApiResult.MalformedResponseError("Unexpected server response: $it") }

            ApiResult.Success(response)
        }
    }

    private suspend inline fun <R> handleResponseDefault(
        response: HttpResponse,
        onOk: (response: HttpResponse) -> ApiResult<R>,
    ): ApiResult<R> {
        var response = response
        while (true) {
            return when (response.status) {
                HttpStatusCode.OK -> {
                    if (response.headers.contains(Headers.CONTINUATION)) {
                        val cont = response.headers[Headers.CONTINUATION]!!
                        while (true) {
                            delay(3000)
                            val response2 = httpClient.post(apiAddress + CONTINUATION_ENDPOINT) {
                                headers.append(Headers.CONTINUATION, cont)
                            }
                            if (response2.headers[Headers.CONTINUATION] != Headers.CONTINUATION_KEEP) {
                                response = response2
                                break
                            }
                        }
                        continue
                    } else {
                        onOk(response)
                    }
                }

                HttpStatusCode.BadRequest -> ApiResult.ClientError(response.bodyAsText())

                HttpStatusCode.NotFound -> ApiResult.NotFound(response.bodyAsText())

                HttpStatusCode.InternalServerError -> ApiResult.ServerError(response.bodyAsText())

                HttpStatusCode.Unauthorized -> ApiResult.ClientError("Illegal login or password")

                else -> ApiResult.MalformedResponseError("Unexpected server response code ${response.status}")
            }
        }
    }

    private inline fun <R> callWrapper(call: () -> ApiResult<R>): ApiResult<R> = runCatching {
        call()
    }.getOrElse {
        ApiResult.ClientError(it.toString())
    }

    companion object {
        private const val LOCAL_API_ADDRESS = "http://localhost:8090"

        private const val API_V0_PREFIX = "/api/v0"
        private const val SIMULATE_V0_ENDPOINT = "$API_V0_PREFIX/simulate"

        private const val API_V1_PREFIX = "/api/v1"
        private const val CREATE_SIMULATION_ENDPOINT = "$API_V1_PREFIX/create_simulation"
        private const val CREATE_SIMULATION_WITH_CONFIG_ENDPOINT = "$API_V1_PREFIX/create_with_config"

        private const val API_V1_USERS_PREFIX = "$API_V1_PREFIX/users"
        private const val AUTHENTICATE_ENDPOINT = "$API_V1_USERS_PREFIX/authenticate"
        private const val REGISTER_ENDPOINT = "$API_V1_USERS_PREFIX/register"
        private const val UPDATE_PASSWORD_ENDPOINT = "$API_V1_USERS_PREFIX/updatePassword"
        private const val LOGOUT_ENDPOINT = "$API_V1_USERS_PREFIX/logout"
        private const val SHORT_INFO_ENDPOINT = "$API_V1_USERS_PREFIX/shortInfo"

        private const val API_V1_HISTORY_PREFIX = "$API_V1_PREFIX/history"
        private const val LIST_HISTORY_ENDPOINT = "$API_V1_HISTORY_PREFIX/list"

        private const val CONTINUATION_ENDPOINT = "$API_V1_PREFIX/continuation"

        private fun restoreFromHistoryEntryEndpoint(id: HistoryEntryId) = "$API_V1_PREFIX/restore/$id"
        private fun addHostEndpoint(simulationId: SimulationId) = "$API_V1_PREFIX/$simulationId/add_host"
        private fun addSwitchEndpoint(simulationId: SimulationId) = "$API_V1_PREFIX/$simulationId/add_switch"
        private fun addLinkEndpoint(simulationId: SimulationId) = "$API_V1_PREFIX/$simulationId/add_link"
        private fun addConnectionEndpoint(simulationId: SimulationId) = "$API_V1_PREFIX/$simulationId/add_connection"
        private fun stateEndpoint(simulationId: SimulationId) = "$API_V1_PREFIX/$simulationId/state"
        private fun removeObjectEndpoint(simulationId: SimulationId) = "$API_V1_PREFIX/$simulationId/remove_object"
        private fun simulateEndpoint(simulationId: SimulationId) = "$API_V1_PREFIX/$simulationId/simulate"
        private fun stopEndpoint(simulationId: SimulationId) = "$API_V1_PREFIX/$simulationId/stop"
        private fun suspendEndpoint(simulationId: SimulationId) = "$API_V1_PREFIX/$simulationId/suspend"

        private fun getHistoryEntryEndpoint(id: HistoryEntryId) = "$API_V1_HISTORY_PREFIX/entry/$id"
    }
}
