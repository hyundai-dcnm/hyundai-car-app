package com.dcnm.carapp.ui.driving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcnm.carapp.core.data.repository.SymptomRepository
import com.dcnm.carapp.core.model.SymptomEvent
import com.dcnm.carapp.core.model.SymptomTrigger
import com.dcnm.carapp.core.navi.NaviRepository
import com.dcnm.carapp.core.navi.WaypointResult
import com.dcnm.carapp.core.vehicle.VehicleRepository
import com.dcnm.carapp.core.voice.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 주행 중 음성 루프.
 *
 * 1) 사용자 1탭 또는 트리거(급제동·조향·경고등) → "방금 이상한 느낌 있으셨어요?"
 * 2) STT 한마디 → 발화 + 링버퍼 스냅샷을 SymptomEvent로 저장 → "기록했어요"
 * 3) 경고등 트리거면 "정비소에 들를까요?" 예/아니오 → addWaypoint
 *
 * 화면에는 상태 한 줄만 보여준다. 실패는 대형 '예' 버튼 폴백으로 넘긴다.
 */
class DrivingViewModel(
    private val vehicle: VehicleRepository,
    private val voice: VoiceManager,
    private val symptoms: SymptomRepository,
    private val navi: NaviRepository,
) : ViewModel() {

    sealed interface UiState {
        data object Idle : UiState
        data object Listening : UiState
        data class Recorded(val utterance: String?) : UiState
        data class AskingWaypoint(val shopName: String) : UiState
        data class Message(val text: String) : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            vehicle.triggers.collect { trigger -> onTrigger(trigger) }
        }
    }

    fun onRecordTapped() = viewModelScope.launch { recordSymptom(SymptomTrigger.USER_TAP) }

    private suspend fun onTrigger(trigger: SymptomTrigger) {
        when (trigger) {
            SymptomTrigger.TELLTALE_ON -> offerWaypoint()
            else -> {
                val yes = voice.askYesNo("방금 이상한 느낌 있으셨어요?")
                if (yes == true) recordSymptom(trigger)
            }
        }
    }

    private suspend fun recordSymptom(trigger: SymptomTrigger) {
        val anchor = System.currentTimeMillis()
        _state.value = UiState.Listening
        val utterance = voice.listenOnce()
        // 직후 5초 샘플을 더 담기 위해 지연 후 잘라낸다.
        kotlinx.coroutines.delay(5_000)
        val event = SymptomEvent(
            id = UUID.randomUUID().toString(),
            createdAtMillis = anchor,
            trigger = trigger,
            utterance = utterance,
            snapshot = vehicle.snapshot(anchor),
        )
        symptoms.record(event)
        voice.speak("기록했어요. 자세한 내용은 주차 후 확인해 주세요.")
        _state.value = UiState.Recorded(utterance)
    }

    private suspend fun offerWaypoint() {
        val candidate = navi.nearbyShops(limit = 1).firstOrNull() ?: return
        _state.value = UiState.AskingWaypoint(candidate.shop.name)
        val yes = voice.askYesNo("${candidate.shop.name}에 들를까요?")
        if (yes == true) {
            when (val r = navi.addWaypoint(candidate.shop)) {
                is WaypointResult.Added -> voice.speak("${r.shop.name}을 경유지에 추가했어요.")
                is WaypointResult.Failed -> voice.speak("경유지 추가에 실패했어요. 주차 후 다시 시도해 주세요.")
            }
        }
        _state.value = UiState.Idle
    }

    /** STT 실패 시 대형 버튼으로 받은 답. */
    fun onManualYes() = viewModelScope.launch { recordSymptom(SymptomTrigger.USER_TAP) }
}
