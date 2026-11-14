package com.dcnm.donghaeng.feature.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcnm.donghaeng.core.call.CallState
import com.dcnm.donghaeng.core.trip.ConnectionStatus

/**
 * 여행방 화면. 왼쪽은 지도 자리(①이 채움), 오른쪽은 일행·목적지·통화.
 * 주행 중에 누를 수 있는 것은 큰 버튼(수락, 음소거, 통화 참여·종료)뿐이다.
 */
@Composable
fun TripScreen(
    viewModel: TripViewModel,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    modifier: Modifier = Modifier,
    /** 왼쪽 지도 영역. 앱에서 core:map 의 TripMap 을 넣는다. 비우면 회색 자리만 보인다 */
    map: @Composable (Modifier) -> Unit = { MapPlaceholder(it) },
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Row(modifier.fillMaxSize()) {
        map(Modifier.weight(1.4f).fillMaxHeight())
        Column(
            Modifier.width(560.dp).fillMaxHeight().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(ui, onLeave = viewModel::leaveTrip)
            ui.message?.let {
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(it, Modifier.weight(1f))
                        TextButton(onClick = viewModel::dismissMessage) { Text("닫기") }
                    }
                }
            }
            DestinationSection(ui, viewModel)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ui.members, key = { it.id }) { MemberItem(it) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("내 위치 공유", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Switch(checked = ui.sharingLocation, onCheckedChange = { viewModel.toggleLocationSharing() })
            }
            CallBar(
                ui.call,
                onJoin = { if (hasMicPermission) viewModel.joinCall() else onRequestMicPermission() },
                onToggleMic = viewModel::toggleMic,
                onLeave = viewModel::leaveCall,
            )
        }
    }
}

@Composable
private fun MapPlaceholder(modifier: Modifier) {
    Box(modifier.background(Color(0xFFE9EDF2)), contentAlignment = Alignment.Center) {
        Text("지도", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun Header(ui: TripUiState, onLeave: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(ui.tripName, style = MaterialTheme.typography.headlineMedium)
            val status = when (ui.connection) {
                ConnectionStatus.Connected -> "초대 코드 ${ui.inviteCode}"
                ConnectionStatus.Connecting -> "연결 중"
                ConnectionStatus.Reconnecting -> "다시 연결하는 중"
                ConnectionStatus.Disconnected -> "연결 끊김"
            }
            Text(status, style = MaterialTheme.typography.bodyLarge)
        }
        TextButton(onClick = onLeave) { Text("나가기") }
    }
}

@Composable
private fun DestinationSection(ui: TripUiState, viewModel: TripViewModel) {
    val proposal = ui.proposal
    when {
        proposal != null -> Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("목적지 제안: ${proposal.destination.name}", style = MaterialTheme.typography.titleLarge)
                Text("${proposal.accepted}/${proposal.total}대 수락")
                if (!proposal.acceptedByMe) {
                    Button(
                        onClick = { viewModel.accept(proposal.id) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    ) { Text("수락하고 같이 가기", style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
        ui.destination != null -> Text("목적지 ${ui.destination.name}", style = MaterialTheme.typography.titleLarge)
        else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("목적지를 제안해 보세요", style = MaterialTheme.typography.titleMedium)
            TripViewModel.sampleDestinations.forEach { dest ->
                OutlinedButton(onClick = { viewModel.propose(dest) }, modifier = Modifier.fillMaxWidth()) {
                    Text(dest.name)
                }
            }
        }
    }
}

@Composable
private fun MemberItem(member: MemberRow) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                member.carLabel + if (member.isMe) " (나)" else "",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(member.nickname, style = MaterialTheme.typography.bodyMedium)
        }
        val tags = buildList {
            if (member.speaking) add("말하는 중")
            else if (member.inCall) add("통화 중")
            if (!member.sharingLocation) add("위치 끔")
        }
        Text(tags.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CallBar(call: CallState, onJoin: () -> Unit, onToggleMic: () -> Unit, onLeave: () -> Unit) {
    val big = Modifier.heightIn(min = 72.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when (call) {
            is CallState.Connected -> {
                OutlinedButton(onClick = onToggleMic, modifier = big.weight(1f)) {
                    Text(if (call.micEnabled) "음소거" else "음소거 해제", style = MaterialTheme.typography.titleMedium)
                }
                Button(onClick = onLeave, modifier = big.weight(1f)) {
                    Text("통화 종료", style = MaterialTheme.typography.titleMedium)
                }
            }
            CallState.Connecting, CallState.Reconnecting -> Button(onClick = onLeave, modifier = big.fillMaxWidth()) {
                Text(if (call == CallState.Connecting) "연결 중… (취소)" else "다시 연결 중… (종료)")
            }
            CallState.Idle, is CallState.Failed -> Column(Modifier.fillMaxWidth()) {
                if (call is CallState.Failed) Text("통화 실패: ${call.reason}", color = MaterialTheme.colorScheme.error)
                Button(onClick = onJoin, modifier = big.fillMaxWidth()) {
                    Text("통화 참여", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
