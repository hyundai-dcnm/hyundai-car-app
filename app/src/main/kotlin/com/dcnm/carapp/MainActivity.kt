package com.dcnm.carapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.dcnm.carapp.ui.CarAppRoot
import com.dcnm.carapp.ui.theme.CarAppTheme

/**
 * 단일 Activity. 화면 전환은 Compose Navigation.
 *
 * TODO(spike-9): CarUxRestrictionsManager 등록 → container.vehicleRepository.onUxRestrictionsChanged(...)
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as CarApp).container
        setContent {
            CarAppTheme {
                CarAppRoot(container)
            }
        }
    }
}
