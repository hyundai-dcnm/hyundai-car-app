package com.dcnm.carapp.core.data.repository

import com.dcnm.carapp.core.data.db.MaintenanceRecordDao
import com.dcnm.carapp.core.data.db.MaintenanceRecordEntity
import com.dcnm.carapp.core.model.MaintenanceRecord
import com.dcnm.carapp.core.model.MaintenanceRule
import com.dcnm.carapp.core.model.Verification
import com.dcnm.carapp.core.model.WorkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MaintenanceRepository(private val dao: MaintenanceRecordDao) {

    fun observeAll(): Flow<List<MaintenanceRecord>> = dao.observeAll().map { l -> l.map { it.toModel() } }

    suspend fun record(record: MaintenanceRecord) = dao.upsert(record.toEntity())

    /**
     * 다음 점검 기준 계산. 거리·날짜 중 먼저 오는 쪽. 이력이 없으면 null ("이력 확인 필요").
     * 교체 당시 주행거리가 없으면 날짜 기준만 돌려준다.
     */
    suspend fun nextDue(rule: MaintenanceRule): NextDue? {
        val last = dao.latestFor(rule.item) ?: return null
        val dueKm = rule.intervalKm?.let { interval -> last.odometerKmAtWork?.let { it + interval } }
        val dueDateMillis = rule.intervalDays?.let { last.workDateMillis + it * 86_400_000L }
        return NextDue(rule.item, dueKm, dueDateMillis, rule.sourceNote)
    }

    private fun MaintenanceRecord.toEntity() = MaintenanceRecordEntity(
        id, symptomEventId, workType.name, item, position, workDateMillis, odometerKmAtWork, shopName, memo, verification.name,
    )

    private fun MaintenanceRecordEntity.toModel() = MaintenanceRecord(
        id, symptomEventId, WorkType.valueOf(workType), item, position, workDateMillis, odometerKmAtWork, shopName, memo,
        Verification.valueOf(verification),
    )
}

data class NextDue(val item: String, val dueKm: Double?, val dueDateMillis: Long?, val sourceNote: String)
