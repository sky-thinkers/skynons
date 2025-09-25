package com.skythinkers.skynons

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }

        val configPath = Path("src/test/resources/config.yml")

        val response = client.post("/api/v0/simulate") {
            contentType(ContentType.Application.Yaml)
            setBody(configPath.readText())
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
