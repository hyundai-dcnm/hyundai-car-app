package com.dcnm.carapp.core.voice

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Pleos TextToSpeech 3.3.1 + SpeechToText 3.3.1 구현.
 *
 * - 모드는 ON_DEVICE 고정 (앱 등록 불필요, 오프라인). 한국어 인식률이 낮으면 HYBRID로 전환한다 (스파이크 4번).
 * - Pleos Connect에는 Android 기본 TTS 엔진이 없으므로 반드시 Pleos TTS SDK를 쓴다.
 * - STT는 텍스트만 돌려준다. 명령 실행(Gleo Interface)은 로드맵.
 */
class PleosVoiceManager(
    private val context: Context,
) : VoiceManager {

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: Flow<Boolean> = _isSpeaking

    // TODO(spike-4,5): TextToSpeech / SpeechToText 클라이언트 생성·initialize

    override suspend fun speak(text: String, rate: Float) {
        // TODO: tts.speak(text, rate) 완료 콜백까지 suspend
    }

    override suspend fun listenOnce(timeoutMillis: Long): String? {
        // TODO: stt.startListening → onResult 텍스트, timeout 시 null
        return null
    }

    override suspend fun askYesNo(question: String): Boolean? {
        speak(question)
        val answer = listenOnce() ?: return null
        return YesNoParser.parse(answer)
    }
}
