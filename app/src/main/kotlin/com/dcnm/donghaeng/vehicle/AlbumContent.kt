package com.dcnm.donghaeng.vehicle

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AlbumUi(val photos: List<TripPhoto> = emptyList(), val loading: Boolean = false, val error: String? = null, val pending: ByteArray? = null)

internal fun photoThumbnail(bytes: ByteArray): android.graphics.Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var factor = 1
    while (bounds.outWidth / factor > 1600 || bounds.outHeight / factor > 1600) factor *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = factor })
}

internal fun orientedPhoto(bytes: ByteArray): android.graphics.Bitmap? {
    val original = photoThumbnail(bytes) ?: return null
    val orientation = runCatching { android.media.ExifInterface(java.io.ByteArrayInputStream(bytes)).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1) }.getOrDefault(1)
    val matrix = android.graphics.Matrix().apply {
        when (orientation) {
            2 -> setScale(-1f, 1f)
            3 -> setRotate(180f)
            4 -> setScale(1f, -1f)
            5 -> { setRotate(90f); postScale(-1f, 1f) }
            6 -> setRotate(90f)
            7 -> { setRotate(270f); postScale(-1f, 1f) }
            8 -> setRotate(270f)
        }
    }
    if (matrix.isIdentity) return original
    return android.graphics.Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true).also { if (it != original) original.recycle() }
}

@Composable
fun ColumnScope.AlbumContent(vm: VehicleViewModel, demo: Boolean, allowed: Boolean, onPickPhoto: () -> Unit) {
    val album by vm.album.collectAsStateWithLifecycle()
    val trip by vm.repository.trip.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<TripPhoto?>(null) }
    var deleting by remember { mutableStateOf(false) }
    LaunchedEffect(trip?.id, demo) { if (!demo && trip != null) vm.refreshAlbum() }
    if (demo) {
        FigmaAsset("cover", Modifier.fillMaxWidth().height(160.dp))
        AutoLabel("시연 앨범이에요. 실제 사진은 동행에 참여한 뒤 올릴 수 있어요.")
        return
    }
    if (trip == null) { AutoLabel("동행에 먼저 참여해 주세요."); return }
    AutoLabel("이 동행의 참가자와 초대받은 웹 참여자가 사진을 볼 수 있어요.")
    AutoButton(if (album.loading) "사진 처리 중…" else "사진 선택", true, allowed && !album.loading, onClick = onPickPhoto)
    album.pending?.let { bytes ->
        val bitmap = remember(bytes) { photoThumbnail(bytes)?.asImageBitmap() }
        bitmap?.let { Image(it, "업로드할 사진 미리보기", Modifier.fillMaxWidth().height(160.dp), contentScale = ContentScale.Fit) }
        AutoLabel("이 사진을 현재 동행 앨범에 올릴까요? 위치 등 EXIF 정보를 제거해 전송해요.")
        AutoButton("이 사진 업로드", true, allowed && !album.loading) { vm.uploadPendingPhoto() }
        AutoButton("선택 취소", enabled = !album.loading) { vm.clearPendingPhoto() }
    }
    album.error?.let { AutoLabel(it, AutoColors.Amber) }
    AutoButton("새로고침", enabled = allowed && !album.loading, onClick = vm::refreshAlbum)
    if (album.photos.isEmpty() && !album.loading && album.error == null) AutoLabel("아직 올라온 사진이 없어요. 첫 순간을 남겨보세요.")
    if (album.photos.isNotEmpty()) LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxWidth().height(340.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(album.photos, key = { it.id }) { photo ->
                Column(Modifier.clickable(enabled = allowed) { selected = photo }) {
                    ServerPhoto(vm.repository, photo, Modifier.fillMaxWidth().height(140.dp))
                    AutoLabel(photo.uploaderName)
                    Text(SimpleDateFormat("M/d HH:mm", Locale.KOREA).format(Date(photo.createdAtMillis)))
                }
            }
    }
    selected?.let { photo ->
        AlertDialog(onDismissRequest = { selected = null; deleting = false }, title = { Text(if (deleting) "이 사진을 삭제할까요?" else photo.uploaderName) },
            text = { if (deleting) AutoLabel("동행 앨범에서 삭제돼요. 올린 사람이나 방장만 삭제할 수 있어요.") else ServerPhoto(vm.repository, photo, Modifier.fillMaxWidth().height(280.dp)) },
            confirmButton = { TextButton(enabled = allowed && !album.loading, onClick = {
                if (deleting) { vm.deletePhoto(photo.id); selected = null; deleting = false } else deleting = true
            }) { Text(if (deleting) "삭제 확인" else "사진 삭제") } },
            dismissButton = { TextButton(onClick = { selected = null; deleting = false }) { Text("닫기") } })
    }
}

@Composable
private fun ServerPhoto(repo: ServerTripRepository, photo: TripPhoto, modifier: Modifier) {
    val state by produceState<Pair<androidx.compose.ui.graphics.ImageBitmap?, Boolean>>(null to false, repo, photo.id) {
        value = try { withContext(Dispatchers.IO) { photoThumbnail(repo.photoBytes(photo.id))?.asImageBitmap() to false } }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { null to true }
    }
    Box(modifier) {
        state.first?.let { Image(it, "${photo.uploaderName}님의 동행 사진", Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            ?: AutoLabel(if (state.second) "사진을 불러오지 못했어요" else "사진을 불러오는 중…")
    }
}
