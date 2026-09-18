package com.example.data

import android.content.Context
import com.example.model.Preset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class StudioRepository(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val presetDao = db.presetDao()
    private val historyDao = db.processingHistoryDao()

    val allPresets: Flow<List<Preset>> = presetDao.getAllPresets().map { list ->
        if (list.isEmpty()) {
            Preset.BUILT_IN_PRESETS
        } else {
            list.map { it.toDomain() }
        }
    }

    val history: Flow<List<ProcessingHistoryEntity>> = historyDao.getAllHistory()

    suspend fun savePreset(preset: Preset) = withContext(Dispatchers.IO) {
        presetDao.insertPreset(PresetEntity.fromDomain(preset))
    }

    suspend fun deletePreset(id: String) = withContext(Dispatchers.IO) {
        presetDao.deleteCustomPresetById(id)
    }

    suspend fun resetDefaultPresets() = withContext(Dispatchers.IO) {
        Preset.BUILT_IN_PRESETS.forEach {
            presetDao.insertPreset(PresetEntity.fromDomain(it))
        }
    }

    suspend fun logHistory(
        fileName: String,
        presetName: String,
        originalDurationMs: Long,
        outputDurationMs: Long,
        detectedSilenceMs: Long,
        status: String,
        outputFilePath: String? = null
    ) = withContext(Dispatchers.IO) {
        historyDao.insertHistory(
            ProcessingHistoryEntity(
                fileName = fileName,
                presetName = presetName,
                originalDurationMs = originalDurationMs,
                outputDurationMs = outputDurationMs,
                detectedSilenceMs = detectedSilenceMs,
                status = status,
                outputFilePath = outputFilePath
            )
        )
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        historyDao.clearAllHistory()
    }
}
