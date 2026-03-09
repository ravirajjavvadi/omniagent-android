package com.omniagent.app.data.local

import androidx.room.*
import com.omniagent.app.core.model.AnalysisLog
import com.omniagent.app.core.model.ChatSession
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AnalysisLog): Long

    @Query("SELECT * FROM analysis_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AnalysisLog>>

    @Query("SELECT * FROM analysis_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<AnalysisLog>>

    @Query("SELECT * FROM analysis_logs WHERE classifiedModule = :module ORDER BY timestamp DESC")
    fun getLogsByModule(module: String): Flow<List<AnalysisLog>>

    @Query("SELECT * FROM analysis_logs WHERE userInput LIKE '%' || :query || '%' OR classifiedModule LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchLogs(query: String): Flow<List<AnalysisLog>>

    @Query("SELECT sessionId AS id, sessionTitle AS title, MAX(timestamp) AS lastTimestamp FROM analysis_logs GROUP BY sessionId ORDER BY lastTimestamp DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Query("SELECT * FROM analysis_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getLogsBySession(sessionId: String): Flow<List<AnalysisLog>>

    @Query("UPDATE analysis_logs SET sessionTitle = :newTitle WHERE sessionId = :sessionId")
    suspend fun renameSession(sessionId: String, newTitle: String)

    @Query("DELETE FROM analysis_logs WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("SELECT * FROM analysis_logs WHERE id = :id")
    suspend fun getLogById(id: Long): AnalysisLog?

    @Query("DELETE FROM analysis_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM analysis_logs")
    suspend fun getLogCount(): Int

    @Delete
    suspend fun deleteLog(log: AnalysisLog)
}
