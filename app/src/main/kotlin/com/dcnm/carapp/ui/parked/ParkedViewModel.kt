package com.dcnm.carapp.ui.parked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcnm.carapp.core.data.repository.SymptomRepository
import com.dcnm.carapp.core.model.SymptomEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** 정차 중: 사건 목록, 리포트 카드, QR. 원본 신호는 차 안에만 두고 QR에는 요약 링크만 담는다. */
class ParkedViewModel(symptoms: SymptomRepository) : ViewModel() {
    val events: StateFlow<List<SymptomEvent>> =
        symptoms.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // TODO(W3): fun reportUrl(event): String — 서버 업로드 후 24h 서명 URL, 오프라인이면 요약 JSON QR(≤2KB)
}
