package com.dcnm.carapp.capture

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.dcnm.carapp.CarApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 앱 화면이 뒤로 가도(순정 내비 화면) 기록을 받기 위한 포그라운드 서비스.
 *
 * - 시작: 앱 실행 시 [start]. 이후 [ACTION_RECORD] 인텐트가 음성 호출 진입점이다.
 *   Gleo·웨이크워드 연동은 검증 전이라(docs/02) 지금은 인텐트로 받는다. 시연은 scripts/record-voice.sh.
 * - 기록이 저장되면 [CaptureNotifier]가 알림음 + 상단 알림을 잠깐 띄운다.
 */
class CaptureService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var notifier: CaptureNotifier

    override fun onCreate() {
        super.onCreate()
        val container = (application as CarApp).container
        notifier = CaptureNotifier(this).also { it.createChannels() }
        ServiceCompat.startForeground(
            this, CaptureNotifier.IDLE_NOTIFICATION_ID, notifier.idleNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        container.symptomCapture.start()
        scope.launch { container.symptomCapture.recorded.collect(notifier::notifyRecorded) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RECORD) {
            val capture = (application as CarApp).container.symptomCapture
            capture.recordFromVoice(intent.getStringExtra(EXTRA_UTTERANCE)?.takeIf { it.isNotBlank() })
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_RECORD = "com.dcnm.carapp.action.RECORD_SYMPTOM"
        /** 호출 문장에 이미 증상이 담겨 있을 때(또는 시연 주입) 그 문장. 없으면 STT를 연다. */
        const val EXTRA_UTTERANCE = "utterance"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, CaptureService::class.java))
        }
    }
}
