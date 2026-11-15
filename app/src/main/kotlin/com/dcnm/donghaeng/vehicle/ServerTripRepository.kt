package com.dcnm.donghaeng.vehicle

import com.dcnm.donghaeng.core.model.*
import com.dcnm.donghaeng.core.trip.*
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class RemoteLocation(val memberId: String, val location: LatLng, val headingDeg: Double?, val speedKph: Double?, val updatedAtMillis: Long)
data class TripPhoto(val id: String, val uploaderName: String, val contentType: String, val sizeBytes: Long, val createdAtMillis: Long, val fileUrl: String)

/** Frontend adapter for the unchanged contract/api.md. Tokens stay in process memory. */
class ServerTripRepository(
    private val scope: CoroutineScope,
    private val baseUrl: String = "https://donghaeng-server.onrender.com",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(65, TimeUnit.SECONDS)
        .callTimeout(70, TimeUnit.SECONDS).retryOnConnectionFailure(false).build(),
) : TripRepository {
    override val trip = MutableStateFlow<Trip?>(null)
    override val myMemberId = MutableStateFlow<String?>(null)
    override val connection = MutableStateFlow(ConnectionStatus.Disconnected)
    val locations = MutableStateFlow<List<RemoteLocation>>(emptyList())
    val error = MutableStateFlow<String?>(null)
    val health = MutableStateFlow("서버 확인 중")
    val consent = MutableStateFlow(false)
    private val gson = Gson()
    private val mutex = Mutex()
    private data class Session(val tripId: String, val memberId: String, val token: String)
    private var session: Session? = null
    private var socket: WebSocket? = null
    private var socketJob: Job? = null
    private var socketGeneration = 0
    private var lastLocationSent = 0L

    suspend fun checkHealth() {
        health.value = "서버 연결 중 · 최대 2회 확인"
        repeat(2) { attempt ->
            try {
                val json = JsonParser.parseString(request("GET", "/actuator/health")).asJsonObject
                health.value = if (json["status"]?.asString == "UP") "서버 응답 정상" else "서버 준비 중"
                return
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (attempt == 0) delay(2_000) }
        }
        health.value = "서버에 연결할 수 없어요"
    }

    override suspend fun createTrip(name: String, profile: Profile) = enter("/api/trips", mapOf("name" to name.trim(), "nickname" to profile.nickname, "carLabel" to profile.carLabel))
    override suspend fun joinTrip(inviteCode: String, profile: Profile) = enter("/api/trips/join", mapOf("inviteCode" to inviteCode.trim().uppercase(), "nickname" to profile.nickname, "carLabel" to profile.carLabel))

    private suspend fun enter(path: String, body: Any) = mutex.withLock {
        check(session == null) { "이미 참여 중인 동행이 있어요" }
        connection.value = ConnectionStatus.Connecting
        try {
            val json = JsonParser.parseString(request("POST", path, body)).asJsonObject
            val joined = gson.fromJson(json["trip"], Trip::class.java)
            val s = Session(joined.id, json["memberId"].asString, json["accessToken"].asString)
            session = s
            consent.value = false
            // Server defaults sharing to true. Explicitly turn it off before presenting the room.
            try { request("PUT", "/api/trips/${s.tripId}/members/me/sharing", mapOf("sharing" to false), s.token) }
            catch (e: Exception) {
                withContext(NonCancellable) { runCatching { request("DELETE", "/api/trips/${s.tripId}/members/me", token = s.token) } }
                session = null
                throw e
            }
            myMemberId.value = s.memberId
            trip.value = joined.copy(members = joined.members.map { if (it.id == s.memberId) it.copy(sharingLocation = false) else it })
            error.value = null
            startSocket(s)
        } catch (e: Exception) { connection.value = ConnectionStatus.Disconnected; throw e }
    }

    override suspend fun leaveTrip() = mutex.withLock {
        val s = session ?: return@withLock
        consent.value = false
        locations.value = emptyList()
        // Keep room and credentials on failure so leaving can be retried.
        request("DELETE", "/api/trips/${s.tripId}/members/me", token = s.token)
        clearSession()
    }

    override suspend fun setLocationSharing(sharing: Boolean) = mutex.withLock {
        val s = requireSession()
        if (!sharing) consent.value = false // Stop outgoing coordinates even if REST is offline.
        request("PUT", "/api/trips/${s.tripId}/members/me/sharing", mapOf("sharing" to sharing), s.token)
        consent.value = sharing
        refresh(s)
    }

    override suspend fun proposeDestination(destination: Destination) = mutex.withLock {
        val s = requireSession()
        request("POST", "/api/trips/${s.tripId}/proposals", mapOf("destination" to destination), s.token)
        refresh(s)
    }
    override suspend fun acceptDestination(proposalId: String) = mutex.withLock {
        val s = requireSession()
        request("POST", "/api/trips/${s.tripId}/proposals/$proposalId/accept", token = s.token)
        refresh(s)
    }
    override suspend fun callCredentials(): CallCredentials {
        val s = requireSession()
        return gson.fromJson(request("POST", "/api/trips/${s.tripId}/call-token", token = s.token), CallCredentials::class.java)
            .also { require(it.url.startsWith("wss://")) { "보안 통화 주소가 필요해요" } }
    }

    fun sendLocation(location: LatLng, headingDeg: Double?, measuredAt: Long, now: Long = System.currentTimeMillis()) {
        val s = session ?: return
        if (!consent.value || connection.value != ConnectionStatus.Connected || now - lastLocationSent < 2_000 || now - measuredAt !in 0..120_000) return
        if (trip.value?.members?.any { it.id == s.memberId && it.sharingLocation } != true) return
        if (socket?.send(gson.toJson(mapOf("type" to "location", "location" to mapOf("location" to location, "headingDeg" to headingDeg)))) == true) lastLocationSent = now
    }

    fun retryConnection() { session?.let(::startSocket) }

    fun pauseLocationSending() { consent.value = false }

    suspend fun listPhotos(): List<TripPhoto> {
        val s = requireSession()
        val photos = gson.fromJson(request("GET", "/api/trips/${s.tripId}/photos", token = s.token), Array<TripPhoto>::class.java).toList()
        check(session == s) { "동행이 변경되었어요" }
        return photos
    }
    suspend fun photoBytes(id: String): ByteArray {
        val s = requireSession()
        val req = Request.Builder().url("$baseUrl/api/trips/${s.tripId}/photos/$id/file").header("Authorization", "Bearer ${s.token}").build()
        return execute(req).also { check(session == s) { "동행이 변경되었어요" } }
    }
    suspend fun uploadPhoto(bytes: ByteArray): TripPhoto {
        require(bytes.isNotEmpty() && bytes.size <= 10 * 1024 * 1024) { "사진은 10MB까지 올릴 수 있어요" }
        val s = requireSession()
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", "trip-photo.jpg", bytes.toRequestBody("image/jpeg".toMediaType())).build()
        val req = Request.Builder().url("$baseUrl/api/trips/${s.tripId}/photos").header("Authorization", "Bearer ${s.token}").post(body).build()
        val photo = gson.fromJson(execute(req).toString(Charsets.UTF_8), TripPhoto::class.java)
        check(session == s) { "동행이 변경되었어요" }
        return photo
    }
    suspend fun deletePhoto(id: String) {
        val s = requireSession()
        request("DELETE", "/api/trips/${s.tripId}/photos/$id", token = s.token)
    }

    private fun startSocket(s: Session) {
        val generation = ++socketGeneration
        socketJob?.cancel()
        socket?.cancel()
        socketJob = scope.launch {
            for (attempt in 0..5) {
                if (session != s) return@launch
                connection.value = if (attempt == 0) ConnectionStatus.Connecting else ConnectionStatus.Reconnecting
                if (attempt > 0) delay(1_000L shl (attempt - 1))
                val closed = CompletableDeferred<Int>()
                val url = baseUrl.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") + "/ws/trips/${s.tripId}?token=${s.token}"
                val currentSocket = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        if (session != s || generation != socketGeneration) return
                        runCatching {
                            val event = JsonParser.parseString(text).asJsonObject
                            when (event["type"].asString) {
                                "snapshot" -> {
                                    trip.value = gson.fromJson(event["trip"], Trip::class.java)
                                    locations.value = gson.fromJson(event["locations"], Array<RemoteLocation>::class.java).toList()
                                    connection.value = ConnectionStatus.Connected
                                    error.value = null
                                }
                                "location" -> {
                                    val loc = gson.fromJson(event["location"], RemoteLocation::class.java)
                                    locations.value = locations.value.filterNot { it.memberId == loc.memberId } + loc
                                }
                                "trip_closed" -> { clearSession(); error.value = "동행이 종료되었어요" }
                                else -> scope.launch { mutex.withLock { if (session == s) runCatching { refresh(s) }.onFailure { error.value = it.message } } }
                            }
                        }.onFailure { error.value = "동행 정보를 읽지 못했어요" }
                    }
                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { closed.complete(code) }
                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) { webSocket.close(code, null); closed.complete(code) }
                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { closed.complete(response?.code ?: -1) }
                })
                socket = currentSocket
                try {
                    val code = closed.await()
                    if (session != s) return@launch
                    connection.value = ConnectionStatus.Reconnecting
                    locations.value = emptyList()
                    if (code in listOf(1000, 1008, 401, 403)) {
                        clearSession(); error.value = "동행 참여가 만료되었어요. 초대 코드로 다시 참여해 주세요"; return@launch
                    }
                } finally { currentSocket.cancel() }
            }
            connection.value = ConnectionStatus.Disconnected
            error.value = "연결이 끊겼어요. 다시 연결을 눌러 주세요"
        }
    }

    private suspend fun refresh(s: Session) {
        val updated = gson.fromJson(request("GET", "/api/trips/${s.tripId}", token = s.token), Trip::class.java)
        if (session == s) { trip.value = updated; locations.value = locations.value.filter { l -> updated.members.any { it.id == l.memberId && it.sharingLocation } } }
    }
    private fun requireSession() = checkNotNull(session) { "동행에 먼저 참여해 주세요" }
    private fun clearSession() {
        session = null; consent.value = false; socketJob?.cancel(); socket?.cancel(); socket = null
        trip.value = null; myMemberId.value = null; locations.value = emptyList(); connection.value = ConnectionStatus.Disconnected
    }
    private suspend fun request(method: String, path: String, body: Any? = null, token: String? = null): String {
        val payload = if (method in listOf("GET", "DELETE")) null else gson.toJson(body ?: emptyMap<String, String>()).toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(baseUrl + path).method(method, payload).apply { if (token != null) header("Authorization", "Bearer $token") }.build()
        return execute(req).toString(Charsets.UTF_8)
    }
    private suspend fun execute(req: Request): ByteArray = suspendCancellableCoroutine { cont ->
        val call = client.newCall(req)
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { if (cont.isActive) cont.resumeWith(Result.failure(IOException("서버에 연결할 수 없어요. 잠시 후 다시 시도해 주세요"))) }
            override fun onResponse(call: Call, response: Response) {
                val result = runCatching {
                    response.use {
                        val bytes = it.body?.bytes() ?: byteArrayOf()
                        if (it.isSuccessful) bytes else {
                        val text = bytes.toString(Charsets.UTF_8)
                        val message = runCatching { JsonParser.parseString(text).asJsonObject["message"].asString }.getOrNull()
                        throw IOException(message ?: "서버 요청에 실패했어요 (${it.code})")
                        }
                    }
                }
                if (cont.isActive) cont.resumeWith(result)
            }
        })
    }
}
