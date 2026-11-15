package com.dcnm.donghaeng.vehicle

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcnm.donghaeng.core.call.CallState
import com.dcnm.donghaeng.core.model.*
import com.dcnm.donghaeng.core.navi.NaviSource
import com.dcnm.donghaeng.core.trip.ConnectionStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun VehicleApp(vm: VehicleViewModel, ux: UxMode, navi: NaviSource, hasLocation: Boolean,
    hasMic: Boolean, locationStatus: String, onLocation: () -> Unit, onMic: () -> Unit,
    map: @Composable (Modifier) -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val trip by vm.repository.trip.collectAsStateWithLifecycle()
    val connection by vm.repository.connection.collectAsStateWithLifecycle()
    val consent by vm.repository.consent.collectAsStateWithLifecycle()
    val health by vm.repository.health.collectAsStateWithLifecycle()
    val serverError by vm.repository.error.collectAsStateWithLifecycle()
    val remoteLocations by vm.repository.locations.collectAsStateWithLifecycle()
    val call by vm.call.state.collectAsStateWithLifecycle()
    val destination by navi.destination.collectAsStateWithLifecycle()
    val turn by navi.nextTurn.collectAsStateWithLifecycle()
    val guiding by navi.guiding.collectAsStateWithLifecycle()
    val naviError by navi.lastError.collectAsStateWithLifecycle()
    val request by navi.lastRequest.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val setupAllowed = canConfigure(ux)
    val inTrip = if (ui.demo) ui.demoTrip else trip != null
    val sharing = if (ui.demo) ui.demoSharing else consent
    val activeCall = if (ui.demo) ui.demoCall else call is CallState.Connected
    val tripTitle = if (ui.demo) "주말 바다 여행" else trip?.name ?: "함께 가는 길"
    val destName = if (ui.demo && ui.demoDestination) "양양 해변 주차장" else trip?.destination?.name
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { tick = System.currentTimeMillis(); delay(1_000) } }
    val time = LocalDateTime.now()
    val home: () -> Unit = { vm.go(if (inTrip) "S11" else "S01") }
    fun configure(screen: String) { if (setupAllowed) vm.go(screen) else vm.go("E06") }
    val configScreens = setOf("S02", "S03", "S04", "S05", "S06", "S07", "S08", "S09", "S21", "S22", "S23", "S24", "S25", "S26", "S27", "S28", "S30", "SETTINGS", "EXCEPTIONS", "REST", "ALBUM", "E07", "E08", "E09", "E10")
    LaunchedEffect(ux) {
        if (!setupAllowed) { keyboard?.hide(); if (ui.screen in configScreens) vm.go("E06") }
    }
    LaunchedEffect(trip?.id) {
        if (!ui.demo && trip == null && ui.screen in setOf("S07", "S09", "S10", "S11", "S14", "S19")) vm.go("S01")
    }
    BackHandler(ui.screen != "S01") { home() }

    AutomotiveTheme {
        BoxWithConstraints(Modifier.fillMaxSize().background(AutoColors.Ink)) {
            val panelWidth = (maxWidth * .35f).coerceIn(340.dp, 448.dp)
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(96.dp).fillMaxHeight().padding(horizontal = 12.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    FigmaAsset("logo", Modifier.size(48.dp).clickable(onClick = home))
                    Spacer(Modifier.height(26.dp))
                    val entries = listOf(Triple("map", "지도", if (inTrip) "S11" else "S01"), Triple("people", "동행", if (inTrip) "S07" else "S02"),
                        Triple("radar", "주변", if (ui.nearby.enabled) "S22" else "S21"), Triple("phone", "통화", if (activeCall) "S14" else "S13"), Triple("music", "음악", "S16"))
                    entries.forEach { (icon, title, screen) ->
                        val selected = when (icon) { "radar" -> ui.screen in (21..30).map { "S$it" }; "phone" -> ui.screen in listOf("S13", "S14", "S15", "E04"); "music" -> ui.screen in listOf("S16", "S17"); "people" -> ui.screen in listOf("S02", "S03", "S04", "S05", "S06", "S07"); else -> ui.screen in listOf("S01", "S11", "S19") }
                        val available = title !in listOf("통화", "음악") || inTrip
                        Column(Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(18.dp))
                            .background(if (selected) AutoColors.Blue.copy(alpha = .22f) else Color.Transparent)
                            .clickable(enabled = available) { if (icon in listOf("people", "radar")) configure(screen) else vm.go(screen) },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            FigmaAsset(icon, Modifier.size(32.dp))
                            Text(title, color = if (available) AutoColors.Muted else AutoColors.Line, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { configure("SETTINGS") }, Modifier.size(64.dp).semantics { contentDescription = "설정" }) { FigmaAsset("settings", Modifier.size(30.dp)) }
                }
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(time.format(DateTimeFormatter.ofPattern("HH:mm")), fontSize = 23.sp, fontWeight = FontWeight.Medium)
                        Text(time.format(DateTimeFormatter.ofPattern("M월 d일")), color = AutoColors.Muted, fontSize = 16.sp, modifier = Modifier.padding(start = 20.dp))
                        Spacer(Modifier.weight(1f))
                        Text(if (ui.demo) "디자인 시연 · 실제 위치/통화/음악 아님" else if (trip != null) when(connection) {
                            ConnectionStatus.Connected -> "동행 연결됨"; ConnectionStatus.Connecting -> "연결 중"; ConnectionStatus.Reconnecting -> "재연결 중"; else -> "연결 끊김"
                        } else health, color = if (ui.demo) AutoColors.Amber else AutoColors.Muted, fontSize = 15.sp)
                        Text(when(ux) { UxMode.Unknown -> "차량 상태 미확인 · 조작 제한"; UxMode.Restricted -> "주행 조작 제한"; UxMode.Unrestricted -> "설정 가능" }, color = if (setupAllowed) AutoColors.Green else AutoColors.Amber, fontSize = 15.sp, modifier = Modifier.padding(start = 24.dp))
                    }
                    Box(Modifier.fillMaxSize()) {
                        if (ui.demo) DemoMap(ui.screen in listOf("S11", "S12", "S13", "S14", "S15", "S16", "S17", "S18", "S19", "S20", "E01", "E02", "E03", "E04", "E06"), ui.nearby.enabled && ui.screen in setOf("S22", "S23", "S24", "S25", "S26", "S27", "S28", "E07"))
                        else map(Modifier.fillMaxSize())
                        val title: @Composable (String, String, @Composable ColumnScope.() -> Unit) -> Unit = { heading, sub, content ->
                            AutoPanel(heading, sub, Modifier.align(Alignment.TopStart).padding(24.dp).width(panelWidth).heightIn(max = 592.dp), onBack = home, content = content)
                        }
                        if (ui.screen in setOf("S13", "S14", "S15", "S16", "S17", "E04")) {
                            SmallCard(Modifier.align(Alignment.TopStart).padding(24.dp).width(328.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FigmaAsset("arrow", Modifier.size(52.dp))
                                    Column(Modifier.padding(start = 18.dp)) {
                                        Text(if (ui.demo) "350 m" else turn?.let { "${it.distanceM.toInt()} m" } ?: "안내 대기", fontSize = 38.sp, fontWeight = FontWeight.Bold)
                                        AutoLabel(if (ui.demo) "인구중앙길 방면 우회전" else turn?.description ?: "차량 내비 응답 대기")
                                    }
                                }
                            }
                            SmallCard(Modifier.align(Alignment.BottomStart).padding(24.dp).fillMaxWidth(.60f).clickable { vm.go("S12") }) {
                                Text(destName ?: "공동 목적지 미정", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                AutoLabel(if (ui.demo) "18분 · 12.4 km · 시연 경로" else if (guiding) "차량 내비 안내 중" else "차량 내비에서 안내 시작을 확인해 주세요")
                            }
                        }
                        when (ui.screen) {
                            "S01" -> {
                                title("어디로 함께 갈까요?", if (ui.demo) "민지님, 오늘도 반가워요." else "함께할 동행을 준비해 보세요.") {
                                    SettingRow("search", "목적지", if (ui.demo) "양양 해변 주차장" else destName ?: "목적지를 선택해 주세요") { configure("S08") }
                                    AutoLabel(if (ui.demo) "시연 목적지 · 실제 검색/경로 아님" else "동행방에서 목적지를 제안하고 함께 수락해요.")
                                    HorizontalDivider(color = AutoColors.Line, modifier = Modifier.padding(vertical = 8.dp))
                                    Text("함께 가는 즐거움", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    AutoButton("동행 만들기", true) { configure("S05") }
                                    AutoButton("주변 동행 찾기") { configure("S21") }
                                }
                                Column(Modifier.align(Alignment.TopEnd).padding(24.dp).width(300.dp).clip(RoundedCornerShape(22.dp)).background(AutoColors.Panel).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("동행", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                                    AutoButton("친구 · 연결 요청") { configure("S02") }
                                    AutoButton("초대 코드로 참여") { configure("S06") }
                                }
                            }
                            "S05", "S06" -> title(if (ui.screen == "S05") "새로운 동행" else "동행 초대", "위치와 마이크는 따로 선택해요.") {
                                if (setupAllowed) {
                                    AutoField("내 이름", ui.nickname) { vm.edit("nickname", it) }
                                    AutoField("차 이름", ui.carLabel) { vm.edit("car", it) }
                                    if (ui.screen == "S05") AutoField("동행 이름", ui.roomName) { vm.edit("name", it) }
                                    else AutoField("초대 코드 6자리", ui.code) { vm.edit("code", it) }
                                }
                                AutoLabel(if (ui.demo) "시연 방 · 서버에 저장하지 않아요" else "서버에 방 이름과 표시 이름을 보내요. 위치는 동의 전까지 전송하지 않아요.")
                                AutoButton(if (ui.busy) "연결 중…" else if (ui.screen == "S05") "동행 만들기" else "참여하기", true,
                                    enabled = setupAllowed && !ui.busy && ui.nickname.isNotBlank() && ui.carLabel.isNotBlank() && (ui.screen == "S05" || ui.code.length == 6)) { if (ui.screen == "S05") vm.create() else vm.join() }
                                AutoButton("취소", onClick = home)
                            }
                            "S07" -> title(tripTitle, "출발 준비 · ${if (ui.demo) 3 else trip?.members?.size ?: 0}대 참여") {
                                MemberList(ui.demo, trip, emptyList())
                                SettingRow("pin", "공동 목적지", destName ?: "목적지 확인이 필요해요") { configure("S08") }
                                AutoLabel(if (ui.demo) "시연 초대 코드 · DEMO00" else "초대 코드  ${trip?.inviteCode.orEmpty()}")
                                AutoButton(if (trip?.proposal != null) "목적지 제안 확인" else "목적지 제안", true) { configure(if (trip?.proposal != null) "S09" else "S08") }
                                AutoButton("동행 지도", onClick = { vm.go("S11") })
                            }
                            "S08", "S30" -> title(if (ui.screen == "S30") "탐색 목적지" else "목적지 선택", "예시 장소 · 검색 제공자 미연동") {
                                AutoLabel("실시간 장소 검색은 연결 전이에요. 아래 저장된 예시 좌표로 제안할 수 있어요.")
                                VehicleViewModel.destinations.forEachIndexed { index, d ->
                                    AutoButton(d.name, primary = index == 0, enabled = setupAllowed && !ui.busy) {
                                        if (ui.screen == "S30") { vm.nearby { it.destination(if (index == 0) "demo-beach" else "demo-$index") }; vm.go("S21") }
                                        else if (inTrip) vm.propose(d) else vm.message("동행을 만든 뒤 목적지를 제안해 주세요")
                                    }
                                }
                                AutoButton("취소", onClick = home)
                            }
                            "S09" -> title("함께 갈 목적지", trip?.proposal?.destination?.name ?: destName ?: "양양 해변 주차장 · 시연") {
                                Text("같은 곳으로 갈까요?", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                AutoLabel(if (ui.demo) "규원님이 목적지를 제안했어요 · 시연" else "${trip?.proposal?.acceptedBy?.size ?: 0} / ${trip?.members?.size ?: 0}대 수락")
                                AutoButton("이 목적지 수락", true, setupAllowed && !ui.busy && (ui.demo || trip?.proposal != null)) { vm.accept() }
                                AutoButton("다른 곳 제안") { configure("S08") }
                                if (destName != null) AutoButton("공유 설정으로") { vm.go("S10") }
                            }
                            "S10" -> title("이번 동행의 공유", destName ?: "목적지 합의를 기다리고 있어요") {
                                SettingRow("car", "내 위치 공유", if (sharing) "켜짐" else "꺼짐") { if (sharing) vm.go("S18") else if (hasLocation || ui.demo) vm.share(true) else onLocation() }
                                AutoLabel("위치를 켜면 동행 참가자가 내 위치를 볼 수 있어요. 언제든 중지할 수 있어요.")
                                AutoButton("위치 권한 확인", enabled = setupAllowed && !hasLocation && !ui.demo, onClick = onLocation)
                                AutoButton("공유 없이 출발", true) { vm.go("S11") }
                                AutoButton("위치 공유 후 출발", enabled = !ui.busy && (hasLocation || ui.demo)) { vm.share(true) }
                            }
                            "S11", "S19", "E02" -> {
                                Column(Modifier.align(Alignment.TopStart).padding(24.dp).width(328.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    SmallCard {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            FigmaAsset("arrow", Modifier.size(52.dp))
                                            Column(Modifier.padding(start = 18.dp)) {
                                                Text(if (ui.demo) "350 m" else turn?.let { "${it.distanceM.toInt()} m" } ?: "안내 대기", fontSize = 38.sp, fontWeight = FontWeight.Bold)
                                                AutoLabel(if (ui.demo) "인구중앙길 방면 우회전" else turn?.description ?: "확정 목적지로 안내를 요청해요")
                                            }
                                        }
                                    }
                                    if (ui.demo) SmallCard { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { repeat(4) { FigmaAsset(if (it < 2) "arrow1" else "arrow2", Modifier.size(32.dp)) } } }
                                }
                                AutoPanel(tripTitle, if (ui.demo) "3대 동행 중 · 시연" else "${trip?.members?.size ?: 0}대 참여", Modifier.align(Alignment.TopEnd).padding(24.dp).width(336.dp).heightIn(max = 592.dp), onBack = { configure("S07") }) {
                                    MemberList(ui.demo, trip, (call as? CallState.Connected)?.participants?.filter { it.speaking }?.map { it.identity }.orEmpty(), remoteLocations.associate { it.memberId to if (tick - it.updatedAtMillis in 0..120_000) "${(tick - it.updatedAtMillis) / 1000}초 전 갱신" else "위치 갱신이 늦어지고 있어요" }, ui.screen == "E02")
                                    AutoButton(if (sharing) "내 위치 공유 중 · 중지" else "내 위치 공유 꺼짐") { if (sharing) vm.go("S18") else vm.go("S10") }
                                    if (!ui.demo) AutoLabel(locationStatus)
                                    AutoButton("목적지 변경") { configure("S08") }
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        AutoButton("통화", modifier = Modifier.weight(1f)) { vm.go(if (activeCall) "S14" else "S13") }
                                        AutoButton("음악", true, modifier = Modifier.weight(1f)) { vm.go("S16") }
                                    }
                                    AutoButton("동행 나가기") { vm.go("S20") }
                                }
                                SmallCard(Modifier.align(Alignment.BottomStart).padding(24.dp).fillMaxWidth(.64f).clickable { vm.go("S12") }) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (ui.demo) "18분" else destination?.let { "${it.remainTimeSec / 60}분" } ?: "안내 요청", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                                        Column { Text(destName ?: "공동 목적지 미정", fontSize = 20.sp, fontWeight = FontWeight.Bold); AutoLabel(if (ui.demo) "12.4 km · 시연 경로" else if (guiding) "차량 내비 안내 중" else "차량 내비에서 시작을 확인해 주세요") }
                                    }
                                }
                            }
                            "S12" -> title("내 차량 안내", destName ?: "공동 목적지를 먼저 확정해 주세요") {
                                AutoLabel("내 차량의 내비게이션에 목적지를 전달해요. 차량마다 안내 경로는 다를 수 있어요.")
                                AutoLabel(if (ui.demo) "시연 경로 · 내비 요청 없음" else if (request?.applied == true) "내비 목적지 반영 확인됨" else if (request != null) "요청 후 차량 내비 응답 대기" else "아직 요청하지 않았어요")
                                naviError?.let { AutoLabel(it, AutoColors.Amber) }
                                AutoButton("내비에 안내 요청", true, enabled = ui.demo || trip?.destination != null) {
                                    if (ui.demo) vm.message("시연 모드에서는 실제 안내를 시작하지 않아요")
                                    else trip?.destination?.let { if (!navi.requestRoute(it)) vm.go("E03") }
                                }
                                AutoButton("동행 지도", onClick = home)
                            }
                            "S13", "S14", "S15", "E04" -> AutoPanel("동행 통화", if (ui.demo) "통화 UI 시연 · 음성 전송 없음" else when(call) {
                                CallState.Idle -> "참여 전"; CallState.Connecting -> "연결 중"; CallState.Reconnecting -> "재연결 중"; is CallState.Failed -> "연결 실패"; is CallState.Connected -> "연결됨"
                            }, Modifier.align(Alignment.TopEnd).padding(24.dp).width(446.dp).heightIn(max = 592.dp), onBack = home) {
                                if (activeCall) {
                                    MemberList(ui.demo, trip, (call as? CallState.Connected)?.participants?.filter { it.speaking }?.map { it.identity }.orEmpty())
                                    AutoButton(if (if (ui.demo) ui.demoMic else (call as? CallState.Connected)?.micEnabled == true) "음소거" else "음소거 해제") { vm.mic() }
                                    AutoButton("통화 나가기", onClick = vm::disconnectCall)
                                } else {
                                    Text("같은 길 위에서,\n목소리로 함께.", fontSize = 30.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold)
                                    AutoLabel("통화에 참여할 때만 마이크를 사용해요.")
                                    (call as? CallState.Failed)?.let { AutoLabel(it.reason, AutoColors.Amber) }
                                    if (!hasMic && !ui.demo) { AutoLabel("마이크 권한을 확인해 주세요."); AutoButton("마이크 사용 동의", enabled = setupAllowed, onClick = onMic) }
                                    AutoButton("통화 참여", true, !ui.busy && call !is CallState.Connecting && (hasMic || ui.demo)) { vm.connectCall() }
                                }
                                AutoButton("동행 지도", onClick = home)
                            }
                            "S16", "S17" -> AutoPanel("함께 듣는 음악", "음악 서비스 미연동 · 시연 콘텐츠", Modifier.align(Alignment.TopEnd).padding(24.dp).width(526.dp).heightIn(max = 592.dp), onBack = home) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    Box(Modifier.size(220.dp).clip(RoundedCornerShape(20.dp))) {
                                        FigmaAsset("cover", Modifier.fillMaxSize())
                                        Text("COASTLINE", color = AutoColors.Panel, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                                        Text("ON THE WAY", fontSize = 15.sp, modifier = Modifier.align(Alignment.BottomStart).padding(16.dp))
                                    }
                                    Column { Text("바다로 가는 길", fontSize = 24.sp, fontWeight = FontWeight.Bold); AutoLabel("동행 플레이리스트"); AutoLabel("COASTLINE") }
                                }
                                Text(if (ui.musicRequested) "응답 대기 화면 시연" else "이 곡을 함께 들어볼까요?", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                AutoLabel("실제 요청 전송과 음원 재생은 제공하지 않아요.")
                                AutoButton(if (ui.musicRequested) "요청 시연 취소" else "함께 듣기 요청 시연", true, ui.demo) { vm.music(!ui.musicRequested) }
                                AutoButton("동행 지도", onClick = home)
                            }
                            "S18", "S20", "S29" -> title(if (ui.screen == "S18") "위치 공유" else if (ui.screen == "S20") "동행 나가기" else "주변 동행 찾기", "내가 선택한 공유만 바뀌어요") {
                                Text(when(ui.screen) { "S18" -> "내 위치 공유를 중지할까요?"; "S20" -> "이번 동행을 나갈까요?"; else -> "주변 찾기를 끄시겠어요?" }, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                AutoLabel(when(ui.screen) { "S18" -> "위치 전송을 중지해요. 동행방과 통화는 유지돼요."; "S20" -> "내 위치 공유와 통화를 종료해요. 다른 참가자는 동행을 계속해요."; else -> "새 동행 탐색을 멈춰요. 기존 동행방은 유지돼요." })
                                AutoButton("취소", onClick = home)
                                AutoButton(if (ui.busy && ui.screen != "S18") "처리 중…" else "확인", true, ui.screen == "S18" || !ui.busy) {
                                    when(ui.screen) { "S18" -> vm.stopSharing(); "S20" -> vm.leave(); else -> { vm.nearby { it.stop() }; vm.go("S21") } }
                                }
                            }
                            "S02", "S03", "S04" -> title("친구 · 연결 요청", "친구 서버 미연동${if (ui.demo) " · 시연" else ""}") {
                                if (ui.demo) {
                                    MemberTile("규원", "친구 · 시연") { vm.go("S05") }
                                    MemberTile("자민", "친구 · 시연") { vm.go("S05") }
                                    when(ui.screen) {
                                        "S02" -> { MemberTile("민수", if (ui.demoFriendConnected) "친구 연결됨 · 시연" else "새 연결 요청 · 시연") { if (ui.demoFriendConnected) vm.go("S05") else vm.go("S03") }; AutoButton("보낸 요청 시연") { vm.go("S04") } }
                                        "S03" -> { Text("민수님이 연결을 요청했어요", fontSize = 24.sp); AutoLabel("서로 수락 후 연결돼요. 위치와 마이크는 따로 선택해요."); AutoButton("수락 시연", true, onClick = vm::acceptFriend); AutoButton("거절") { vm.go("S02") } }
                                        else -> { AutoLabel("민수님의 응답을 기다려요 · 시연"); AutoButton("요청 취소") { vm.go("S02") } }
                                    }
                                } else AutoLabel("친구 목록과 연결 요청은 준비 중이에요. 초대 코드로 동행에 참여할 수 있어요.")
                                AutoButton("초대 코드로 참여") { configure("S06") }
                                AutoButton("홈으로", onClick = home)
                            }
                            "S21", "S22", "S23", "S24", "S25", "S26", "S27", "S28", "S30x", "E07", "E08", "E09", "E10" -> title("주변 동행 찾기", if (ui.demo) "같은 목적지, 가까운 곳에서 · 시연" else "주변 탐색 서버 미연동") {
                                NearbyContent(ui, vm, setupAllowed, home)
                            }
                            "SETTINGS" -> title("설정과 이용 상태", "공유와 권한을 직접 관리해요") {
                                AutoLabel("위치: $locationStatus")
                                AutoButton("위치 사용 동의", enabled = !hasLocation && setupAllowed, onClick = onLocation)
                                AutoButton(if (sharing) "위치 공유 중지" else "위치 공유 설정", enabled = inTrip) { if (sharing) vm.stopSharing() else vm.go("S10") }
                                AutoButton("마이크 권한 확인", enabled = !hasMic && setupAllowed, onClick = onMic)
                                if (ui.demo) AutoButton("시연 종료 · 실제 모드", true, onClick = vm::endPreview)
                                else AutoButton("디자인 시연 시작", true, enabled = !inTrip && setupAllowed, onClick = vm::preview)
                                if (inTrip) AutoButton("동행 나가기") { vm.go("S20") }
                                if (ui.demo) { AutoButton("예외 상태 살펴보기") { vm.go("EXCEPTIONS") }; AutoButton("휴식 추천 · 화면 시연") { configure("REST") }; AutoButton("여행 앨범 · 화면 시연") { configure("ALBUM") } }
                            }
                            "EXCEPTIONS" -> title("예외 화면 시연", "실제 오류와 분리된 디자인 확인") {
                                listOf("E01" to "연결 끊김", "E02" to "오래된 위치", "E03" to "내비 응답 없음", "E04" to "마이크 권한", "E05" to "초대 만료", "E06" to "조작 제한", "E07" to "주변 결과 없음", "E08" to "위치 없음", "E09" to "요청 만료", "E10" to "차단").forEach { (code, label) -> AutoButton(label) { vm.go(code) } }
                            }
                            "REST" -> title("잠시 쉬어갈까요?", "추가 화면 시연 · 추천 API 미연동") {
                                AutoLabel("실제 영업 여부·거리·추천 결과가 아니에요.")
                                MemberTile("바다 산책", "여행지 예시 · 정차 후 확인") { vm.message("추천 서비스가 연결되면 장소 정보를 표시해요") }
                                MemberTile("한 끼 쉬어가기", "식당 예시 · 정차 후 확인") { vm.message("실제 식당 정보는 아직 없어요") }
                                AutoButton("동행 지도", onClick = home)
                            }
                            "ALBUM" -> title("우리의 여행 앨범", "추가 화면 시연 · 사진을 올리지 않아요") {
                                FigmaAsset("cover", Modifier.fillMaxWidth().height(220.dp))
                                Text("함께 남긴 순간", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                AutoLabel("아직 불러온 사진이 없어요. 서버 사진 API는 있지만 이 화면의 조회·업로드는 후속 연결이 필요해요.")
                                AutoButton("동행 지도", onClick = home)
                            }
                            else -> title(when(ui.screen) { "E06" -> "정차 후 이용해 주세요"; "E01" -> "동행 연결이 끊겼어요"; "E03" -> "내비 응답을 확인해 주세요"; "E05" -> "초대에 참여할 수 없어요"; else -> "상태를 확인해 주세요" }, "현재 정보를 확인한 뒤 다시 시도해 주세요") {
                                AutoLabel(when(ui.screen) { "E06" -> "차량 조작 제한이 해제된 뒤 입력·검색·설정을 이용할 수 있어요. 상태 미확인 중에도 제한돼요."; "E01" -> "일행의 최신 위치를 확인할 수 없어요. 마지막 정보가 현재 위치를 뜻하지 않아요."; "E03" -> "합의한 목적지는 유지돼요. 차량 내비 연결과 안내 시작을 확인해 주세요."; else -> "이전 요청의 성공을 확인할 수 없어요." })
                                AutoButton("동행 지도", true, onClick = home)
                                if (ui.screen == "E01") AutoButton("다시 연결") { if (ui.demo) home() else vm.repository.retryConnection() }
                            }
                        }
                        val error = ui.message ?: if (!ui.demo) serverError else null
                        if (error != null || (inTrip && !ui.demo && connection != ConnectionStatus.Connected)) {
                            Surface(Modifier.align(Alignment.BottomCenter).padding(16.dp).widthIn(max = 700.dp), color = AutoColors.Panel, shape = RoundedCornerShape(16.dp), shadowElevation = 6.dp) {
                                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(error ?: "동행 연결을 복구하고 있어요. 최신 위치를 표시하지 않아요.", color = AutoColors.Amber, fontSize = 18.sp, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { vm.message(null); vm.repository.error.value = null; if (connection == ConnectionStatus.Disconnected) vm.repository.retryConnection() }) { Text("확인") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoPanel(title: String, subtitle: String, modifier: Modifier, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, color = AutoColors.Panel, shape = RoundedCornerShape(22.dp), shadowElevation = 4.dp) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack, Modifier.size(48.dp)) { FigmaAsset("back", Modifier.size(26.dp)) }
                Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp)
            }
            AutoLabel(subtitle)
            Spacer(Modifier.height(20.dp))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
        }
    }
}

@Composable
private fun SmallCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, color = AutoColors.Panel, shape = RoundedCornerShape(20.dp), shadowElevation = 3.dp) { Column(Modifier.padding(20.dp), content = content) }
}

@Composable
private fun SettingRow(icon: String, label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 88.dp).clip(RoundedCornerShape(15.dp)).background(AutoColors.Raised).clickable(onClick = onClick).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        FigmaAsset(icon, Modifier.size(28.dp))
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) { Text(label, fontSize = 16.sp, color = AutoColors.Muted); Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        FigmaAsset("chevron", Modifier.size(23.dp))
    }
}

@Composable
private fun AutoField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, shape = RoundedCornerShape(14.dp))
}

@Composable
private fun MemberTile(name: String, state: String, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 88.dp).clip(RoundedCornerShape(16.dp)).background(AutoColors.Raised)
        .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        FigmaAsset("car", Modifier.size(32.dp))
        Column(Modifier.padding(start = 18.dp)) { Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text(state, fontSize = 16.sp, color = AutoColors.Muted) }
    }
}

@Composable
private fun MemberList(demo: Boolean, trip: Trip?, speaking: List<String>, updates: Map<String, String> = emptyMap(), stale: Boolean = false) {
    if (demo) { MemberTile("규원", if (stale) "위치 갱신 지연 · 시연" else "300 m 앞 · 시연 위치"); MemberTile("자민", "500 m 뒤 · 시연 위치") }
    else trip?.members?.forEach { MemberTile(it.carLabel, if (it.id in speaking) "말하는 중" else if (it.sharingLocation) updates[it.id] ?: "위치 갱신 대기" else "위치 공유 꺼짐") }
}

@Composable
private fun DemoMap(route: Boolean, nearby: Boolean) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFFE9ECEA))) {
        FigmaAsset("terrain", Modifier.fillMaxSize())
        listOf(Triple("현남면", .40f, .08f), Triple("인구리", .48f, .64f), Triple("남애항", .84f, .77f), Triple("남애천", .61f, .80f), Triple("동해대로", .35f, .35f), Triple("인구중앙길", .43f, .49f), Triple("솔밭공원", .07f, .62f), Triple("죽도해변", .86f, .12f)).forEach { (name, x, y) ->
            Text(name, Modifier.offset(maxWidth * x, maxHeight * y), color = Color(0xFF6F7E82), fontSize = if (name in listOf("현남면", "인구리")) 25.sp else 17.sp)
        }
        if (route) {
            FigmaAsset("route", Modifier.fillMaxSize())
            FigmaAsset("heading", Modifier.offset(maxWidth * .33f, maxHeight * .73f).size(50.dp, 56.dp))
            DemoMarker("규원  300 m", AutoColors.Amber, Modifier.offset(maxWidth * .49f, maxHeight * .36f))
            DemoMarker("자민  500 m", AutoColors.Raised, Modifier.offset(maxWidth * .25f, maxHeight * .68f))
        } else DemoMarker(if (nearby) "내 주변" else "나", AutoColors.Blue, Modifier.offset(maxWidth * .61f, maxHeight * .36f))
        if (nearby) FigmaAsset("radar_range", Modifier.align(Alignment.CenterEnd).padding(end = 140.dp).size(440.dp))
        Surface(Modifier.align(Alignment.BottomEnd).padding(24.dp), color = AutoColors.Panel, shape = RoundedCornerShape(12.dp)) { Text("시연 지도 · 실제 지리/위치 아님", Modifier.padding(12.dp), fontSize = 16.sp, color = AutoColors.Amber) }
    }
}

@Composable
private fun DemoMarker(label: String, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.clip(RoundedCornerShape(9.dp)).background(AutoColors.Panel).padding(horizontal = 12.dp, vertical = 5.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(26.dp)).background(color).border(4.dp, Color.White, RoundedCornerShape(26.dp)), contentAlignment = Alignment.Center) { FigmaAsset("car", Modifier.size(28.dp)) }
    }
}

@Composable
private fun NearbyContent(ui: VehicleUi, vm: VehicleViewModel, allowed: Boolean, home: () -> Unit) {
    val n = ui.nearby
    if (!ui.demo) {
        SettingRow("radar", "주변 동행 찾기", "꺼짐 · 서비스 준비 중") { vm.message("주변 탐색 API가 연결되지 않았어요") }
        AutoLabel("동일 목적지·반경 내 탐색과 상호 수락은 시연 모드에서 미리 볼 수 있어요. 현재 위치를 주변 차량에 보내지 않아요.")
        AutoButton("홈으로", onClick = home)
        return
    }
    when(ui.screen) {
        "S21", "S22" -> {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(AutoColors.Raised).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("주변 동행 찾기", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Switch(n.enabled, enabled = allowed, onCheckedChange = { enabled ->
                    if (enabled && n.destinationId == null) vm.go("S30")
                    else if (enabled) { vm.nearby { it.copy(enabled = true) }; vm.go("S22") } else vm.go("S29")
                })
            }
            SettingRow("pin", "목적지", when(n.destinationId) { "demo-beach" -> "양양 해변 주차장"; "demo-1" -> "강릉 안목해변"; "demo-2" -> "속초 영금정"; else -> "선택해 주세요" }) { vm.go("S30") }
            SettingRow("radar", "탐색 반경 · 설계 예시", "${n.radiusM} m") { vm.go("S23") }
            AutoLabel("연결 전에는 정밀 위치를 공개하지 않아요.")
            AutoButton("주변 동행 보기", true, n.enabled) { vm.go(if (n.candidates(VehicleViewModel.candidates).isEmpty()) "E07" else "S24") }
            AutoButton("홈으로", onClick = home)
        }
        "S23" -> {
            Text("얼마나 가까운 동행을 찾을까요?", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            listOf(100, 500, 1000).forEach { r -> AutoButton("${if (n.radiusM == r) "✓ " else ""}$r m", n.radiusM == r, allowed) { vm.nearby { it.radius(r) }; vm.go(if (n.enabled) "S22" else "S21") } }
            AutoLabel("반경은 설계 예시예요. 반경 변경만으로 탐색을 켜지 않아요.")
        }
        "S24" -> {
            AutoLabel("반경 ${n.radiusM} m · 같은 목적지", AutoColors.Green)
            n.candidates(VehicleViewModel.candidates).forEach { c -> MemberTile(c.name, "약 ${c.distanceM} m · 시연") { vm.go("S25") } }
            AutoLabel("서로 수락한 뒤에 연결돼요. 위치 공유와 통화는 따로 선택해요.")
            AutoButton("받은 요청 시연") { vm.nearby { it.receive() }; vm.go("S27") }
            AutoButton("탐색 설정") { vm.go("S22") }
        }
        "S25", "S27" -> {
            Text(if (ui.screen == "S27") "하늘길님이 동행을 요청했어요" else "하늘길님과 함께 갈까요?", fontSize = 27.sp, fontWeight = FontWeight.Bold)
            AutoLabel("같은 목적지 · 양양 해변 주차장\n약 300 m · 시연")
            AutoLabel("위치와 마이크는 별도로 선택해요.")
            AutoButton(if (ui.screen == "S27") "수락 시연" else "연결 요청 시연", true, n.enabled && allowed) {
                if (ui.screen == "S27") { vm.nearby { it.acceptIncoming() }; vm.go("S28") } else { vm.nearby { it.send() }; vm.go("S26") }
            }
            AutoButton("거절 · 목록으로") { vm.nearby { it.copy(request = NearbyRequest.None) }; vm.go("S24") }
            AutoButton("차단 시연") { vm.nearby { it.block("demo-sky") }; vm.go("E10") }
        }
        "S26" -> {
            Text("하늘길님의 응답을 기다려요", fontSize = 27.sp, fontWeight = FontWeight.Bold)
            AutoLabel("수락 전에는 위치와 통화가 공유되지 않아요.")
            AutoButton("상대 수락 시연", true) { vm.nearby { it.peerAccepts() }; vm.go("S28") }
            AutoButton("요청 취소") { vm.nearby { it.copy(request = NearbyRequest.None) }; vm.go("S24") }
            AutoButton("만료 시연") { vm.nearby { it.copy(request = NearbyRequest.Invalid) }; vm.go("E09") }
        }
        "S28" -> {
            Text("하늘길님과 연결되었어요 · 시연", fontSize = 27.sp, fontWeight = FontWeight.Bold)
            AutoLabel("양쪽 수락 화면 시연이에요. 실제 친구 연결은 없어요. 위치 공유와 통화는 시작하지 않았어요.")
            AutoButton("동행 만들기", true) { vm.go("S05") }
            AutoButton("연결 목록") { vm.go("S02") }
        }
        else -> {
            Text(when(ui.screen) { "E07" -> "같은 목적지의 동행이 없어요"; "E08" -> "현재 위치를 확인할 수 없어요"; "E09" -> "이 요청을 연결할 수 없어요"; else -> "하늘길님을 차단했어요 · 시연" }, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            AutoLabel("시연 상태예요. 실제 연결과 위치 공유는 시작하지 않았어요.")
            AutoButton("설정으로", true) { vm.go(if (n.enabled) "S22" else "S21") }
            AutoButton("목록 새로 확인") { vm.go(if (n.candidates(VehicleViewModel.candidates).isEmpty()) "E07" else "S24") }
            AutoButton("찾기 끄기") { vm.nearby { it.stop() }; vm.go("S21") }
        }
    }
}

