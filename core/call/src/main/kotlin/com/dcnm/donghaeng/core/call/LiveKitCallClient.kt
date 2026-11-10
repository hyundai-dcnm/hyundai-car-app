package com.dcnm.donghaeng.core.call

import android.content.Context
import com.dcnm.donghaeng.core.model.CallCredentials
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LiveKitCallClient(
    private val context: Context,
    private val scope: CoroutineScope,
) : CallClient {

    private val _state = MutableStateFlow<CallState>(CallState.Idle)
    override val state: StateFlow<CallState> = _state

    private var room: Room? = null
    private var eventsJob: Job? = null

    override suspend fun connect(credentials: CallCredentials) {
        disconnect()
        _state.value = CallState.Connecting
        val room = LiveKit.create(context.applicationContext).also { room = it }
        eventsJob = scope.launch {
            room.events.collect { event ->
                when (event) {
                    is RoomEvent.Reconnecting -> _state.value = CallState.Reconnecting
                    is RoomEvent.Disconnected -> _state.value = CallState.Idle
                    is RoomEvent.FailedToConnect -> _state.value = CallState.Failed(event.error.message ?: "연결 실패")
                    is RoomEvent.Reconnected,
                    is RoomEvent.ParticipantConnected,
                    is RoomEvent.ParticipantDisconnected,
                    is RoomEvent.ActiveSpeakersChanged,
                    is RoomEvent.TrackMuted,
                    is RoomEvent.TrackUnmuted -> publish(room)
                    else -> Unit
                }
            }
        }
        try {
            room.connect(credentials.url, credentials.token)
            room.localParticipant.setMicrophoneEnabled(true)
            publish(room)
        } catch (e: Exception) {
            _state.value = CallState.Failed(e.message ?: "연결 실패")
        }
    }

    override suspend fun setMicEnabled(enabled: Boolean) {
        val room = room ?: return
        room.localParticipant.setMicrophoneEnabled(enabled)
        publish(room)
    }

    override suspend fun disconnect() {
        eventsJob?.cancel()
        eventsJob = null
        room?.disconnect()
        room?.release()
        room = null
        _state.value = CallState.Idle
    }

    private fun publish(room: Room) {
        val speaking = room.activeSpeakers.mapNotNull { it.identity?.value }.toSet()
        _state.value = CallState.Connected(
            micEnabled = room.localParticipant.isMicrophoneEnabled,
            participants = room.remoteParticipants.values.map {
                val id = it.identity?.value.orEmpty()
                CallParticipant(id, it.name ?: id, id in speaking)
            },
        )
    }
}
