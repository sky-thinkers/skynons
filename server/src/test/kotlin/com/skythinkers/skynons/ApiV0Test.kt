package com.skythinkers.skynons

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration test for API V0. Requires a compiled backend.
 */
class ApiV0Test {

    @Test
    @Ignore
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
