package com.beatflow.app.data.local

import android.content.Context
import androidx.room.Room
import com.beatflow.app.data.local.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BeatFlowDatabase {
        return Room.databaseBuilder(
            context,
            BeatFlowDatabase::class.java,
            BeatFlowDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideSessionDao(database: BeatFlowDatabase): SessionDao {
        return database.sessionDao()
    }
}
