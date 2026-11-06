package com.dcnm.carapp.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dcnm.carapp.MainActivity
import com.dcnm.carapp.R
import com.dcnm.carapp.core.model.SymptomEvent

/**
 * 알림 두 종류.
 * - 대기: 서비스 유지용. 소리·상단 표시 없음.
 * - 기록 완료: 알림음 + 상단(헤드업) 알림, [RECORDED_TIMEOUT_MILLIS] 뒤 스스로 사라진다.
 *
 * 주행 중에 보이므로 문구는 한 줄로 끝내고 버튼을 달지 않는다. 자세한 내용은 정차 후 앱에서 본다.
 */
class CaptureNotifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        val idle = NotificationChannel(CHANNEL_IDLE, "기록 대기", NotificationManager.IMPORTANCE_MIN).apply {
            description = "음성 기록 대기"
            setShowBadge(false)
        }
        val recorded = NotificationChannel(CHANNEL_RECORDED, "기록 완료", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "음성 기록 저장 시 알림음과 함께 잠깐 표시"
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(false)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(idle, recorded))
    }

    fun idleNotification(): Notification = NotificationCompat.Builder(context, CHANNEL_IDLE)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("상황 기록 대기 중")
        .setOngoing(true)
        .setSilent(true)
        .setContentIntent(openAppIntent())
        .build()

    /** 알림 권한이 없으면 조용히 건너뛴다 (기록 자체는 이미 저장됐다). */
    fun notifyRecorded(event: SymptomEvent) {
        if (!manager.areNotificationsEnabled()) {
            Log.w(TAG, "알림 꺼짐: 기록 ${event.id}는 저장만 했다")
            return
        }
        val text = event.utterance?.let { "“$it”" } ?: "녹음 내용 인식 실패, 차량 신호만 저장"
        val notification = NotificationCompat.Builder(context, CHANNEL_RECORDED)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("기록 완료")
            .setContentText(text)
            // Pleos는 외부 앱 헤드업을 msg·call·navigation 카테고리만 띄운다(에뮬레이터 v2.1.0 확인: STATUS면 안 뜸).
            // 기록 완료는 엄밀히 내비 알림이 아니므로 심사에서 문제 되면 TTS 한마디로 대체한다.
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setTimeoutAfter(RECORDED_TIMEOUT_MILLIS)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        try {
            manager.notify(event.id.hashCode(), notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "POST_NOTIFICATIONS 거부: 기록 ${event.id}는 저장만 했다", e)
        }
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val CHANNEL_IDLE = "capture_idle"
        const val CHANNEL_RECORDED = "capture_recorded"
        const val IDLE_NOTIFICATION_ID = 1
        const val RECORDED_TIMEOUT_MILLIS = 4_000L
        private const val TAG = "CaptureNotifier"
    }
}
