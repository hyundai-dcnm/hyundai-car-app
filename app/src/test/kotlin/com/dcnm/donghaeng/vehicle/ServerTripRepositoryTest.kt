package com.dcnm.donghaeng.vehicle

import com.dcnm.donghaeng.core.trip.Profile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.*
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class ServerTripRepositoryTest {
    private val tripJson = """{"id":"demo-trip","name":"UI test","inviteCode":"ABC234","hostId":"me","members":[{"id":"me","nickname":"test","carLabel":"test car","sharingLocation":false}],"destination":null,"proposal":null}"""
    @Test fun `join disables server sharing before exposing room and sends bearer token`() = runBlocking {
        val server = MockWebServer()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            server.enqueue(MockResponse().setBody("""{"trip":$tripJson,"memberId":"me","accessToken":"test-only-token"}"""))
            server.enqueue(MockResponse().setResponseCode(204))
            server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) { webSocket.send("""{"type":"snapshot","trip":$tripJson,"locations":[]}""") }
            }))
            val repo = ServerTripRepository(scope, server.url("/").toString().trimEnd('/'))
            repo.createTrip("UI test", Profile("test", "test car"))
            assertNotNull(repo.trip.value)
            assertFalse(repo.consent.value)
            val create = server.takeRequest(3, TimeUnit.SECONDS)!!
            assertEquals("/api/trips", create.path)
            val privacy = server.takeRequest(3, TimeUnit.SECONDS)!!
            assertEquals("Bearer test-only-token", privacy.getHeader("Authorization"))
            assertEquals("{\"sharing\":false}", privacy.body.readUtf8())
            assertFalse(repo.trip.value!!.members.single().sharingLocation)
            val ws = server.takeRequest(3, TimeUnit.SECONDS)!!
            assertTrue(ws.path!!.startsWith("/ws/trips/demo-trip?token="))
        } finally { scope.cancel(); server.shutdown() }
    }
    @Test fun `failed stop keeps local consent off and server message reaches caller`() = runBlocking {
        val server = MockWebServer()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            server.enqueue(MockResponse().setBody("""{"trip":$tripJson,"memberId":"me","accessToken":"test-only-token"}"""))
            server.enqueue(MockResponse().setResponseCode(204))
            server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {}))
            server.enqueue(MockResponse().setResponseCode(503).setBody("""{"message":"공유 상태를 저장하지 못했어요"}"""))
            val repo = ServerTripRepository(scope, server.url("/").toString().trimEnd('/'))
            repo.createTrip("UI test", Profile("test", "test car"))
            repeat(3) { assertNotNull(server.takeRequest(3, TimeUnit.SECONDS)) }
            repo.consent.value = true
            val failure = runCatching { repo.setLocationSharing(false) }.exceptionOrNull()
            assertEquals("공유 상태를 저장하지 못했어요", failure?.message)
            assertFalse(repo.consent.value)
            assertNotNull(repo.trip.value)
        } finally { scope.cancel(); server.shutdown() }
    }
    @Test fun `manual reconnect does not let old job cancel the new socket`() = runBlocking {
        val server = MockWebServer()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            server.enqueue(MockResponse().setBody("""{"trip":$tripJson,"memberId":"me","accessToken":"test-only-token"}"""))
            server.enqueue(MockResponse().setResponseCode(204))
            fun snapshot(name: String) = MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send("""{"type":"snapshot","trip":${tripJson.replace("UI test", name)},"locations":[]}""")
                }
            })
            server.enqueue(snapshot("first"))
            server.enqueue(snapshot("second"))
            val repo = ServerTripRepository(scope, server.url("/").toString().trimEnd('/'))
            repo.createTrip("UI test", Profile("test", "test car"))
            withTimeout(5_000) { repo.trip.first { it?.name == "first" } }
            repo.retryConnection()
            withTimeout(5_000) { repo.trip.first { it?.name == "second" } }
            delay(100)
            assertEquals(com.dcnm.donghaeng.core.trip.ConnectionStatus.Connected, repo.connection.value)
        } finally { scope.cancel(); server.shutdown() }
    }
}
