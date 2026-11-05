package com.dcnm.carapp.core.model

import kotlinx.serialization.Serializable

/** 정비 결과 기록. 자동 도착만으로 완료 처리하지 않고 사용자가 입력한다. */
@Serializable
data class MaintenanceRecord(
    val id: String,
    val symptomEventId: String?,
    val workType: WorkType,
    val item: String,
    val position: String? = null,
    val workDateMillis: Long,
    val odometerKmAtWork: Double?,
    val shopName: String? = null,
    val memo: String? = null,
    val verification: Verification = Verification.USER_INPUT,
)

@Serializable
enum class WorkType { INSPECTION, ADJUSTMENT, REPLACEMENT, NONE }

@Serializable
enum class Verification { USER_INPUT, RECEIPT_CONFIRMED, SHOP_CONFIRMED }

/**
 * 다음 점검 기준. 거리·기간을 함께 두고 먼저 도달하는 쪽으로 알린다.
 * 기준 출처([sourceNote])를 반드시 남긴다. 모든 차량에 같은 값을 넣지 않는다.
 */
@Serializable
data class MaintenanceRule(
    val item: String,
    val intervalKm: Double?,
    val intervalDays: Int?,
    val sourceNote: String,
)
