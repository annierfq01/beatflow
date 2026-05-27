package com.beatflow.app.data.repository

import com.beatflow.app.data.local.dao.SessionDao
import com.beatflow.app.data.local.entity.RawRecordEntity
import com.beatflow.app.data.local.entity.SessionEntity
import com.beatflow.app.domain.model.PatientData
import com.beatflow.app.domain.model.RawRecord
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao
) {
    suspend fun createSession(startTime: Long): Long {
        return sessionDao.insertSession(
            SessionEntity(startTime = startTime)
        )
    }

    suspend fun finalizeSession(
        sessionId: Long,
        endTime: Long,
        patient: PatientData
    ) {
        sessionDao.finalizeSession(
            id = sessionId,
            endTime = endTime,
            nombre = patient.nombre,
            apellidos = patient.apellidos,
            edad = patient.edad,
            sexo = patient.sexo
        )
    }

    suspend fun addRecords(sessionId: Long, records: List<RawRecord>) {
        val entities = records.map { record ->
            RawRecordEntity(
                sessionId = sessionId,
                timestamp = record.timestamp,
                hr = record.hr,
                rr = record.rr,
                ecgSignal = record.ecgSignal
            )
        }
        sessionDao.insertRecords(entities)
    }

    suspend fun getSession(sessionId: Long): SessionEntity? {
        return sessionDao.getSession(sessionId)
    }

    suspend fun getRecords(sessionId: Long): List<RawRecord> {
        return sessionDao.getRecordsForSession(sessionId).map { it.toDomain() }
    }
}
