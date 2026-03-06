package com.omniagent.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.omniagent.app.core.model.AnalysisLog

@Database(
    entities = [AnalysisLog::class],
    version = 1,
    exportSchema = false
)
abstract class OmniAgentDatabase : RoomDatabase() {

    abstract fun analysisLogDao(): AnalysisLogDao

    companion object {
        @Volatile
        private var INSTANCE: OmniAgentDatabase? = null

        fun getDatabase(context: Context): OmniAgentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OmniAgentDatabase::class.java,
                    "omniagent_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
