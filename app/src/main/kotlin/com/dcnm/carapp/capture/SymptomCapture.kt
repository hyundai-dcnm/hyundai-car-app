package com.dcnm.carapp.capture

import com.dcnm.carapp.core.data.repository.SymptomRepository
import com.dcnm.carapp.core.model.SymptomEvent
import com.dcnm.carapp.core.model.SymptomTrigger
import com.dcnm.carapp.core.navi.NaviRepository
import com.dcnm.carapp.core.navi.WaypointResult
import com.dcnm.carapp.core.vehicle.VehicleRepository
import com.dcnm.carapp.core.voice.VoiceManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * 증상 기록 루프. 앱 화면이 아니라 앱 생명주기에 붙어 있어서, 운전자가 순정 내비 화면을 보고 있어도 동작한다.
 *
 * 1) 음성 호출([recordFromVoice]) · 주행 화면 버튼([recordFromTap]) · 트리거(급제동·조향·경고등)
 * 2) STT 한마디 → 발화 + 링버퍼 스냅샷을 SymptomEvent로 저장 → [recorded]로 알림 (알림음 + 상단 알림은 서비스가 띄운다)
 * 3) 경고등 트리거면 "정비소에 들를까요?" 예/아니오 → addWaypoint
 *
 * [state]는 앱이 앞에 있을 때 주행 화면이 상태 한 줄로 보여준다.
 */
class SymptomCapture(
    private val vehicle: VehicleRepository,
    private val voice: VoiceManager,
    private val symptoms: SymptomRepository,
    private val navi: NaviRepository,
    private val scope: CoroutineScope,
) {
    sealed interface State {
        data object Idle : State
        data object Listening : State
        data class Recorded(val utterance: String?) : State
        data class AskingWaypoint(val shopName: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _recorded = MutableSharedFlow<SymptomEvent>(extraBufferCapacity = 4)
    /** 저장이 끝난 기록. 알림음·상단 알림의 근거. */
    val recorded: SharedFlow<SymptomEvent> = _recorded.asSharedFlow()

    // 한 번에 하나만 녹음한다. 녹음 중에 들어온 호출·트리거는 버린다.
    private val busy = Mutex()
    private var triggerJob: Job? = null
    private var manualAnswer: CompletableDeferred<Boolean>? = null

    /** 차량 트리거 감시 시작. 여러 번 불러도 한 번만 붙는다. */
    fun start() {
        if (triggerJob?.isActive == true) return
        triggerJob = scope.launch { vehicle.triggers.collect { onTrigger(it) } }
    }

    fun recordFromTap() = launchExclusive { record(SymptomTrigger.USER_TAP, presetUtterance = null) }

    /**
     * 음성 호출로 들어온 기록. [presetUtterance]가 있으면(호출 문장에 증상이 이미 담겼거나 시연 주입) STT를 다시 열지 않는다.
     */
    fun recordFromVoice(presetUtterance: String?) = launchExclusive { record(SymptomTrigger.USER_VOICE, presetUtterance) }

    /** 경유 질문에 대형 버튼으로 답한 경우. 음성 답보다 먼저 오면 이쪽을 쓴다. */
    fun answerWaypoint(yes: Boolean) {
        manualAnswer?.complete(yes)
    }

    private fun launchExclusive(block: suspend () -> Unit) = scope.launch {
        if (!busy.tryLock()) return@launch
        try { block() } finally { busy.unlock() }
    }

    private suspend fun onTrigger(trigger: SymptomTrigger) {
        if (busy.isLocked) return
        busy.withLock {
            when (trigger) {
                SymptomTrigger.TELLTALE_ON -> offerWaypoint()
                else -> if (voice.askYesNo("방금 이상한 느낌 있으셨어요?") == true) record(trigger, presetUtterance = null)
            }
        }
    }

    private suspend fun record(trigger: SymptomTrigger, presetUtterance: String?) {
        val anchor = System.currentTimeMillis()
        _state.value = State.Listening
        val utterance = presetUtterance ?: voice.listenOnce()
        // 직후 5초 샘플을 더 담기 위해 지연 후 잘라낸다.
        delay(AFTER_MILLIS)
        val event = SymptomEvent(
            id = UUID.randomUUID().toString(),
            createdAtMillis = anchor,
            trigger = trigger,
            utterance = utterance,
            snapshot = vehicle.snapshot(anchor),
        )
        symptoms.record(event)
        _recorded.emit(event)
        _state.value = State.Recorded(utterance)
        scope.launch {
            delay(RECORDED_HOLD_MILLIS)
            if (_state.value is State.Recorded) _state.value = State.Idle
        }
    }

    private suspend fun offerWaypoint() {
        val candidate = navi.nearbyShops(limit = 1).firstOrNull() ?: return
        _state.value = State.AskingWaypoint(candidate.shop.name)
        val manual = CompletableDeferred<Boolean>().also { manualAnswer = it }
        val spoken = scope.async { voice.askYesNo("${candidate.shop.name}에 들를까요?") }
        val yes = select {
            manual.onAwait { it }
            spoken.onAwait { it }
        }
        spoken.cancel()
        manualAnswer = null
        if (yes == true) {
            when (val r = navi.addWaypoint(candidate.shop)) {
                is WaypointResult.Added -> voice.speak("${r.shop.name}을 경유지에 추가했어요.")
                is WaypointResult.Failed -> voice.speak("경유지 추가에 실패했어요. 주차 후 다시 시도해 주세요.")
            }
        }
        _state.value = State.Idle
    }

    private companion object {
        const val AFTER_MILLIS = 5_000L
        const val RECORDED_HOLD_MILLIS = 3_000L
    }
}
