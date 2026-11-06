package com.dcnm.carapp.ui.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcnm.carapp.core.data.repository.MaintenanceRepository
import com.dcnm.carapp.core.model.MaintenanceRecord
import com.dcnm.carapp.core.model.MaintenanceRule
import com.dcnm.carapp.core.model.WorkType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 정비 결과 입력(점검/교체/조치 안 함)과 다음 점검 기준. 정차 중에만 열린다.
 * 입력 중 주행이 시작돼도 ViewModel이 살아 있으므로 정차 복귀 시 입력값이 그대로 남는다.
 */
class MaintenanceViewModel(private val repo: MaintenanceRepository) : ViewModel() {

    enum class DateChoice(val label: String, val daysAgo: Int) { TODAY("오늘", 0), YESTERDAY("어제", 1), WEEK_AGO("1주 전", 7) }

    /** 다음 기준 출처. 출처 없는 기본값을 넣지 않는다. "모름"은 '확인 필요'로 저장. */
    enum class RuleSource(val label: String) { MANUAL("설명서"), SHOP("정비사 지정"), UNKNOWN("모름") }

    data class Form(
        val workType: WorkType? = null,
        val item: String? = null,
        val date: DateChoice = DateChoice.TODAY,
        val odometerText: String = "",
        val intervalKm: Int = 0,
        val intervalMonths: Int = 0,
        val ruleSource: RuleSource? = null,
    ) {
        val odometerKm: Double? get() = odometerText.toDoubleOrNull()
        val hasRule: Boolean get() = intervalKm > 0 || intervalMonths > 0

        /** 저장할 수 없는 이유. null이면 저장 가능. */
        val blockReason: String?
            get() = when {
                workType == null -> "작업 종류 선택 필요"
                item == null -> "항목 선택 필요"
                odometerText.isNotEmpty() && odometerKm == null -> "주행거리는 숫자만 입력"
                hasRule && ruleSource == null -> "다음 기준 출처 선택 필요"
                else -> null
            }
    }

    private val _form = MutableStateFlow(Form())
    val form: StateFlow<Form> = _form.asStateFlow()

    fun update(block: Form.() -> Form) = _form.update(block)

    fun save(onSaved: () -> Unit) {
        val f = _form.value
        if (f.blockReason != null) return
        val workDate = System.currentTimeMillis() - f.date.daysAgo * 86_400_000L
        viewModelScope.launch {
            repo.record(
                MaintenanceRecord(
                    id = UUID.randomUUID().toString(),
                    symptomEventId = null,
                    workType = f.workType!!,
                    item = f.item!!,
                    workDateMillis = workDate,
                    odometerKmAtWork = f.odometerKm,
                ),
            )
            // TODO(④ 스키마 11/10): MaintenanceRule 저장 테이블이 생기면 rule()을 함께 저장한다.
            onSaved()
        }
    }

    fun rule(f: Form = _form.value): MaintenanceRule? = if (!f.hasRule || f.item == null || f.ruleSource == null) null else MaintenanceRule(
        item = f.item,
        intervalKm = f.intervalKm.takeIf { it > 0 }?.toDouble(),
        intervalDays = f.intervalMonths.takeIf { it > 0 }?.let { it * 30 },
        sourceNote = if (f.ruleSource == RuleSource.UNKNOWN) "확인 필요" else f.ruleSource.label,
    )

    companion object {
        val ITEMS = listOf("엔진오일", "오일필터", "에어컨 필터", "브레이크 패드", "타이어", "와이퍼", "배터리", "냉각수", "기타")
        const val KM_STEP = 1_000
    }
}
