package com.beatflow.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.beatflow.app.data.local.dao.SessionDao
import com.beatflow.app.data.local.entity.RawRecordEntity
import com.beatflow.app.data.local.entity.SessionEntity

@Database(
    entities = [SessionEntity::class, RawRecordEntity::class],
    version = 2,
    exportSchema = false
)
abstract class BeatFlowDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        const val DATABASE_NAME = "beatflow_db"
    }
}
