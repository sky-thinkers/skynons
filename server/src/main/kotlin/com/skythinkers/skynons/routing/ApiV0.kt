package com.skythinkers.skynons.routing

import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.appendText
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.pathString
import kotlin.io.path.readBytes
import kotlin.io.path.writeText

fun Route.apiV0(nonsPath: String) {
    post("/simulate") {
        try {
            val text = call.receiveText()
            val tempSimulationConfig = createTempFile("config", ".yml")

            tempSimulationConfig.writeText("$text\n")

            // needed for parsing on back
            tempSimulationConfig.appendText(
                    "topology_config_path: ${tempSimulationConfig.absolutePathString()}\n"
            )

            val tempOutputDirectory = createTempDirectory("output")
            val process =
                    Runtime.getRuntime()
                            .exec(
                                    arrayOf(
                                            nonsPath,
                                            "-c",
                                            tempSimulationConfig.absolutePathString(),
                                            "--output-dir",
                                            tempOutputDirectory.absolutePathString()
                                    )
                            )
            val result = process.waitFor()
            if (result != 0) {
                val stderrReader = BufferedReader(InputStreamReader(process.errorStream))
                val stderrLines = stderrReader.readLines()
                call.respond(
                        HttpStatusCode.BadRequest,
                        "Backend error:\n ${stderrLines.joinToString(separator = "\n")}"
                )
            }

            val baseNames = listOf("cwnd.svg", "reordering.svg", "rate.svg", "rtt.svg")

            val data =
                    MultiPartFormDataContent(
                            formData {
                                baseNames.forEach { basename ->
                                    append(
                                            basename,
                                            Path(tempOutputDirectory.pathString, basename)
                                                    .readBytes(),
                                            Headers.build {
                                                append(HttpHeaders.ContentType, "image/svg")
                                                append(
                                                        HttpHeaders.ContentDisposition,
                                                        "filename=\"${basename.takeUnless { it == "reordering.svg" } ?: "packet_reordering.svg"}\""
                                                )
                                            }
                                    )
                                }
                            }
                    )

            call.respond(HttpStatusCode.OK, data)
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, e.toString())
        }
    }
}
