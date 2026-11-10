package com.dcnm.donghaeng.core.call

import com.dcnm.donghaeng.core.model.CallCredentials
import kotlinx.coroutines.flow.StateFlow

/** 여행방 음성 통화. 방 하나에 통화 하나다. */
interface CallClient {
    val state: StateFlow<CallState>

    suspend fun connect(credentials: CallCredentials)
    suspend fun setMicEnabled(enabled: Boolean)
    suspend fun disconnect()
}

sealed interface CallState {
    data object Idle : CallState
    data object Connecting : CallState
    data class Connected(
        val micEnabled: Boolean,
        val participants: List<CallParticipant>,
    ) : CallState
    /** 네트워크가 잠깐 끊겼다. LiveKit이 스스로 다시 붙는 중이다. */
    data object Reconnecting : CallState
    data class Failed(val reason: String) : CallState
}

/** identity 는 서버가 토큰에 넣은 멤버 id 와 같다. */
data class CallParticipant(
    val identity: String,
    val name: String,
    val speaking: Boolean,
)
