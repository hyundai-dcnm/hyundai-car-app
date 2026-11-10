package com.dcnm.donghaeng.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.dcnm.donghaeng.DonghaengApp
import com.dcnm.donghaeng.MainActivity
import com.dcnm.donghaeng.R
import com.dcnm.donghaeng.core.call.CallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 통화 중에 앱이 뒤로 가도 마이크가 막히지 않게 하는 포그라운드 서비스.
 *
 * Android 는 화면에 없는 앱의 녹음을 무음으로 바꾼다. 에뮬레이터에서 순정 내비를 앞으로 띄우면
 * 몇 초 안에 silenced:true 로 바뀌었고, microphone 타입 포그라운드 서비스가 있으면 유지됐다.
 * 통화가 끝나면(Idle, Failed) 스스로 멈춘다.
 */
class CallService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val call by lazy { (application as DonghaengApp).container.call }
    private var observing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForegroundService 로 시작했으면 5초 안에 반드시 불러야 한다.
        startInForeground()
        if (intent?.action == ACTION_HANG_UP) scope.launch { call.disconnect() }
        if (!observing) {
            observing = true
            scope.launch {
                call.state.collect { if (it == CallState.Idle || it is CallState.Failed) stopSelf() }
            }
        }
        // 프로세스가 죽었다 살아나도 통화는 이미 끊겼으므로 다시 시작하지 않는다.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startInForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), type)
    }

    /*
     * Pleos 는 CATEGORY_CALL 알림을 왼쪽 패널 통화 위젯으로 띄운다 (에뮬레이터 2.1.0 확인).
     * - 첫 번째 액션은 초록 전화 버튼, 두 번째는 빨간 종료 버튼으로 그린다. 액션이 하나여도 초록이다.
     *   CallStyle 은 종료를 첫 번째로 넣어서 초록 버튼이 종료가 되므로 쓰지 않는다.
     * - 처음 올린 문구를 갱신하지 않는다. 그래서 상태(연결 중, 인원 수, 음소거)를 문구에 넣지 않는다.
     */
    private fun notification(): Notification {
        createChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_notification)
            .setContentTitle("동행 통화")
            .setContentText("일행과 통화 중")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(activityIntent())
            .addAction(0, "통화 화면", activityIntent())
            .addAction(0, "통화 종료", hangUpIntent())
            .build()
    }

    private fun activityIntent() = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun hangUpIntent() = PendingIntent.getService(
        this, 0,
        Intent(this, CallService::class.java).setAction(ACTION_HANG_UP),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        // LOW: 소리 없이 띄운다. 통화 중 표시용이라 운전자를 부를 일이 없다.
        val channel = NotificationChannel(CHANNEL_ID, "통화", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "call"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_HANG_UP = "com.dcnm.donghaeng.action.HANG_UP"

        /** 앱이 화면에 있을 때 불러야 한다. 뒤에서는 마이크 포그라운드 서비스를 시작할 수 없다. */
        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, CallService::class.java))
        }
    }
}
