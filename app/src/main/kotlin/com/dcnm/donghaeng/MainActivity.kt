package com.dcnm.donghaeng

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dcnm.donghaeng.feature.room.RoomScreen
import com.dcnm.donghaeng.feature.room.RoomViewModel
import com.dcnm.donghaeng.feature.trip.TripScreen
import com.dcnm.donghaeng.feature.trip.TripViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as DonghaengApp).container
        setContent {
            MaterialTheme {
                Surface {
                    var hasMic by remember { mutableStateOf(granted(Manifest.permission.RECORD_AUDIO)) }
                    // 통화 알림 권한은 함께 묻되 거절해도 통화는 된다 (알림만 안 보임).
                    val micLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions(),
                    ) { hasMic = it[Manifest.permission.RECORD_AUDIO] == true }
                    // 방에 들어가 있으면 여행방 화면, 아니면 방 만들기·참여 화면.
                    val trip by container.trips.trip.collectAsStateWithLifecycle()
                    if (trip == null) {
                        RoomScreen(viewModel(factory = viewModelFactory { initializer { RoomViewModel(container.trips) } }))
                    } else {
                        TripScreen(
                            viewModel = viewModel(
                                key = trip!!.id,
                                factory = viewModelFactory {
                                    initializer { TripViewModel(container.trips, container.call) }
                                },
                            ),
                            hasMicPermission = hasMic,
                            onRequestMicPermission = { micLauncher.launch(callPermissions()) },
                        )
                    }
                }
            }
        }
    }

    private fun callPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
