package com.dcnm.donghaeng.core.network

import com.dcnm.donghaeng.core.model.CallCredentials
import com.dcnm.donghaeng.core.model.Destination
import com.dcnm.donghaeng.core.model.LatLng
import com.dcnm.donghaeng.core.model.MemberLocation
import com.dcnm.donghaeng.core.model.TripEvent
import com.dcnm.donghaeng.core.model.TripState
import com.dcnm.donghaeng.core.model.reduce
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RemoteTripRepository(
    baseUrl: String,
    private val scope: CoroutineScope,
    debugLogging: Boolean = false,
) : TripRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        encodeDefaults = true
    }

    private var accessToken: String? = null

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val token = accessToken
            val request = if (token == null) chain.request()
            else chain.request().newBuilder().header("Authorization", "Bearer $token").build()
            chain.proceed(request)
        }
        .apply { if (debugLogging) addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)) }
        .build()

    private val baseUrl = baseUrl.trimEnd('/') + "/"

    private val api = Retrofit.Builder()
        .baseUrl(this.baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(TripApi::class.java)

    private val socket = TripSocket(client, json, scope)

    private val _state = MutableStateFlow<TripState?>(null)
    override val state: StateFlow<TripState?> = _state

    private val _myMemberId = MutableStateFlow<String?>(null)
    override val myMemberId: StateFlow<String?> = _myMemberId

    override val connection: StateFlow<ConnectionStatus> = socket.status

    init {
        scope.launch {
            socket.events.collect { event -> _state.update { it?.reduce(event) } }
        }
    }

    override suspend fun createTrip(name: String, profile: Profile) =
        enter(api.create(CreateTripRequest(name, profile.nickname, profile.carLabel)))

    override suspend fun joinTrip(inviteCode: String, profile: Profile) =
        enter(api.join(JoinTripRequest(inviteCode.trim().uppercase(), profile.nickname, profile.carLabel)))

    private fun enter(joined: JoinedTrip) {
        accessToken = joined.accessToken
        _myMemberId.value = joined.memberId
        _state.value = TripState(joined.trip)
        val wsUrl = baseUrl.replaceFirst("http", "ws") + "ws/trips/${joined.trip.id}?token=${joined.accessToken}"
        socket.open(wsUrl)
    }

    override suspend fun leaveTrip() {
        val tripId = tripId() ?: return
        runCatching { api.leave(tripId) }
        socket.close()
        _state.value = null
        _myMemberId.value = null
        accessToken = null
    }

    override suspend fun setLocationSharing(sharing: Boolean) {
        api.setSharing(requireTripId(), SharingRequest(sharing))
    }

    override suspend fun sendLocation(location: LatLng, headingDeg: Float?, speedKph: Float?) {
        val me = _myMemberId.value ?: return
        val sharing = _state.value?.trip?.members?.firstOrNull { it.id == me }?.sharingLocation ?: false
        if (!sharing) return
        socket.send(
            TripEvent.LocationUpdated(MemberLocation(me, location, headingDeg, speedKph, System.currentTimeMillis())),
        )
    }

    override suspend fun proposeDestination(destination: Destination) {
        api.propose(requireTripId(), ProposeRequest(destination))
    }

    override suspend fun acceptDestination(proposalId: String) {
        api.accept(requireTripId(), proposalId)
    }

    override suspend fun callCredentials(): CallCredentials = api.callToken(requireTripId())

    private fun tripId() = _state.value?.trip?.id
    private fun requireTripId() = checkNotNull(tripId()) { "여행방에 들어가 있지 않다" }
}
