package com.dcnm.carapp.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SymptomEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SymptomEventEntity)

    @Query("SELECT * FROM symptom_events ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<SymptomEventEntity>>

    @Query("SELECT * FROM symptom_events WHERE id = :id")
    suspend fun byId(id: String): SymptomEventEntity?

    @Query("UPDATE symptom_events SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}

@Dao
interface MaintenanceRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MaintenanceRecordEntity)

    @Query("SELECT * FROM maintenance_records ORDER BY workDateMillis DESC")
    fun observeAll(): Flow<List<MaintenanceRecordEntity>>

    @Query("SELECT * FROM maintenance_records WHERE item = :item ORDER BY workDateMillis DESC LIMIT 1")
    suspend fun latestFor(item: String): MaintenanceRecordEntity?
}
