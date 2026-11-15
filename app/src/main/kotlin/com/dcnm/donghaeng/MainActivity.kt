package com.dcnm.donghaeng

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dcnm.donghaeng.core.location.LocationSource
import com.dcnm.donghaeng.core.map.MapCar
import com.dcnm.donghaeng.core.map.TripMap
import com.dcnm.donghaeng.vehicle.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var ux: VehicleUxMonitor
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as DonghaengApp).container
        ux = VehicleUxMonitor(this)
        container.navi.start()
        setContent {
            val mode by ux.mode.collectAsStateWithLifecycle()
            val trip by container.trips.trip.collectAsStateWithLifecycle()
            val me by container.location.location.collectAsStateWithLifecycle()
            val locationState by container.location.status.collectAsStateWithLifecycle()
            val others by container.trips.locations.collectAsStateWithLifecycle()
            var hasMic by remember { mutableStateOf(granted(Manifest.permission.RECORD_AUDIO)) }
            var hasLocation by remember { mutableStateOf(container.location.hasPermission()) }
            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
            val vm: VehicleViewModel = viewModel(factory = viewModelFactory { initializer { VehicleViewModel(container.trips, container.call, DestinationStore(applicationContext)) } })
            val ui by vm.ui.collectAsStateWithLifecycle()
            val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasMic = it }
            val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                hasLocation = it
                if (it) container.location.start()
            }
            val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null && canConfigure(ux.mode.value)) vm.preparePhoto(applicationContext, uri)
            }
            DisposableEffect(Unit) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        hasMic = granted(Manifest.permission.RECORD_AUDIO)
                        hasLocation = container.location.hasPermission()
                        if (hasLocation) container.location.start()
                    }
                    if (event == Lifecycle.Event.ON_STOP) container.location.stop()
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }
            LaunchedEffect(Unit) { container.trips.checkHealth() }
            LaunchedEffect(ui.demo) {
                while (true) {
                    now = System.currentTimeMillis()
                    if (!ui.demo && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && container.location.hasPermission()) {
                        container.location.location.value?.let { container.trips.sendLocation(it.location, it.headingDeg, it.measuredAtMillis) }
                    }
                    delay(2_000)
                }
            }
            val fresh = hasLocation && me?.let { now - it.measuredAtMillis in 0..120_000 } == true
            VehicleApp(vm, mode, container.navi, hasLocation, hasMic,
                if (!hasLocation) "위치 권한 없음" else if (fresh) "현재 위치 수신 중" else "위치 수신 대기 또는 오래됨 · $locationState",
                onLocation = { if (canConfigure(ux.mode.value)) locationLauncher.launch(LocationSource.PERMISSION) },
                onMic = { if (canConfigure(ux.mode.value)) micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                onPickPhoto = { if (canConfigure(ux.mode.value)) photoLauncher.launch("image/*") },
                map = { modifier ->
                    if (com.dcnm.donghaeng.core.map.BuildConfig.NAVER_MAP_CLIENT_ID.isBlank()) {
                        MapDesignPreview(modifier)
                    } else TripMap(myLocation = me?.location?.takeIf { fresh }, myHeadingDeg = me?.headingDeg,
                        destination = trip?.destination, modifier = modifier,
                        others = others.filter { now - it.updatedAtMillis in 0..120_000 && trip?.members?.any { m -> m.id == it.memberId && m.sharingLocation } == true }
                            .filterNot { it.memberId == container.trips.myMemberId.value }
                            .map { l -> MapCar(l.memberId, trip?.members?.firstOrNull { it.id == l.memberId }?.carLabel ?: "동행", l.location) })
                })
        }
    }
    override fun onStart() { super.onStart(); ux.start() }
    override fun onStop() { ux.stop(); super.onStop() }
    override fun onDestroy() { (application as DonghaengApp).container.navi.release(); super.onDestroy() }
    private fun granted(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
