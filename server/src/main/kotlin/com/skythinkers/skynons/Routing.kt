package com.skythinkers.skynons

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

import io.ktor.client.request.forms.*
import kotlin.io.path.*

import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Runtime

fun Application.configureRouting() {
    val nonsPath = System.getenv("NONS_PATH")?.takeUnless(String::isEmpty) ?: "../backend/build/nons"
    routing {
        route("/api/v0") {
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
                    val process = Runtime.getRuntime().exec(
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

                    val baseNames = listOf(
                        "cwnd.svg",
                        "packet_reordering.svg",
                        "rate.svg",
                        "rtt.svg"
                    )

                    val data = MultiPartFormDataContent(formData {
                        baseNames.forEach { basename ->
                            append(
                                basename,
                                Path(tempOutputDirectory.pathString, basename).readBytes(),
                                Headers.build {
                                    append(HttpHeaders.ContentType, "image/svg")
                                    append(HttpHeaders.ContentDisposition, "filename=\"$basename\"")
                                })
                        }
                    })

                    call.respond(HttpStatusCode.OK, data)

                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.toString())
                }

            }
        }
    }
}
