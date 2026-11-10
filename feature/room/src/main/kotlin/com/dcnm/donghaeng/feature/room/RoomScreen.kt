package com.dcnm.donghaeng.feature.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** 방 만들기·초대 코드로 참여. 키보드 입력이 있으므로 정차 중에만 연다. */
@Composable
fun RoomScreen(viewModel: RoomViewModel, modifier: Modifier = Modifier) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Row(
        modifier.fillMaxSize().padding(48.dp),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("동행", style = MaterialTheme.typography.displaySmall)
            Text("같은 곳으로 가는 차들과 통화하고 위치를 나눠요", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(ui.nickname, viewModel::onNickname, Modifier.fillMaxWidth(), label = { Text("내 이름") })
            OutlinedTextField(ui.carLabel, viewModel::onCarLabel, Modifier.fillMaxWidth(), label = { Text("차 이름 (예: 자민네 차)") })
            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("새 여행방", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(ui.tripName, viewModel::onTripName, Modifier.fillMaxWidth(), label = { Text("방 이름") })
            Button(
                onClick = viewModel::create,
                enabled = ui.profileReady && !ui.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
            ) { Text("방 만들기", style = MaterialTheme.typography.titleMedium) }

            Text("초대받은 방", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp))
            OutlinedTextField(ui.inviteCode, viewModel::onInviteCode, Modifier.fillMaxWidth(), label = { Text("초대 코드") })
            OutlinedButton(
                onClick = viewModel::join,
                enabled = ui.profileReady && ui.inviteCode.isNotBlank() && !ui.busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
            ) { Text("참여하기", style = MaterialTheme.typography.titleMedium) }
        }
    }
}
