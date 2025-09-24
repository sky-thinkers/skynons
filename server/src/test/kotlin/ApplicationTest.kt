package com.skythinkers.skynons

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }

        val configUrl = javaClass.getResource("/config.yml")
            ?: throw AssertionError("Test config not found")

        val response = client.post("/api/v0/simulate") {
            contentType(ContentType.Application.Yaml)
            setBody(configUrl.readText())
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
