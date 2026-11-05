package com.dcnm.carapp.core.data.repository

import com.dcnm.carapp.core.data.db.SymptomEventDao
import com.dcnm.carapp.core.data.db.SymptomEventEntity
import com.dcnm.carapp.core.model.SignalSnapshot
import com.dcnm.carapp.core.model.SymptomEvent
import com.dcnm.carapp.core.model.SymptomStatus
import com.dcnm.carapp.core.model.SymptomTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class SymptomRepository(
    private val dao: SymptomEventDao,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun observeAll(): Flow<List<SymptomEvent>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun record(event: SymptomEvent) = dao.upsert(event.toEntity())

    suspend fun updateStatus(id: String, status: SymptomStatus) = dao.updateStatus(id, status.name)

    private fun SymptomEvent.toEntity() = SymptomEventEntity(
        id = id,
        createdAtMillis = createdAtMillis,
        trigger = trigger.name,
        utterance = utterance,
        utteranceConfirmed = utteranceConfirmed,
        snapshotJson = json.encodeToString(SignalSnapshot.serializer(), snapshot),
        status = status.name,
    )

    private fun SymptomEventEntity.toModel() = SymptomEvent(
        id = id,
        createdAtMillis = createdAtMillis,
        trigger = SymptomTrigger.valueOf(trigger),
        utterance = utterance,
        utteranceConfirmed = utteranceConfirmed,
        snapshot = json.decodeFromString(SignalSnapshot.serializer(), snapshotJson),
        status = SymptomStatus.valueOf(status),
    )
}
