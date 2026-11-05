package com.dcnm.carapp.core.voice

/** 한국어 예/아니오 판정. 애매하면 null. */
object YesNoParser {
    private val yes = listOf("예", "네", "응", "어", "그래", "좋아", "해줘", "맞아")
    private val no = listOf("아니", "아뇨", "싫어", "괜찮아", "됐어", "안 해", "안해")

    fun parse(text: String): Boolean? {
        val t = text.trim()
        if (no.any { t.contains(it) }) return false
        if (yes.any { t.contains(it) }) return true
        return null
    }
}
