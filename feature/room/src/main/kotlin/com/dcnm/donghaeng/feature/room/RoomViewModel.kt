package com.dcnm.donghaeng.feature.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcnm.donghaeng.core.network.Profile
import com.dcnm.donghaeng.core.network.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RoomUiState(
    val nickname: String = "",
    val carLabel: String = "",
    val tripName: String = "",
    val inviteCode: String = "",
    val busy: Boolean = false,
    val error: String? = null,
) {
    val profileReady get() = nickname.isNotBlank() && carLabel.isNotBlank()
}

class RoomViewModel(private val trips: TripRepository) : ViewModel() {

    private val _ui = MutableStateFlow(RoomUiState())
    val ui: StateFlow<RoomUiState> = _ui

    fun onNickname(value: String) = _ui.update { it.copy(nickname = value) }
    fun onCarLabel(value: String) = _ui.update { it.copy(carLabel = value) }
    fun onTripName(value: String) = _ui.update { it.copy(tripName = value) }
    fun onInviteCode(value: String) = _ui.update { it.copy(inviteCode = value.uppercase()) }

    fun create() = run { trips.createTrip(_ui.value.tripName.ifBlank { "같이 가요" }, profile()) }

    fun join() = run { trips.joinTrip(_ui.value.inviteCode, profile()) }

    private fun profile() = Profile(_ui.value.nickname.trim(), _ui.value.carLabel.trim())

    private fun run(block: suspend () -> Unit) {
        if (_ui.value.busy) return
        _ui.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { block() }
            _ui.update { it.copy(busy = false, error = result.exceptionOrNull()?.message) }
        }
    }
}
