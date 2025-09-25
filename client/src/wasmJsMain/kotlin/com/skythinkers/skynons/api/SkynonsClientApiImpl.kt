package com.skythinkers.skynons.api

import com.skythinkers.skynons.data.SvgData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.*
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.contentType
import kotlinx.io.readString

class SkynonsClientApiImpl(
    private val apiAddress: String = LOCAL_API_ADDRESS,
    private val httpClient: HttpClient = HttpClient(Js),
) : SkynonsClientApi {

    override suspend fun simulate(config: String): ApiResult<SimpleSimulationResult> {
        //val response = httpClient.post(apiAddress + SIMULATE_ENDPOINT) {
        val response = httpClient.post(apiAddress) {
            setBody(config)
            contentType(ContentType.Application.Yaml)
        }

        return ApiResult.ClientError(response.status.toString())

//        return when (response.status) {
//            HttpStatusCode.OK -> {
//                var cvnd: SvgData? = null
//                var packetReordering: SvgData? = null
//                var rate: SvgData? = null
//                var rtt: SvgData? = null
//                val data = response.body<MultiPartData>()
//                var part = data.readPart()
//                while (part != null) {
//                    if (part !is PartData.BinaryItem) {
//                        part.dispose()
//                        return ApiResult.MalformedResponseError("Expected multipart data of file parts, but got non-file part ${part.name}")
//                    }
//                    when (part.name) {
//                        "cvnd.svg" -> cvnd = SvgData(part.provider().readString())
//                        "packet_reordering.svg" -> packetReordering = SvgData(part.provider().readString())
//                        "rate.svg" -> rate = SvgData(part.provider().readString())
//                        "rtt.svg" -> rtt = SvgData(part.provider().readString())
//                    }
//                    part.dispose()
//                    part = data.readPart()
//                }
//                ApiResult.Success(
//                    SimpleSimulationResult(
//                        cvnd ?: return ApiResult.MalformedResponseError("Missing cvnd.svg in response"),
//                        packetReordering
//                            ?: return ApiResult.MalformedResponseError("Missing packet_reordering.svg in response"),
//                        rate ?: return ApiResult.MalformedResponseError("Missing rate.svg in response"),
//                        rtt ?: return ApiResult.MalformedResponseError("Missing rtt.svg in response"),
//                    )
//                )
//            }
//
//            HttpStatusCode.BadRequest -> ApiResult.ClientError(response.bodyAsText())
//
//            HttpStatusCode.InternalServerError -> ApiResult.ServerError(response.bodyAsText())
//
//            else -> ApiResult.MalformedResponseError("Unexpected server response code ${response.status}")
//        }
    }

    companion object {
        private const val LOCAL_API_ADDRESS = "http://localhost:8090"
        private const val API_PREFIX = "/api/v0"
        private const val SIMULATE_ENDPOINT = "$API_PREFIX/simulate"
    }
}
