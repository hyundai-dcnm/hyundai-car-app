package com.dcnm.carapp.ui.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcnm.carapp.core.data.repository.MaintenanceRepository
import com.dcnm.carapp.core.model.MaintenanceRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 정비 결과 입력(점검만/교체/조치 안 함)과 교체 이력, 다음 점검. 정차 중에만 열린다. */
class MaintenanceViewModel(private val repo: MaintenanceRepository) : ViewModel() {
    val records: StateFlow<List<MaintenanceRecord>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(record: MaintenanceRecord) = viewModelScope.launch { repo.record(record) }

    // TODO(W3): nextDue(rule) 표시. 규칙 출처(sourceNote) 없는 기본값을 넣지 않는다.
}
