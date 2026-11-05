package com.dcnm.carapp.core.model

import kotlinx.serialization.Serializable

/**
 * 운전자가 "이상 느낌"을 말한 순간의 기록.
 *
 * - [utterance]: STT 인식 텍스트 (정차 후 수정 가능)
 * - [snapshot]: 발화 직전 N초 + 직후 M초의 신호 구간. 확보 못 한 구간은 [SignalSnapshot.coveredMillis]로 드러낸다.
 * - [trigger]: 사용자가 눌렀는지, 앱이 먼저 물었는지(급제동·조향·경고등)
 */
@Serializable
data class SymptomEvent(
    val id: String,
    val createdAtMillis: Long,
    val trigger: SymptomTrigger,
    val utterance: String?,
    val utteranceConfirmed: Boolean = false,
    val snapshot: SignalSnapshot,
    val status: SymptomStatus = SymptomStatus.RECORDED,
)

@Serializable
enum class SymptomTrigger { USER_TAP, USER_VOICE, HARD_BRAKE, SHARP_STEERING, TELLTALE_ON }

@Serializable
enum class SymptomStatus { RECORDED, CONFIRMED, VISIT_PLANNED, RESULT_RECORDED, RESOLVED, RECURRED }
