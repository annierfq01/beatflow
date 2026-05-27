package com.beatflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.beatflow.app.domain.model.RawRecord

@Entity(tableName = "raw_records")
data class RawRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val hr: Int?,
    val rr: Double?,
    val ecgSignal: Double?
) {
    fun toDomain() = RawRecord(
        timestamp = timestamp,
        hr = hr,
        rr = rr,
        ecgSignal = ecgSignal
    )
}
