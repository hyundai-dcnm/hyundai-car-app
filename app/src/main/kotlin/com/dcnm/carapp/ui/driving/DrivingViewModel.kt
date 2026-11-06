package com.dcnm.carapp.ui.driving

import androidx.lifecycle.ViewModel
import com.dcnm.carapp.capture.SymptomCapture
import kotlinx.coroutines.flow.StateFlow

/**
 * 주행 중 화면용 얇은 껍데기. 기록 루프 자체는 [SymptomCapture]가 앱 생명주기 동안 돌린다
 * (앱이 뒤로 가 있어도 음성 기록·알림이 동작해야 하므로).
 */
class DrivingViewModel(private val capture: SymptomCapture) : ViewModel() {
    val state: StateFlow<SymptomCapture.State> = capture.state

    fun onRecordTapped() = capture.recordFromTap()

    /** 경유 질문에 대형 버튼으로 답한 경우. 음성 답보다 먼저 오면 이쪽을 쓴다. */
    fun onWaypointAnswer(yes: Boolean) = capture.answerWaypoint(yes)
}
