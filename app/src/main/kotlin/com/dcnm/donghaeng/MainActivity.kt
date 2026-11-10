package com.dcnm.donghaeng

import android.Manifest
import android.content.pm.PackageManager
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
                    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                        hasMic = it
                    }
                    // 방에 들어가 있으면 여행방 화면, 아니면 방 만들기·참여 화면.
                    val trip by container.trips.state.collectAsStateWithLifecycle()
                    if (trip == null) {
                        RoomScreen(viewModel(factory = viewModelFactory { initializer { RoomViewModel(container.trips) } }))
                    } else {
                        TripScreen(
                            viewModel = viewModel(
                                key = trip!!.trip.id,
                                factory = viewModelFactory {
                                    initializer { TripViewModel(container.trips, container.call, container.location, container.navi) }
                                },
                            ),
                            hasMicPermission = hasMic,
                            onRequestMicPermission = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        )
                    }
                }
            }
        }
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
