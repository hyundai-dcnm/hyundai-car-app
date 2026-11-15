package com.dcnm.donghaeng.vehicle

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcnm.donghaeng.core.model.Destination
import com.dcnm.donghaeng.core.model.LatLng

@Composable
fun ColumnScope.DestinationContent(vm: VehicleViewModel, vehicleDestination: Destination?, allowed: Boolean) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val trip by vm.repository.trip.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var direct by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf("") }
    var longitude by rememberSaveable { mutableStateOf("") }
    ui.favoriteSlot?.let { AutoLabel("$it 목적지를 등록해요") }
    AutoLabel(if (trip == null) "선택한 장소를 이 기기에 저장해요." else "선택하면 참가자에게 목적지 확인을 요청해요.")
    if (vehicleDestination != null) {
        AutoLabel("현재 차량 내비 목적지")
        AutoButton(vehicleDestination.name, true, allowed && !ui.busy) { vm.chooseDestination(vehicleDestination) }
    } else AutoLabel("차량 내비에 목적지가 설정되면 여기서 가져올 수 있어요.")
    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, enabled = allowed, label = { Text("저장한 목적지 검색") })
    ui.saved.recent.filter { it.name.contains(query, ignoreCase = true) }.forEach { d -> AutoButton(d.name, enabled = allowed && !ui.busy) { vm.chooseDestination(d) } }
    if (ui.saved.recent.isEmpty()) AutoLabel("최근 기록이 없어요. 차량 내비 목적지를 가져오거나 장소 좌표를 등록해 주세요.")
    AutoButton(if (direct) "직접 입력 닫기" else "이름·좌표 직접 등록", enabled = allowed) { direct = !direct }
    if (direct && allowed) {
        OutlinedTextField(name, { name = it.take(80) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("장소 이름") })
        OutlinedTextField(latitude, { latitude = it.take(20) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("위도 (-90 ~ 90)") })
        OutlinedTextField(longitude, { longitude = it.take(20) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("경도 (-180 ~ 180)") })
        val lat = latitude.toDoubleOrNull(); val lng = longitude.toDoubleOrNull()
        AutoButton("이 목적지 선택", true, name.isNotBlank() && lat != null && lat.isFinite() && lat in -90.0..90.0 && lng != null && lng.isFinite() && lng in -180.0..180.0) {
            vm.chooseDestination(Destination(name.trim(), LatLng(lat!!, lng!!)))
        }
    }
    AutoLabel("외부 장소 검색 서비스는 연결 전이에요. 직접 선택한 이름과 좌표만 사용해요.")
}
