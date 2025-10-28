package com.skythinkers.skynons

import com.skythinkers.skynons.api.Connection
import com.skythinkers.skynons.api.CreateSimulationResponseData
import com.skythinkers.skynons.api.ErrorResponseData
import com.skythinkers.skynons.api.Host
import com.skythinkers.skynons.api.Link
import com.skythinkers.skynons.api.RemoveObject
import com.skythinkers.skynons.api.RemovedObjectList
import com.skythinkers.skynons.api.SimpleSimulationResult
import com.skythinkers.skynons.api.SimulationState
import com.skythinkers.skynons.api.Switch
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for API V1.
 * Only tests server ability to bypass correct request/response message types between client and backend.
 * Does not test or use backend in any other way.
 */
class ApiV1Test {

    @Test
    fun createSimulation() = test {
        val response = client.post("/api/v1/create_simulation")

        assertEquals(HttpStatusCode.OK, response.status)

        response.body<CreateSimulationResponseData>() // should not fail
    }

    @Test
    fun addHost() = test {
        val simResponse = client.post("/api/v1/create_simulation")
        val id = simResponse.body<CreateSimulationResponseData>().id

        val response = client.post("/api/v1/$id/add_host") {
            setBody(Host("host"))
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun addSwitch() = test {
        val simResponse = client.post("/api/v1/create_simulation")
        val id = simResponse.body<CreateSimulationResponseData>().id

        val response = client.post("/api/v1/$id/add_switch") {
            setBody(Switch("switch"))
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun addLink() = test {
        val simResponse = client.post("/api/v1/create_simulation")
        val id = simResponse.body<CreateSimulationResponseData>().id

        assertEquals(HttpStatusCode.OK, client.post("/api/v1/$id/add_host") {
            setBody(Host("h1"))
            contentType(ContentType.Application.Json)
        }.status)
        assertEquals(HttpStatusCode.OK, client.post("/api/v1/$id/add_host") {
            setBody(Host("h2"))
            contentType(ContentType.Application.Json)
        }.status)

        val response = client.post("/api/v1/$id/add_link") {
            setBody(Link("link", "h1", "h2", "100Mbps"))
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun addConnection() = test {
        val simResponse = client.post("/api/v1/create_simulation")
        val id = simResponse.body<CreateSimulationResponseData>().id

        assertEquals(HttpStatusCode.OK, client.post("/api/v1/$id/add_host") {
            setBody(Host("h1"))
            contentType(ContentType.Application.Json)
        }.status)
        assertEquals(HttpStatusCode.OK, client.post("/api/v1/$id/add_host") {
            setBody(Host("h2"))
            contentType(ContentType.Application.Json)
        }.status)
        assertEquals(HttpStatusCode.OK, client.post("/api/v1/$id/add_link") {
            setBody(Link("link", "h1", "h2", "100Mbps"))
            contentType(ContentType.Application.Json)
        }.status)

        val response = client.post("/api/v1/$id/add_connection") {
            setBody(Connection("connection", "h1", "h2", "1GB"))
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun state() = test {
        val simResponse = client.post("/api/v1/create_simulation")
        val id = simResponse.body<CreateSimulationResponseData>().id

        assertEquals(HttpStatusCode.OK, client.post("/api/v1/$id/add_host") {
            setBody(Host("h1"))
            contentType(ContentType.Application.Json)
        }.status)
        assertEquals(HttpStatusCode.OK, client.post("/api/v1/$id/add_host") {
            setBody(Host("h2"))
            contentType(ContentType.Application.Json)
        }.status)
        assertEquals(HttpStatusCode.OK, client.post("/api/v1/$id/add_link") {
            setBody(Link("link", "h1", "h2", "100Mbps"))
            contentType(ContentType.Application.Json)
        }.status)
        assertEquals(HttpStatusCode.OK, client.post("/api/v1/$id/add_connection") {
            setBody(Connection("connection", "h1", "h2", "1GB"))
            contentType(ContentType.Application.Json)
        }.status)

        val response = client.get("/api/v1/$id/state")
        assertEquals(HttpStatusCode.OK, response.status)

        val state = response.body<SimulationState>()

        assertEquals(setOf(Host("h1"), Host("h2")), state.hosts.toSet())
        assertEquals(setOf(Link("link", "h1", "h2", "100Mbps")), state.links.toSet())
        assertEquals(setOf(Connection("connection", "h1", "h2", "1GB")), state.connections.toSet())
    }

    @Test
    fun simulate() = test {
        val simResponse = client.post("/api/v1/create_simulation")
        val id = simResponse.body<CreateSimulationResponseData>().id

        assertEquals(HttpStatusCode.OK, client.post("/api/v1/$id/add_host") {
            setBody(Host("h1"))
            contentType(ContentType.Application.Json)
        }.status)
        assertEquals(HttpStatusCode.OK, client.post("/api/v1/$id/add_host") {
            setBody(Host("h2"))
            contentType(ContentType.Application.Json)
        }.status)
        assertEquals(HttpStatusCode.OK, client.post("/api/v1/$id/add_link") {
            setBody(Link("link", "h1", "h2", "100Mbps"))
            contentType(ContentType.Application.Json)
        }.status)
        assertEquals(HttpStatusCode.OK, client.post("/api/v1/$id/add_connection") {
            setBody(Connection("connection", "h1", "h2", "1GB"))
            contentType(ContentType.Application.Json)
        }.status)

        val response = client.post("/api/v1/$id/simulate")
        assertEquals(HttpStatusCode.OK, response.status)
        response.body<SimpleSimulationResult>() // should not fail
    }

    @Test
    fun duplicateIds() = test {
        val simResponse = client.post("/api/v1/create_simulation")
        val id = simResponse.body<CreateSimulationResponseData>().id

        assertEquals(HttpStatusCode.OK, client.post("/api/v1/$id/add_host") {
            setBody(Host("h1"))
            contentType(ContentType.Application.Json)
        }.status)
        val response = client.post("/api/v1/$id/add_host") {
            setBody(Host("h1"))
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        response.body<ErrorResponseData>() // should not fail
    }

    @Test
    fun duplicateIdsForDifferentObjectKind() = test {
        val simResponse = client.post("/api/v1/create_simulation")
        val id = simResponse.body<CreateSimulationResponseData>().id

        assertEquals(HttpStatusCode.OK, client.post("/api/v1/$id/add_host") {
            setBody(Host("h1"))
            contentType(ContentType.Application.Json)
        }.status)

        val response = client.post("/api/v1/$id/add_switch") {
            setBody(Switch("h1"))
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        response.body<ErrorResponseData>() // should not fail
    }

    @Test
    fun removeObject() = test {
        val simResponse = client.post("/api/v1/create_simulation")
        val id = simResponse.body<CreateSimulationResponseData>().id

        assertEquals(HttpStatusCode.OK, client.post("/api/v1/$id/add_host") {
            setBody(Host("h1"))
            contentType(ContentType.Application.Json)
        }.status)

        val response = client.delete("/api/v1/$id/remove_object") {
            setBody(RemoveObject("h1"))
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(RemovedObjectList(listOf("h1")), response.body<RemovedObjectList>())
    }

    private inline fun test(crossinline action: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        application {
            module(ProcessManagerMock())
        }
        client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }
        action()
    }
}
