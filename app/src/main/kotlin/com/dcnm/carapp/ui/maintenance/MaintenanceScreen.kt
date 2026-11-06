package com.dcnm.carapp.ui.maintenance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dcnm.carapp.core.model.WorkType
import com.dcnm.carapp.di.AppContainer
import com.dcnm.carapp.ui.components.ActionButton
import com.dcnm.carapp.ui.components.CarCard
import com.dcnm.carapp.ui.components.ChoiceButton
import com.dcnm.carapp.ui.components.ScreenHeader
import com.dcnm.carapp.ui.components.SectionLabel
import com.dcnm.carapp.ui.format.km
import com.dcnm.carapp.ui.theme.CarDimens
import com.dcnm.carapp.ui.theme.CarTheme
import com.dcnm.carapp.ui.maintenance.MaintenanceViewModel.Form

/**
 * 정비 결과 기록. 정차 중이므로 숫자 키보드를 허용하되, 나머지는 프리셋 선택으로 받는다.
 * 좌: ① 작업 종류 ② 항목 ③ 작업일·당시 주행거리 / 우: ④ 다음 기준(출처 필수) → 미리보기 → 저장.
 */
@Composable
fun MaintenanceScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: MaintenanceViewModel = viewModel { MaintenanceViewModel(container.maintenanceRepository) }
    val form by vm.form.collectAsState()
    val c = CarTheme.colors

    Column(Modifier.fillMaxSize().background(c.background).padding(horizontal = CarDimens.Margin, vertical = 24.dp)) {
        ScreenHeader("정비 결과 기록", onBack = onBack)
        BoxWithConstraints(Modifier.fillMaxSize().padding(top = CarDimens.Spacing)) {
            val wide = maxWidth >= CarDimens.TwoColumnMinWidth
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(CarDimens.Gutter)) {
                    WorkColumn(form, vm::update, Modifier.weight(1f))
                    RuleColumn(form, vm, onSaved = onBack, modifier = Modifier.weight(1f))
                }
            } else {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(CarDimens.Spacing)) {
                    WorkColumn(form, vm::update, Modifier.fillMaxWidth())
                    RuleColumn(form, vm, onSaved = onBack, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkColumn(form: Form, update: (Form.() -> Form) -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CarDimens.Spacing)) {
        SectionLabel("① 작업 종류")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(WorkType.INSPECTION to "점검만", WorkType.REPLACEMENT to "부품 교체", WorkType.NONE to "조치 안 함").forEach { (type, label) ->
                ChoiceButton(label, form.workType == type, { update { copy(workType = type) } }, Modifier.weight(1f), height = CarDimens.ButtonL)
            }
        }
        SectionLabel("② 항목")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), maxItemsInEachRow = 3) {
            MaintenanceViewModel.ITEMS.forEach { item ->
                ChoiceButton(item, form.item == item, { update { copy(item = item) } }, Modifier.weight(1f))
            }
        }
        SectionLabel("③ 작업일과 주행거리")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            MaintenanceViewModel.DateChoice.entries.forEach { d ->
                ChoiceButton(d.label, form.date == d, { update { copy(date = d) } }, Modifier.weight(1f))
            }
            OdometerField(form.odometerText, { text -> update { copy(odometerText = text.filter(Char::isDigit).take(7)) } }, Modifier.weight(1.6f))
        }
    }
}

@Composable
private fun OdometerField(value: String, onChange: (String) -> Unit, modifier: Modifier) {
    val c = CarTheme.colors
    val shape = RoundedCornerShape(CarDimens.RadiusM)
    Box(
        modifier.heightIn(min = CarDimens.SecondaryTouch).background(c.surface, shape).border(CarDimens.ControlBorder, c.controlBorder, shape).padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) Text("주행거리 km", style = CarTheme.type.body, color = c.inkTertiary, maxLines = 1)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = CarTheme.type.body.copy(color = c.ink),
            cursorBrush = SolidColor(c.ink),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RuleColumn(form: Form, vm: MaintenanceViewModel, onSaved: () -> Unit, modifier: Modifier) {
    val c = CarTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CarDimens.Spacing)) {
        SectionLabel("④ 다음 점검 기준 (먼저 도달하는 쪽으로 알림)")
        Stepper("거리", if (form.intervalKm == 0) "없음" else "+${km(form.intervalKm.toDouble())}",
            onMinus = { vm.update { copy(intervalKm = (intervalKm - MaintenanceViewModel.KM_STEP).coerceAtLeast(0)) } },
            onPlus = { vm.update { copy(intervalKm = intervalKm + MaintenanceViewModel.KM_STEP) } })
        Stepper("기간", if (form.intervalMonths == 0) "없음" else "+${form.intervalMonths}개월",
            onMinus = { vm.update { copy(intervalMonths = (intervalMonths - 1).coerceAtLeast(0)) } },
            onPlus = { vm.update { copy(intervalMonths = intervalMonths + 1) } })
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("출처", style = CarTheme.type.body, color = c.ink, modifier = Modifier.width(100.dp))
            MaintenanceViewModel.RuleSource.entries.forEach { s ->
                ChoiceButton(s.label, form.ruleSource == s, { vm.update { copy(ruleSource = s) } }, Modifier.weight(1f))
            }
        }
        CarCard(Modifier.fillMaxWidth()) {
            SectionLabel("저장될 내용")
            Text(previewLine(form), style = CarTheme.type.bodyStrong, color = c.ink)
            Text(rulePreview(form), style = CarTheme.type.body, color = c.inkSecondary)
        }
        form.blockReason?.let { Text(it, style = CarTheme.type.body, color = c.errorText) }
        ActionButton("저장", { vm.save(onSaved) }, Modifier.fillMaxWidth(), enabled = form.blockReason == null)
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = CarTheme.type.body, color = CarTheme.colors.ink, modifier = Modifier.width(100.dp))
        ActionButton("−", onMinus, Modifier.width(CarDimens.ButtonL), filled = false, height = CarDimens.ButtonM)
        Text(value, style = CarTheme.type.bodyStrong, color = CarTheme.colors.ink, modifier = Modifier.weight(1f))
        ActionButton("+", onPlus, Modifier.width(CarDimens.ButtonL), filled = false, height = CarDimens.ButtonM)
    }
}

private fun previewLine(f: Form): String {
    val type = when (f.workType) {
        WorkType.INSPECTION -> "점검"; WorkType.REPLACEMENT -> "교체"; WorkType.NONE -> "조치 안 함"; else -> "작업 종류 미선택"
    }
    return listOfNotNull(f.item ?: "항목 미선택", type, f.date.label, f.odometerKm?.let(::km)).joinToString(", ")
}

private fun rulePreview(f: Form): String = when {
    !f.hasRule -> "다음 기준 없음, 나중에 추가 가능"
    f.ruleSource == MaintenanceViewModel.RuleSource.UNKNOWN -> "다음 기준 출처 확인 필요"
    else -> buildList {
        if (f.intervalKm > 0) add(if (f.odometerKm != null) "${km(f.odometerKm!! + f.intervalKm)}" else "+${km(f.intervalKm.toDouble())} (당시 거리 없음)")
        if (f.intervalMonths > 0) add("${f.intervalMonths}개월 뒤")
    }.joinToString(" 또는 ") + (f.ruleSource?.let { ", 출처 ${it.label}" } ?: "")
}
