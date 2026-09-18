package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "processing_history")
data class ProcessingHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val presetName: String,
    val originalDurationMs: Long,
    val outputDurationMs: Long,
    val detectedSilenceMs: Long,
    val status: String,
    val outputFilePath: String? = null
)

@Dao
interface ProcessingHistoryDao {
    @Query("SELECT * FROM processing_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ProcessingHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ProcessingHistoryEntity): Long

    @Query("DELETE FROM processing_history")
    suspend fun clearAllHistory()
}
