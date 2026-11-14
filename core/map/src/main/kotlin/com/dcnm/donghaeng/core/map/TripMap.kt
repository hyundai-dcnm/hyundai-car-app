package com.dcnm.donghaeng.core.map

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dcnm.donghaeng.core.model.Destination
import com.dcnm.donghaeng.core.model.LatLng
import com.naver.maps.geometry.LatLng as NaverLatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMapSdk
import com.naver.maps.map.compose.ExperimentalNaverMapApi
import com.naver.maps.map.compose.MapUiSettings
import com.naver.maps.map.compose.Marker
import com.naver.maps.map.compose.MarkerState
import com.naver.maps.map.compose.NaverMap
import com.naver.maps.map.compose.rememberCameraPositionState
import com.naver.maps.map.util.MarkerIcons

/** 지도에 표시할 일행 차량. 서버에서 위치를 받기 시작하면 채운다 */
data class MapCar(val id: String, val label: String, val location: LatLng)

private const val TAG = "DonghaengMap"
private val SEOUL = NaverLatLng(37.5666, 126.9784)

/**
 * 여행방 지도(NAVER 지도). 내 차, 일행 차량, 확정된 목적지를 마커로 보여 준다.
 * 내 위치가 바뀌면 카메라가 따라간다.
 * Client ID 가 없으면(local.properties 에 NAVER_MAP_CLIENT_ID 미설정) 지도 대신 안내 문구를 보인다.
 */
@OptIn(ExperimentalNaverMapApi::class)
@Composable
fun TripMap(
    myLocation: LatLng?,
    myHeadingDeg: Double?,
    destination: Destination?,
    modifier: Modifier = Modifier,
    others: List<MapCar> = emptyList(),
) {
    val clientId = BuildConfig.NAVER_MAP_CLIENT_ID
    if (clientId.isBlank()) {
        Notice("지도 키가 없어요\nlocal.properties 에 NAVER_MAP_CLIENT_ID 를 넣어 주세요", modifier)
        return
    }

    val context = LocalContext.current
    var authError by remember { mutableStateOf<String?>(null) }
    // 지도 뷰가 만들어지기 전에 한 번 설정한다
    remember(clientId) {
        NaverMapSdk.getInstance(context).apply {
            client = NaverMapSdk.NcpKeyClient(clientId)
            onAuthFailedListener = NaverMapSdk.OnAuthFailedListener { e ->
                Log.w(TAG, "지도 인증 실패", e)
                authError = when (e) {
                    is NaverMapSdk.UnauthorizedClientException -> "지도 인증 실패(401)\n콘솔에 등록한 패키지 이름을 확인해 주세요"
                    is NaverMapSdk.QuotaExceededException -> "지도 인증 실패(429)\n콘솔에서 Dynamic Map 선택 여부를 확인해 주세요"
                    else -> "지도 인증 실패: ${e.errorCode}"
                }
            }
        }
    }

    val camera = rememberCameraPositionState {
        position = CameraPosition(myLocation?.toNaver() ?: SEOUL, 15.0)
    }
    LaunchedEffect(myLocation) {
        myLocation?.let { camera.move(CameraUpdate.scrollTo(it.toNaver())) }
    }

    Box(modifier) {
        NaverMap(
            cameraPositionState = camera,
            uiSettings = MapUiSettings(isZoomControlEnabled = true, isLocationButtonEnabled = false),
        ) {
            destination?.let {
                Marker(
                    state = MarkerState(position = it.location.toNaver()),
                    icon = MarkerIcons.RED,
                    captionText = it.name,
                )
            }
            others.forEach { car ->
                Marker(
                    state = MarkerState(position = car.location.toNaver()),
                    icon = MarkerIcons.GREEN,
                    captionText = car.label,
                )
            }
            myLocation?.let {
                Marker(
                    state = MarkerState(position = it.toNaver()),
                    icon = MarkerIcons.BLUE,
                    captionText = "나",
                    angle = (myHeadingDeg ?: 0.0).toFloat(),
                    zIndex = 1,
                )
            }
        }
        authError?.let { Notice(it, Modifier.align(Alignment.Center)) }
    }
}

@Composable
private fun Notice(text: String, modifier: Modifier) {
    Box(modifier.background(Color(0xFFE9EDF2)), contentAlignment = Alignment.Center) {
        Text(text, Modifier.padding(24.dp), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

private fun LatLng.toNaver() = NaverLatLng(lat, lng)
