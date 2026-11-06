package com.dcnm.carapp.ui.report

import com.dcnm.carapp.core.model.MaintenanceRecord
import com.dcnm.carapp.core.model.SymptomEvent
import com.dcnm.carapp.ui.format.label
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlin.math.roundToInt

/**
 * 정비사 휴대폰용 웹 리포트 링크. 요약 JSON을 raw deflate + base64url로 접어 URL 조각(#r=)에 담는다.
 * 조각은 브라우저가 서버로 보내지 않으므로 페이지 호스팅만 있으면 되고 기록은 차 밖에 저장되지 않는다.
 *
 * 형식은 web/mechanic/index.html의 decode와 짝이다. 키를 바꾸면 [VERSION]을 올린다.
 * 원본 샘플은 담지 않고 1초 간격으로 줄인 값만 담는다 (QR 밀도 제한).
 */
object MechanicLink {
    const val VERSION = 1
    const val FROM_SEC = -30
    const val TO_SEC = 10

    private val laneKeys = listOf("speed", "brake", "steering")

    fun url(baseUrl: String, payload: JsonObject): String = "$baseUrl#r=${encode(payload.toString())}"

    fun payload(event: SymptomEvent, sameTriggerCount: Int, latestMaintenance: MaintenanceRecord?): JsonObject = buildJsonObject {
        val snap = event.snapshot
        put("v", VERSION)
        put("id", event.id.take(8))
        put("at", event.createdAtMillis)
        put("trig", event.trigger.label())
        event.utterance?.let { put("said", it) }
        put("cov", snap.coveredMillis / 1000)
        put("req", (snap.requestedBeforeMillis + snap.requestedAfterMillis) / 1000)
        put("from", FROM_SEC)
        putJsonObject("lanes") { laneKeys.forEach { put(it, bins(event, it)) } }
        lastValueAtOrBefore(event, "gear")?.let { put("gear", it) }
        lastValueAtOrBefore(event, "odometer")?.toDoubleOrNull()?.let { put("odo", it.toInt()) } // 계기판처럼 버림
        putJsonArray("tt") {
            snap.samples.filter { it.type.startsWith("telltale:") && it.value == "true" }
                .groupBy { it.type.removePrefix("telltale:") }
                .forEach { (id, on) ->
                    add(buildJsonObject {
                        put("id", id)
                        put("s", ((on.minOf { it.epochMillis } - snap.anchorMillis) / 1000.0).roundToInt())
                    })
                }
        }
        putJsonArray("obs") { observations(snap).forEach { add(it) } }
        putJsonArray("src") { sourcesOf(snap).forEach { add(it.name) } }
        put("same", sameTriggerCount)
        latestMaintenance?.let { r ->
            putJsonObject("maint") {
                put("item", r.item)
                put("work", r.workType.label())
                put("date", SimpleDateFormat("yyyy.M.d", Locale.KOREA).format(Date(r.workDateMillis)))
                r.odometerKmAtWork?.let { put("km", it.roundToInt()) }
            }
        }
    }

    /** [FROM_SEC]..[TO_SEC] 1초 칸의 평균. 샘플이 없는 칸은 null(공백)로 남기고 보간하지 않는다. */
    internal fun bins(event: SymptomEvent, type: String): JsonArray {
        val anchor = event.snapshot.anchorMillis
        val bySecond = event.snapshot.samples
            .filter { it.type == type }
            .mapNotNull { s -> s.value.toFloatOrNull()?.let { ((s.epochMillis - anchor) / 1000.0).roundToInt() to it } }
            .groupBy({ it.first }, { it.second })
        return buildJsonArray {
            for (sec in FROM_SEC..TO_SEC) {
                val values = bySecond[sec]
                add(if (values == null) JsonNull else JsonPrimitive(values.average().roundToInt()))
            }
        }
    }

    private fun lastValueAtOrBefore(event: SymptomEvent, type: String): String? =
        event.snapshot.samples.filter { it.type == type && it.epochMillis <= event.snapshot.anchorMillis }
            .maxByOrNull { it.epochMillis }?.value
            ?: event.snapshot.samples.filter { it.type == type }.minByOrNull { it.epochMillis }?.value

    fun encode(json: String): String {
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        deflater.setInput(json.toByteArray(Charsets.UTF_8))
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!deflater.finished()) out.write(buf, 0, deflater.deflate(buf))
        deflater.end()
        return Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray())
    }

    fun decode(encoded: String): String {
        val inflater = Inflater(true)
        inflater.setInput(Base64.getUrlDecoder().decode(encoded))
        val out = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!inflater.finished()) {
            val n = inflater.inflate(buf)
            if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
            out.write(buf, 0, n)
        }
        inflater.end()
        return out.toString(Charsets.UTF_8.name())
    }
}
