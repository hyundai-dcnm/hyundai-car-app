package com.dcnm.carapp.ui.report

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** 흑백 QR 비트맵. 정차 중에만 그리며, 내용은 요약 링크만 담는다 (원본 신호는 차 안에 둔다). */
fun qrBitmap(content: String, sizePx: Int): ImageBitmap {
    val matrix = QRCodeWriter().encode(
        content, BarcodeFormat.QR_CODE, sizePx, sizePx,
        mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 2,
            // 링크가 1KB 안팎이라 밀도를 낮추려고 L. 차량 화면은 훼손·오염이 없다.
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L,
        ),
    )
    val pixels = IntArray(sizePx * sizePx) { i ->
        if (matrix[i % sizePx, i / sizePx]) 0xFF131417.toInt() else 0xFFFFFFFF.toInt()
    }
    return Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888).asImageBitmap()
}
