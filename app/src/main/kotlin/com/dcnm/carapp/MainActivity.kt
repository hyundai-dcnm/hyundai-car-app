package com.dcnm.carapp

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dcnm.carapp.capture.CaptureService
import com.dcnm.carapp.ui.CarAppRoot
import com.dcnm.carapp.ui.theme.CarAppTheme

/**
 * 단일 Activity. 화면 전환은 Compose Navigation.
 * 주행/정차 판정은 [CarApp]에서 시작한 UxRestrictionsMonitor가 맡는다.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 기록 완료 알림(알림음 + 상단 표시)에 필요. 거부해도 기록은 저장된다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        // 앱을 한 번 열면 이후엔 내비 화면 뒤에서도 음성 기록을 받는다.
        CaptureService.start(this)
        val container = (application as CarApp).container
        setContent {
            CarAppTheme {
                CarAppRoot(container)
            }
        }
    }
}
