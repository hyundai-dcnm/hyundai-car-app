package com.dcnm.donghaeng.core.model

/*
 * 화면이 쓰는 여행방 모델. 서버 응답 형식은 ④와 정한 뒤 core/trip 에서 이 모양으로 바꿔 넣는다.
 */

data class LatLng(val lat: Double, val lng: Double)

data class Destination(val name: String, val location: LatLng)

data class Member(
    val id: String,
    val nickname: String,
    /** 화면에 보이는 차량 구분 이름. 예: "민지네 차" */
    val carLabel: String,
    val sharingLocation: Boolean = true,
)

/** 여행방. 같은 목적지로 가는 차량들의 묶음이다. */
data class Trip(
    val id: String,
    val name: String,
    val inviteCode: String,
    val hostId: String,
    val members: List<Member>,
    /** 모든 차량이 수락해 확정된 목적지. 확정 전에는 null. */
    val destination: Destination? = null,
    /** 아직 수락을 기다리는 목적지 제안. */
    val proposal: DestinationProposal? = null,
)

data class DestinationProposal(
    val id: String,
    val proposerId: String,
    val destination: Destination,
    val acceptedBy: Set<String> = emptySet(),
)

/** LiveKit 접속 정보. 토큰은 여행방 하나에만 유효하다. */
data class CallCredentials(val url: String, val token: String)
