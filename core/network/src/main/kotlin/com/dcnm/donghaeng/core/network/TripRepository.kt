package com.dcnm.donghaeng.core.network

import com.dcnm.donghaeng.core.model.CallCredentials
import com.dcnm.donghaeng.core.model.Destination
import com.dcnm.donghaeng.core.model.LatLng
import com.dcnm.donghaeng.core.model.TripState
import kotlinx.coroutines.flow.StateFlow

/** 앱이 여행방과 주고받는 모든 것. 화면은 이 인터페이스만 안다. */
interface TripRepository {
    /** 참여 중인 방. 방이 없으면 null. */
    val state: StateFlow<TripState?>
    /** 이 차량의 멤버 id. 방에 들어가기 전에는 null. */
    val myMemberId: StateFlow<String?>
    val connection: StateFlow<ConnectionStatus>

    suspend fun createTrip(name: String, profile: Profile)
    suspend fun joinTrip(inviteCode: String, profile: Profile)
    suspend fun leaveTrip()

    suspend fun setLocationSharing(sharing: Boolean)
    /** 위치 공유가 꺼져 있으면 보내지 않는다. */
    suspend fun sendLocation(location: LatLng, headingDeg: Float?, speedKph: Float?)

    suspend fun proposeDestination(destination: Destination)
    suspend fun acceptDestination(proposalId: String)

    suspend fun callCredentials(): CallCredentials
}

data class Profile(val nickname: String, val carLabel: String)

enum class ConnectionStatus { Disconnected, Connecting, Connected, Reconnecting }
