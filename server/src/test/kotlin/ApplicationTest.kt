package com.skythinkers.skynons

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.io.path.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }

        val config_path = Path("src/test/kotlin/config.yml")

        println(config_path.readText())

        val responce = client.post("/api/v0/simulate") {
            contentType(ContentType.Application.Yaml)
            setBody(config_path.readText())
        }

        assertEquals(responce.status, HttpStatusCode.OK)
    }

}
