package com.dcnm.donghaeng.core.location

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.dcnm.donghaeng.core.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 이 차량의 위치. 서버 규약(contract/api.md)의 위치 전송 `{location, headingDeg?, speedKph?}` 에 그대로 쓴다.
 * @param headingDeg 진행 방향(0~360, 북=0). 에뮬레이터에서는 정지 상태라 항상 0 이 온다(실차 미확인).
 * @param measuredAtMillis SDK 가 위치를 계산한 시각
 */
data class VehicleLocation(
    val location: LatLng,
    val headingDeg: Double?,
    val altitudeM: Double,
    val confidence: Float,
    val measuredAtMillis: Long,
)

enum class LocationStatus {
    /** initialize() 전 */
    Idle,
    /** 이 차량(환경)에서 FusedLocation 을 지원하지 않음 */
    Unsupported,
    /** 위치 권한(PERMISSION)이 없음 */
    NoPermission,
    /** 초기화됨, 수신 전 */
    Ready,
    Receiving,
    Error,
}

/**
 * Pleos FusedLocation SDK 1.1.3 위치 수신.
 * 공식 문서 순서: 생성 → isSupported() → initialize() → getLastKnownLocation/register → unregister → release().
 * 에뮬레이터에서 약 1초 간격으로 수신되고, `adb emu geo fix <경도> <위도>` 로 옮기면 따라오는 것을 확인했다.
 */
class LocationSource(context: Context) {

    companion object {
        const val PERMISSION = "pleos.car.permission.FUSED_LOCATION"
        private const val TAG = "DonghaengLocation"
    }

    private val appContext = context.applicationContext
    private val fused = FusedLocation(appContext)

    private val _location = MutableStateFlow<VehicleLocation?>(null)
    /** 가장 최근 위치. 아직 없으면 null */
    val location: StateFlow<VehicleLocation?> = _location.asStateFlow()

    private val _status = MutableStateFlow(LocationStatus.Idle)
    val status: StateFlow<LocationStatus> = _status.asStateFlow()

    private var initialized = false
    private var receiving = false
    private var count = 0

    private val listener = object : FusedLocationListener {
        override fun onChanged(location: LocationData?) {   // 공식 문서 예시대로 null 가능
            location ?: return
            _location.value = location.toVehicleLocation()
            _status.value = LocationStatus.Receiving
            count++
            if (count == 1 || count % 50 == 0) Log.d(TAG, "위치 #$count: ${location.latitude}, ${location.longitude}, heading ${location.heading}")
        }

        override fun onError(exception: Exception) {
            Log.w(TAG, "위치 오류", exception)
            _status.value = LocationStatus.Error
        }
    }

    fun hasPermission() = appContext.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED

    /** 생성 직후 1회 */
    fun initialize() {
        if (initialized) return
        val supported = runCatching { fused.isSupported() }.getOrElse { Log.w(TAG, "isSupported 예외", it); false }
        if (!supported) { _status.value = LocationStatus.Unsupported; return }
        runCatching { fused.initialize() }
            .onSuccess { initialized = true; _status.value = LocationStatus.Ready }
            .onFailure { Log.w(TAG, "initialize 실패", it); _status.value = LocationStatus.Error }
    }

    /** 위치 받기 시작. 권한이 없으면 NoPermission 상태가 되고 아무것도 하지 않는다 */
    fun start() {
        if (!initialized || receiving) return
        if (!hasPermission()) { _status.value = LocationStatus.NoPermission; return }
        runCatching {
            fused.getLastKnownLocation(object : FusedLocationListener {
                override fun onChanged(location: LocationData?) {
                    if (location != null && _location.value == null) _location.value = location.toVehicleLocation()
                }
                override fun onError(exception: Exception) = Unit   // 아직 계산된 위치가 없을 때. 콜백을 기다린다
            })
            fused.registerFusedLocationCallback(listener)
        }.onSuccess { receiving = true }
            .onFailure { Log.w(TAG, "위치 수신 시작 실패", it); _status.value = LocationStatus.Error }
    }

    fun stop() {
        if (!receiving) return
        runCatching { fused.unregisterFusedLocationCallback(listener) }
        receiving = false
        if (initialized) _status.value = LocationStatus.Ready
    }

    /** initialize() 를 했으면 반드시 호출. 이후 이 객체는 다시 쓰지 않는다 */
    fun release() {
        stop()
        if (initialized) runCatching { fused.release() }
        initialized = false
        _status.value = LocationStatus.Idle
    }

    private fun LocationData.toVehicleLocation() = VehicleLocation(
        location = LatLng(latitude, longitude),
        headingDeg = heading.toDouble(),
        altitudeM = altitude,
        confidence = confidence,
        measuredAtMillis = timestampMillis,
    )
}
