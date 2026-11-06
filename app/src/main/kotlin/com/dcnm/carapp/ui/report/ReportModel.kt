package com.dcnm.carapp.ui.report

import com.dcnm.carapp.core.model.SignalSample
import com.dcnm.carapp.core.model.SignalSnapshot
import com.dcnm.carapp.core.model.SignalSource
import com.dcnm.carapp.core.model.SymptomStatus
import com.dcnm.carapp.core.model.SymptomTrigger
import kotlin.math.abs
import kotlin.math.roundToInt

/** 그래프 레인의 선 모양. 색이 아니라 모양과 이름으로 레인을 구분한다. */
enum class LaneStyle { SOLID, STEP, DASHED }

/** 발화 시점(0초) 기준 상대 시간(초)과 값. */
data class LanePoint(val seconds: Float, val value: Float)

data class Lane(
    val type: String,
    val label: String,
    val unit: String,
    val style: LaneStyle,
    val maxValue: Float,
    val points: List<LanePoint>,
    val sources: Set<SignalSource>,
)

private data class LaneSpec(val type: String, val label: String, val unit: String, val style: LaneStyle, val floor: Float)

private val laneSpecs = listOf(
    LaneSpec("speed", "차속", "km/h", LaneStyle.SOLID, 60f),
    LaneSpec("brake", "브레이크", "%", LaneStyle.STEP, 100f),
    LaneSpec("steering", "조향", "°", LaneStyle.DASHED, 30f),
)

/** 리포트 그래프에 그릴 세 레인. 신호가 없으면 points가 빈 레인을 돌려준다 (보간하지 않음). */
fun buildLanes(snapshot: SignalSnapshot): List<Lane> = laneSpecs.map { spec ->
    val samples = snapshot.samples.filter { it.type == spec.type }
    val points = samples.mapNotNull { s ->
        s.value.toFloatOrNull()?.let { LanePoint((s.epochMillis - snapshot.anchorMillis) / 1000f, if (spec.type == "steering") abs(it) else it) }
    }
    Lane(
        type = spec.type,
        label = spec.label,
        unit = spec.unit,
        style = spec.style,
        maxValue = maxOf(spec.floor, points.maxOfOrNull { it.value } ?: 0f),
        points = points,
        sources = samples.map { it.source }.toSet(),
    )
}

/** 신호 공백 구간(초). 연속 샘플 간격이 [gapSeconds]보다 길거나, 창 시작·끝까지 비어 있으면 공백이다. */
fun gaps(lane: Lane, fromSeconds: Float, toSeconds: Float, gapSeconds: Float = 2f): List<ClosedFloatingPointRange<Float>> {
    val xs = lane.points.map { it.seconds }.filter { it in fromSeconds..toSeconds }.sorted()
    if (xs.isEmpty()) return listOf(fromSeconds..toSeconds)
    val result = mutableListOf<ClosedFloatingPointRange<Float>>()
    if (xs.first() - fromSeconds > gapSeconds) result += fromSeconds..xs.first()
    xs.zipWithNext().forEach { (a, b) -> if (b - a > gapSeconds) result += a..b }
    if (toSeconds - xs.last() > gapSeconds) result += xs.last()..toSeconds
    return result
}

/**
 * 관측 요약 문장. 데이터로 확인되는 사실만 쓰고 원인·고장은 판정하지 않는다.
 * 신호가 없으면 "없음"이라고 쓴다 (없음 ≠ 이상 없음).
 */
fun observations(snapshot: SignalSnapshot): List<String> {
    val lanes = buildLanes(snapshot).associateBy { it.type }
    val lines = mutableListOf<String>()
    lanes["speed"]?.points?.takeIf { it.isNotEmpty() }?.let { p ->
        lines += "기록 구간 차속 ${p.minOf { it.value }.roundToInt()}–${p.maxOf { it.value }.roundToInt()} km/h"
    } ?: run { lines += "차속 신호 없음" }
    lanes["brake"]?.points?.takeIf { it.isNotEmpty() }?.let { p ->
        val peak = p.maxBy { it.value }
        if (peak.value > 0f) lines += "브레이크 입력 최대 ${peak.value.roundToInt()}% (${relative(peak.seconds)})"
        else lines += "브레이크 입력 관측 안 됨"
    } ?: run { lines += "브레이크 신호 없음" }
    lanes["steering"]?.points?.takeIf { it.isNotEmpty() }?.let { p ->
        lines += "조향 최대 ${p.maxOf { it.value }.roundToInt()}°"
    } ?: run { lines += "조향 신호 없음" }
    snapshot.samples.filter { it.type.startsWith("telltale:") && it.value == "true" }
        .map { it.type.removePrefix("telltale:") }.distinct()
        .forEach { lines += "경고등 켜짐 관측: $it" }
    return lines
}

fun relative(seconds: Float): String {
    val s = seconds.roundToInt()
    return when {
        s == 0 -> "녹음 시점"
        s < 0 -> "녹음 ${-s}초 전"
        else -> "녹음 ${s}초 후"
    }
}

fun coverageLine(snapshot: SignalSnapshot): String {
    val requested = (snapshot.requestedBeforeMillis + snapshot.requestedAfterMillis) / 1000
    val covered = snapshot.coveredMillis / 1000
    return if (covered >= requested) "차량 신호 ${covered}초 확보" else "차량 신호 ${covered}초 확보 (요청 ${requested}초 중)"
}

fun sourcesOf(snapshot: SignalSnapshot): Set<SignalSource> = snapshot.samples.map(SignalSample::source).toSet()

fun SignalSource.label(): String = when (this) {
    SignalSource.PLEOS_SDK -> "Pleos SDK"
    SignalSource.AAOS_CAR_API -> "AAOS"
    SignalSource.SIMULATED -> "합성"
}

fun SymptomTrigger.label(): String = when (this) {
    SymptomTrigger.USER_TAP -> "직접 기록"
    SymptomTrigger.USER_VOICE -> "음성 기록"
    SymptomTrigger.HARD_BRAKE -> "급제동 감지"
    SymptomTrigger.SHARP_STEERING -> "큰 조향 감지"
    SymptomTrigger.TELLTALE_ON -> "경고등 켜짐"
}

fun SymptomStatus.label(): String = when (this) {
    SymptomStatus.RECORDED -> "기록됨"
    SymptomStatus.CONFIRMED -> "확인 완료"
    SymptomStatus.VISIT_PLANNED -> "방문 예정"
    SymptomStatus.RESULT_RECORDED -> "정비 결과 기록됨"
    SymptomStatus.RESOLVED -> "해결됨"
    SymptomStatus.RECURRED -> "재발"
}
