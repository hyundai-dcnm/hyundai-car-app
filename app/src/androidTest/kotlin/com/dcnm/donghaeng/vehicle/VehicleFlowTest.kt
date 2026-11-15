package com.dcnm.donghaeng.vehicle

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.dcnm.donghaeng.DonghaengApp
import com.dcnm.donghaeng.core.trip.Profile
import com.dcnm.donghaeng.core.trip.ConnectionStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import com.dcnm.donghaeng.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import com.dcnm.donghaeng.core.model.Destination
import com.dcnm.donghaeng.core.model.LatLng

/** Runs only against a parked emulator, using the real platform restriction state. */
@RunWith(AndroidJUnit4::class)
class VehicleFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun tap(text: String) { compose.onAllNodesWithText(text).onLast().performScrollTo().performClick(); compose.waitForIdle() }
    private fun capture(name: String) {
        compose.waitForIdle()
        Thread.sleep(350) // Allow SurfaceFlinger to present the frame that semantics just verified.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val descriptor = instrumentation.uiAutomation.executeShellCommand("screencap -p /data/local/tmp/donghaeng-$name.png")
        java.io.FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        descriptor.close()
    }
    @Test fun parkedPreviewRequiresMutualConsentAndNeverStartsLocationOrAudio() {
        compose.waitUntil(15_000) { compose.onAllNodesWithText("설정 가능").fetchSemanticsNodes().isNotEmpty() }
        capture("real-home")
        compose.onNodeWithContentDescription("설정").performClick()
        tap("디자인 시연 시작")
        capture("home")
        tap("주변 동행 찾기")
        // Enabling without destination first requires selection.
        compose.onNode(isToggleable()).performClick()
        tap("양양 해변 주차장")
        compose.onNode(isToggleable()).performClick()
        capture("nearby-settings")
        tap("주변 동행 보기")
        capture("nearby-list")
        tap("하늘길")
        tap("연결 요청 시연")
        compose.onNodeWithText("하늘길님의 응답을 기다려요").assertIsDisplayed()
        tap("상대 수락 시연")
        compose.onNodeWithText("하늘길님과 연결되었어요 · 시연").assertIsDisplayed()
        tap("동행 만들기")
        tap("동행 만들기")
        tap("목적지 제안")
        tap("양양 해변 주차장")
        tap("이 목적지 수락")
        tap("공유 없이 출발")
        compose.onNodeWithText("내 위치 공유 꺼짐").assertExists()
        capture("trip")
        tap("통화")
        tap("통화 참여")
        tap("음소거")
        compose.onNodeWithText("음소거 해제").assertExists()
        capture("call-muted")
        tap("통화 나가기")
        tap("음악")
        tap("함께 듣기 요청 시연")
        compose.onNodeWithText("응답 대기 화면 시연").assertExists()
        capture("music")
    }
    @Test fun restrictionChangeClosesInputAndUnknownRemainsRestricted() {
        val container = (compose.activity.application as DonghaengApp).container
        val vm = VehicleViewModel(container.trips, container.call)
        val mode = mutableStateOf(UxMode.Unrestricted)
        vm.go("S05")
        compose.activity.runOnUiThread {
            compose.activity.setContent { VehicleApp(vm, mode.value, container.navi, false, false, "위치 권한 없음", {}, {}, {}) }
        }
        compose.onNodeWithText("내 이름").assertExists()
        compose.runOnIdle { mode.value = UxMode.Restricted }
        compose.waitForIdle()
        compose.onNodeWithText("내 이름").assertDoesNotExist()
        compose.onNodeWithText("정차 후 이용해 주세요").assertExists()
        capture("restricted")
        compose.runOnIdle { mode.value = UxMode.Unknown }
        compose.waitForIdle()
        compose.onNodeWithText("내 이름").assertDoesNotExist()
        compose.onNodeWithText("차량 상태 미확인 · 조작 제한").assertExists()
    }
    @Test fun deployedServerAdapterUsesSnapshotConsensusAndExplicitSharingConsent() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val repository = ServerTripRepository(scope)
        try {
            repository.createTrip("UI-app-" + java.util.UUID.randomUUID().toString().take(8), Profile("UI isolated test", "demo car"))
            withTimeout(40_000) { repository.connection.first { it == ConnectionStatus.Connected } }
            assertFalse(repository.consent.value)
            assertFalse(repository.trip.value!!.members.single().sharingLocation)
            repository.proposeDestination(VehicleViewModel.destinations.first())
            assertEquals("양양 해변 주차장", repository.trip.value!!.destination?.name)
            assertTrue(repository.callCredentials().url.startsWith("wss://"))
            assertTrue(repository.listPhotos().isEmpty())
            val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLUE) }
            val bytes = java.io.ByteArrayOutputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out); bitmap.recycle(); out.toByteArray() }
            val photo = repository.uploadPhoto(bytes)
            try {
                assertTrue(repository.listPhotos().any { it.id == photo.id })
                assertNotNull(photoThumbnail(repository.photoBytes(photo.id)))
                val app = compose.activity.application as DonghaengApp
                val albumVm = VehicleViewModel(repository, app.container.call).apply { go("ALBUM") }
                compose.activity.runOnUiThread {
                    compose.activity.setContent { VehicleApp(albumVm, UxMode.Unrestricted, app.container.navi, false, false, "위치 권한 없음", {}, {}, {}) }
                }
                compose.waitUntil(20_000) { albumVm.album.value.photos.any { it.id == photo.id } }
                compose.onNodeWithText("새로고침").performScrollTo()
                compose.waitUntil(20_000) { compose.onAllNodesWithContentDescription("${photo.uploaderName}님의 동행 사진").fetchSemanticsNodes().isNotEmpty() }
                capture("album")
            } finally { repository.deletePhoto(photo.id) }
            assertTrue(repository.listPhotos().isEmpty())
        } finally {
            if (repository.trip.value != null) repository.leaveTrip()
            scope.cancel()
        }
    }
    @Test fun destinationHistoryIsEncryptedAndRoundTrips() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = DestinationStore(context, "destination-test")
        val d = Destination("Synthetic saved beach", LatLng(38.0306, 128.7166))
        val saved = SavedDestinations(listOf(d), mapOf("집" to d))
        store.save(saved)
        assertEquals(saved, store.load())
        val encrypted = context.getSharedPreferences("destination-test", 0).getString("encrypted", "")!!
        assertFalse(encrypted.contains("Synthetic"))
        assertFalse(encrypted.contains("38.0306"))
        context.getSharedPreferences("destination-test", 0).edit().clear().commit()
    }
    @Test fun launcherIconRendersWithBrandSymbol() {
        val context = compose.activity
        val icon = context.packageManager.getApplicationIcon(context.packageName)
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, 256, 256)
        icon.draw(android.graphics.Canvas(bitmap))
        File(context.cacheDir, "icon.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        context.runOnUiThread {
            context.setContent {
                AutomotiveTheme {
                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        androidx.compose.ui.viewinterop.AndroidView(factory = { android.widget.ImageView(it).apply { setImageDrawable(icon) } }, modifier = androidx.compose.ui.Modifier.size(192.dp))
                    }
                }
            }
        }
        capture("launcher-icon")
    }
}
