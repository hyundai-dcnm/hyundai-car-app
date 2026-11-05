package com.dcnm.carapp.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 증상 사건. snapshotJson은 [com.dcnm.carapp.core.model.SignalSnapshot] 직렬화.
 * 원본은 차량 안에만 둔다. 서버·QR 리포트에는 요약만 내보낸다.
 */
@Entity(tableName = "symptom_events")
data class SymptomEventEntity(
    @PrimaryKey val id: String,
    val createdAtMillis: Long,
    val trigger: String,
    val utterance: String?,
    val utteranceConfirmed: Boolean,
    val snapshotJson: String,
    val status: String,
)

@Entity(tableName = "maintenance_records")
data class MaintenanceRecordEntity(
    @PrimaryKey val id: String,
    val symptomEventId: String?,
    val workType: String,
    val item: String,
    val position: String?,
    val workDateMillis: Long,
    val odometerKmAtWork: Double?,
    val shopName: String?,
    val memo: String?,
    val verification: String,
)
