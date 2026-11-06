package com.dcnm.carapp.ui.parked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcnm.carapp.core.data.repository.MaintenanceRepository
import com.dcnm.carapp.core.data.repository.SymptomRepository
import com.dcnm.carapp.core.model.MaintenanceRecord
import com.dcnm.carapp.core.model.SymptomEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 정차 홈: 최근 사건, 최근 정비, 수집 상태. 원본 신호는 차 안에만 두고 QR에는 요약만 담는다. */
class ParkedViewModel(
    private val symptoms: SymptomRepository,
    maintenance: MaintenanceRepository,
) : ViewModel() {
    val events: StateFlow<List<SymptomEvent>> = symptoms.observeAll()
        .map { list -> list.sortedByDescending { it.createdAtMillis } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val latestMaintenance: StateFlow<MaintenanceRecord?> = maintenance.observeAll()
        .map { list -> list.maxByOrNull { it.workDateMillis } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 디버그 빌드 전용 (화면에서 BuildConfig.DEBUG로 막는다). */
    fun addDemoEvent() = viewModelScope.launch { symptoms.record(demoSymptomEvent()) }
}
