package com.dcnm.donghaeng.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcnm.donghaeng.core.call.CallClient
import com.dcnm.donghaeng.core.call.CallState
import com.dcnm.donghaeng.core.model.*
import com.dcnm.donghaeng.core.trip.Profile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class VehicleUi(
    val screen: String = "S01", val demo: Boolean = false, val demoTrip: Boolean = false,
    val nickname: String = "", val carLabel: String = "", val roomName: String = "", val code: String = "",
    val busy: Boolean = false, val message: String? = null,
    val nearby: NearbyPreview = NearbyPreview(), val demoSharing: Boolean = false,
    val demoCall: Boolean = false, val demoMic: Boolean = true, val musicRequested: Boolean = false,
    val demoDestination: Boolean = false,
    val demoFriendConnected: Boolean = false,
    val draftDestination: Destination? = null,
    val saved: SavedDestinations = SavedDestinations(),
    val favoriteSlot: String? = null,
)

class VehicleViewModel(val repository: ServerTripRepository, val call: CallClient, private val destinationStore: DestinationStore? = null) : ViewModel() {
    val ui = MutableStateFlow(VehicleUi(saved = destinationStore?.load() ?: SavedDestinations()))
    val album = MutableStateFlow(AlbumUi())
    private val destinationWrites = Mutex()
    init {
        viewModelScope.launch { repository.trip.map { it?.id }.distinctUntilChanged().collect { album.value = AlbumUi() } }
    }
    fun edit(key: String, value: String) = ui.update {
        when (key) {
            "nickname" -> it.copy(nickname = value.take(20))
            "car" -> it.copy(carLabel = value.take(20))
            "name" -> it.copy(roomName = value.take(30))
            else -> it.copy(code = value.uppercase().filter(Char::isLetterOrDigit).take(6))
        }
    }
    fun go(screen: String) = ui.update { it.copy(screen = screen, message = null, favoriteSlot = if (screen == "S08") it.favoriteSlot else null) }
    fun message(text: String?) = ui.update { it.copy(message = text) }
    fun preview() { if (repository.trip.value == null) ui.value = VehicleUi(demo = true, nickname = "민지", carLabel = "민지네 차") }
    fun endPreview() { ui.value = VehicleUi(saved = destinationStore?.load() ?: SavedDestinations()) }
    fun create() {
        if (ui.value.demo) { ui.update { it.copy(demoTrip = true, screen = "S07") }; return }
        act { repository.createTrip(ui.value.roomName.ifBlank { "함께 가는 길" }, profile()); go("S07"); ui.value.draftDestination?.let { repository.proposeDestination(it) } }
    }
    fun join() {
        if (ui.value.demo) { ui.update { it.copy(demoTrip = true, screen = "S07") }; return }
        act { repository.joinTrip(ui.value.code, profile()); go("S07") }
    }
    fun propose(destination: Destination) {
        if (ui.value.demo) { ui.update { it.copy(demoDestination = true, screen = "S09") }; return }
        act { repository.proposeDestination(destination); go("S09") }
    }
    fun accept() {
        if (ui.value.demo) { go("S10"); return }
        repository.trip.value?.proposal?.let { p -> act { repository.acceptDestination(p.id); go("S10") } }
    }
    fun share(value: Boolean) {
        if (ui.value.demo) { ui.update { it.copy(demoSharing = value, screen = "S11") }; return }
        act { repository.setLocationSharing(value); go("S11") }
    }
    fun stopSharing() {
        if (ui.value.demo) { ui.update { it.copy(demoSharing = false, screen = "S19") }; return }
        repository.pauseLocationSending()
        go("S19")
        message("이 기기의 위치 전송을 중지했어요. 서버 반영을 확인하고 있어요.")
        viewModelScope.launch {
            try { repository.setLocationSharing(false); message(null) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { message("이 기기의 위치 전송은 중지됐어요. 서버의 공유 표시를 끄려면 다시 시도해 주세요.") }
        }
    }
    fun connectCall() {
        if (ui.value.demo) { ui.update { it.copy(demoCall = true, screen = "S14") }; return }
        act { call.connect(repository.callCredentials()); go("S14") }
    }
    fun mic() {
        if (ui.value.demo) { ui.update { it.copy(demoMic = !it.demoMic, screen = if (it.demoMic) "S15" else "S14") }; return }
        act { val s = call.state.value as? CallState.Connected ?: return@act; call.setMicEnabled(!s.micEnabled) }
    }
    fun disconnectCall() {
        if (ui.value.demo) { ui.update { it.copy(demoCall = false, screen = "S11") }; return }
        act { call.disconnect(); go("S11") }
    }
    fun leave() {
        if (ui.value.demo) { ui.value = VehicleUi(demo = true, nickname = "민지", carLabel = "민지네 차"); return }
        act { call.disconnect(); repository.leaveTrip(); go("S01") }
    }
    fun nearby(change: (NearbyPreview) -> NearbyPreview) { if (ui.value.demo) ui.update { it.copy(nearby = change(it.nearby)) } }
    fun music(value: Boolean) { if (ui.value.demo) ui.update { it.copy(musicRequested = value, screen = if (value) "S17" else "S16") } }
    fun acceptFriend() { if (ui.value.demo) ui.update { it.copy(demoFriendConnected = true, screen = "S02") } }
    fun chooseDestination(destination: Destination) {
        if (ui.value.demo) { propose(destination); return }
        val slot = ui.value.favoriteSlot
        val saved = ui.value.saved.copy(recent = (listOf(destination) + ui.value.saved.recent.filterNot { it.location == destination.location }).take(6),
            favorites = if (slot == null) ui.value.saved.favorites else ui.value.saved.favorites + (slot to destination))
        ui.update { it.copy(saved = saved, draftDestination = destination, favoriteSlot = null) }
        persistDestinations(saved)
        if (slot != null) go(if (repository.trip.value == null) "S01" else "S11")
        else if (repository.trip.value != null) propose(destination) else go("S01")
    }
    fun favorite(slot: String) {
        ui.value.saved.favorites[slot]?.let { chooseDestination(it); return }
        ui.update { it.copy(favoriteSlot = slot, screen = "S08") }
    }
    fun clearHistory() {
        ui.update { it.copy(saved = SavedDestinations(), draftDestination = null) }
        persistDestinations(SavedDestinations())
    }
    private fun persistDestinations(value: SavedDestinations) {
        viewModelScope.launch {
            try { destinationWrites.withLock { withContext(Dispatchers.IO) { destinationStore?.save(value) } } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { message("목적지 기록을 저장하지 못했어요. 다시 시도해 주세요") }
        }
    }
    fun refreshAlbum() {
        if (album.value.loading || ui.value.demo) return
        viewModelScope.launch {
            album.update { it.copy(loading = true, error = null) }
            try { val photos = repository.listPhotos(); album.update { it.copy(photos = photos) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { album.update { it.copy(error = e.message ?: "사진을 불러오지 못했어요") } }
            finally { album.update { it.copy(loading = false) } }
        }
    }
    fun preparePhoto(context: Context, uri: Uri) {
        if (ui.value.demo || album.value.loading) return
        val tripId = repository.trip.value?.id ?: return
        viewModelScope.launch {
            album.update { it.copy(loading = true, error = null, pending = null) }
            try {
                val jpeg = withContext(Dispatchers.IO) {
                    val input = context.contentResolver.openInputStream(uri) ?: error("사진을 열 수 없어요")
                    val bytes = input.use { stream ->
                        val out = ByteArrayOutputStream(); val buffer = ByteArray(8192)
                        while (true) { val count = stream.read(buffer); if (count < 0) break; require(out.size() + count <= 10 * 1024 * 1024) { "원본 사진은 10MB까지 선택할 수 있어요" }; out.write(buffer, 0, count) }
                        out.toByteArray()
                    }
                    val bitmap = orientedPhoto(bytes) ?: error("지원하지 않는 사진이에요")
                    ByteArrayOutputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out); bitmap.recycle(); out.toByteArray() }
                }
                if (repository.trip.value?.id == tripId) album.update { it.copy(pending = jpeg) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { album.update { it.copy(error = e.message ?: "사진을 준비하지 못했어요") } }
            finally { album.update { it.copy(loading = false) } }
        }
    }
    fun clearPendingPhoto() { album.update { it.copy(pending = null) } }
    fun uploadPendingPhoto() {
        val bytes = album.value.pending ?: return
        albumAction { repository.uploadPhoto(bytes); album.update { it.copy(pending = null) } }
    }
    fun deletePhoto(id: String) = albumAction { repository.deletePhoto(id) }
    private fun albumAction(block: suspend () -> Unit) {
        if (album.value.loading || ui.value.demo) return
        viewModelScope.launch {
            album.update { it.copy(loading = true, error = null) }
            try { block(); val photos = repository.listPhotos(); album.update { it.copy(photos = photos) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { album.update { it.copy(error = e.message ?: "사진 요청을 완료하지 못했어요") } }
            finally { album.update { it.copy(loading = false) } }
        }
    }
    private fun profile() = Profile(ui.value.nickname.trim(), ui.value.carLabel.trim())
    private fun act(block: suspend () -> Unit) {
        if (ui.value.busy) return
        ui.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try { block() } catch (e: CancellationException) { throw e }
            catch (e: Exception) { message(e.message ?: "요청을 완료하지 못했어요") }
            finally { ui.update { it.copy(busy = false) } }
        }
    }
    companion object {
        val destinations = listOf(Destination("양양 해변 주차장", LatLng(38.0306, 128.7166)), Destination("강릉 안목해변", LatLng(37.7717, 128.9476)), Destination("속초 영금정", LatLng(38.2133, 128.6002)))
        val candidates = listOf(NearbyCandidate("demo-sky", "하늘길", "demo-beach", 300), NearbyCandidate("demo-other", "다른 목적지", "elsewhere", 200), NearbyCandidate("demo-far", "먼 동행", "demo-beach", 900))
    }
}
