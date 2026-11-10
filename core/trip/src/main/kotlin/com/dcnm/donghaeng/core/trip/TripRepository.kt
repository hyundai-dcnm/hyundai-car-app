package com.dcnm.donghaeng.core.trip

import com.dcnm.donghaeng.core.model.CallCredentials
import com.dcnm.donghaeng.core.model.Destination
import com.dcnm.donghaeng.core.model.Trip
import kotlinx.coroutines.flow.StateFlow

/**
 * 앱이 여행방과 주고받는 것. 화면은 이 인터페이스만 안다.
 * 서버 연동 구현은 ④와 API 를 정한 뒤 추가한다. 그 전까지는 FakeTripRepository.
 */
interface TripRepository {
    /** 참여 중인 방. 방이 없으면 null. */
    val trip: StateFlow<Trip?>
    /** 이 차량의 멤버 id. 방에 들어가기 전에는 null. */
    val myMemberId: StateFlow<String?>
    val connection: StateFlow<ConnectionStatus>

    suspend fun createTrip(name: String, profile: Profile)
    suspend fun joinTrip(inviteCode: String, profile: Profile)
    suspend fun leaveTrip()

    suspend fun setLocationSharing(sharing: Boolean)

    suspend fun proposeDestination(destination: Destination)
    suspend fun acceptDestination(proposalId: String)

    suspend fun callCredentials(): CallCredentials
}

data class Profile(val nickname: String, val carLabel: String)

enum class ConnectionStatus { Disconnected, Connecting, Connected, Reconnecting }
