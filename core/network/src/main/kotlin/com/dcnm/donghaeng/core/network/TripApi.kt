package com.dcnm.donghaeng.core.network

import com.dcnm.donghaeng.core.model.CallCredentials
import com.dcnm.donghaeng.core.model.Destination
import com.dcnm.donghaeng.core.model.DestinationProposal
import com.dcnm.donghaeng.core.model.Trip
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** REST 엔드포인트. 형식은 contract/api.md 와 같아야 한다. */
internal interface TripApi {
    @POST("api/trips")
    suspend fun create(@Body body: CreateTripRequest): JoinedTrip

    @POST("api/trips/join")
    suspend fun join(@Body body: JoinTripRequest): JoinedTrip

    @DELETE("api/trips/{tripId}/members/me")
    suspend fun leave(@Path("tripId") tripId: String)

    @PUT("api/trips/{tripId}/members/me/sharing")
    suspend fun setSharing(@Path("tripId") tripId: String, @Body body: SharingRequest)

    @POST("api/trips/{tripId}/proposals")
    suspend fun propose(@Path("tripId") tripId: String, @Body body: ProposeRequest): DestinationProposal

    @POST("api/trips/{tripId}/proposals/{proposalId}/accept")
    suspend fun accept(@Path("tripId") tripId: String, @Path("proposalId") proposalId: String)

    @POST("api/trips/{tripId}/call-token")
    suspend fun callToken(@Path("tripId") tripId: String): CallCredentials
}

@Serializable
internal data class CreateTripRequest(val name: String, val nickname: String, val carLabel: String)

@Serializable
internal data class JoinTripRequest(val inviteCode: String, val nickname: String, val carLabel: String)

@Serializable
internal data class JoinedTrip(val trip: Trip, val memberId: String, val accessToken: String)

@Serializable
internal data class SharingRequest(val sharing: Boolean)

@Serializable
internal data class ProposeRequest(val destination: Destination)
