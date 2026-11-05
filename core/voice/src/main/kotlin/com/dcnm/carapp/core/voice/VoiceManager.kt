package com.dcnm.carapp.core.voice

import kotlinx.coroutines.flow.Flow

/**
 * TTS/STT 통합 인터페이스. 주행 중에는 이 인터페이스만 사용자와 대화한다.
 *
 * 규칙:
 * - 한 문장씩만 말한다. 주행 중 장문 금지.
 * - 질문은 예/아니오 또는 한마디 답만 받는다.
 * - 내비 음성·통화와 겹치면 미룬다 (오디오 포커스는 구현체 책임).
 */
interface VoiceManager {
    suspend fun speak(text: String, rate: Float = 0.9f)

    /** 짧은 발화 1회 인식. 실패·무응답은 null. */
    suspend fun listenOnce(timeoutMillis: Long = 5_000): String?

    /** "예"/"아니오" 판정. 인식 실패는 null → UI 대형 버튼 폴백. */
    suspend fun askYesNo(question: String): Boolean?

    val isSpeaking: Flow<Boolean>
}
