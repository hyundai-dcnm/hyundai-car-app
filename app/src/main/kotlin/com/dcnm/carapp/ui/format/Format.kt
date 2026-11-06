package com.dcnm.carapp.ui.format

import com.dcnm.carapp.core.model.WorkType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun dateTime(millis: Long, withTime: Boolean = true): String =
    SimpleDateFormat(if (withTime) "M월 d일 HH:mm" else "yyyy.M.d", Locale.KOREA).format(Date(millis))

fun WorkType.label(): String = when (this) {
    WorkType.INSPECTION -> "점검"
    WorkType.ADJUSTMENT -> "조정"
    WorkType.REPLACEMENT -> "교체"
    WorkType.NONE -> "조치 안 함"
}

fun km(value: Double): String = "%,d km".format(Locale.KOREA, value.toLong())
