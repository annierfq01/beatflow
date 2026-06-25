package com.beatflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.beatflow.app.data.local.entity.RawRecordEntity
import com.beatflow.app.data.local.entity.SessionEntity

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: Long): SessionEntity?

    @Query("UPDATE sessions SET endTime = :endTime, patientNombre = :nombre, patientApellidos = :apellidos, patientEdad = :edad, patientSexo = :sexo WHERE id = :id")
    suspend fun finalizeSession(id: Long, endTime: Long, nombre: String, apellidos: String, edad: Int, sexo: String)

    @Query("UPDATE sessions SET protocolConfigJson = :json WHERE id = :id")
    suspend fun updateProtocolConfig(id: Long, json: String?)

    @Insert
    suspend fun insertRecords(records: List<RawRecordEntity>)

    @Query("SELECT * FROM raw_records WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getRecordsForSession(sessionId: Long): List<RawRecordEntity>

    @Query("DELETE FROM raw_records WHERE sessionId = :sessionId")
    suspend fun deleteRecordsForSession(sessionId: Long)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)
}
