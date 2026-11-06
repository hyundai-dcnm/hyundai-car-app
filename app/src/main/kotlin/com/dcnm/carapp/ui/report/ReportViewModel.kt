package com.dcnm.carapp.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcnm.carapp.BuildConfig
import com.dcnm.carapp.core.data.repository.MaintenanceRepository
import com.dcnm.carapp.core.data.repository.SymptomRepository
import com.dcnm.carapp.core.model.MaintenanceRecord
import com.dcnm.carapp.core.model.SymptomEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** 분석 대시보드와 정비사 QR이 같이 쓴다. 정차 중에만 열린다. */
class ReportViewModel(
    eventId: String,
    symptoms: SymptomRepository,
    maintenance: MaintenanceRepository,
) : ViewModel() {

    data class UiState(
        val event: SymptomEvent?,
        val sameTriggerCount: Int,
        val latestMaintenance: MaintenanceRecord?,
    )

    val state: StateFlow<UiState> = combine(symptoms.observeAll(), maintenance.observeAll()) { events, records ->
        val event = events.firstOrNull { it.id == eventId }
        UiState(
            event = event,
            sameTriggerCount = event?.let { e -> events.count { it.id != e.id && it.trigger == e.trigger } } ?: 0,
            latestMaintenance = records.maxByOrNull { it.workDateMillis },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState(null, 0, null))

    /** 정비사 휴대폰용 웹 리포트 주소. 요약은 URL 조각에만 담겨 서버에 저장되지 않는다. */
    fun mechanicUrl(state: UiState): String? = state.event?.let { e ->
        MechanicLink.url(BuildConfig.REPORT_WEB_URL, MechanicLink.payload(e, state.sameTriggerCount, state.latestMaintenance))
    }
}
