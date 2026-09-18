package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProcessingHistoryEntity
import com.example.data.StudioRepository
import com.example.engine.AudioPreviewManager
import com.example.engine.AudioSilenceDetector
import com.example.engine.AudioSilenceProcessor
import com.example.engine.MediaHelper
import com.example.engine.SyntheticDubbingSampleGenerator
import com.example.model.AudioExportConfig
import com.example.model.BatchMediaItem
import com.example.model.ExportFormat
import com.example.model.MediaMetadata
import com.example.model.Preset
import com.example.model.ProcessingProgressState
import com.example.model.ProcessingStage
import com.example.model.SilenceAction
import com.example.model.SilenceAnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudioRepository(application)
    val previewManager = AudioPreviewManager(application)

    // Current active preset (starts with RD Studio Dialogue Silence Cut)
    private val _currentPreset = MutableStateFlow(Preset.RD_STUDIO_DEFAULT)
    val currentPreset: StateFlow<Preset> = _currentPreset.asStateFlow()

    // All available presets
    private val _presets = MutableStateFlow<List<Preset>>(Preset.BUILT_IN_PRESETS)
    val presets: StateFlow<List<Preset>> = _presets.asStateFlow()

    // Current loaded file metadata
    private val _mediaMetadata = MutableStateFlow<MediaMetadata?>(null)
    val mediaMetadata: StateFlow<MediaMetadata?> = _mediaMetadata.asStateFlow()

    private val _currentFileUri = MutableStateFlow<Uri?>(null)
    val currentFileUri: StateFlow<Uri?> = _currentFileUri.asStateFlow()

    // Analysis results & waveform
    private val _analysisResult = MutableStateFlow<SilenceAnalysisResult?>(null)
    val analysisResult: StateFlow<SilenceAnalysisResult?> = _analysisResult.asStateFlow()

    private val _progressivePeaks = MutableStateFlow<FloatArray?>(null)
    val progressivePeaks: StateFlow<FloatArray?> = _progressivePeaks.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisProgressPercent = MutableStateFlow(0)
    val analysisProgressPercent: StateFlow<Int> = _analysisProgressPercent.asStateFlow()

    // Processing progress
    private val _processingState = MutableStateFlow(ProcessingProgressState())
    val processingState: StateFlow<ProcessingProgressState> = _processingState.asStateFlow()

    // Export configuration
    private val _exportConfig = MutableStateFlow(AudioExportConfig())
    val exportConfig: StateFlow<AudioExportConfig> = _exportConfig.asStateFlow()

    // Output processed file
    private val _processedFile = MutableStateFlow<File?>(null)
    val processedFile: StateFlow<File?> = _processedFile.asStateFlow()

    // Batch processing items
    private val _batchQueue = MutableStateFlow<List<BatchMediaItem>>(emptyList())
    val batchQueue: StateFlow<List<BatchMediaItem>> = _batchQueue.asStateFlow()

    // Processing history
    private val _history = MutableStateFlow<List<ProcessingHistoryEntity>>(emptyList())
    val history: StateFlow<List<ProcessingHistoryEntity>> = _history.asStateFlow()

    // Cancellation control
    @Volatile
    private var isCancelledFlag = false
    private var processingJob: Job? = null

    // UI feedback notification message
    private val _uiToastMessage = MutableStateFlow<String?>(null)
    val uiToastMessage: StateFlow<String?> = _uiToastMessage.asStateFlow()

    init {
        // Collect presets from Room database
        viewModelScope.launch {
            repository.allPresets.collect { list ->
                _presets.value = list
            }
        }
        // Collect history
        viewModelScope.launch {
            repository.history.collect { list ->
                _history.value = list
            }
        }
    }

    fun dismissToast() {
        _uiToastMessage.value = null
    }

    fun showToast(msg: String) {
        _uiToastMessage.value = msg
    }

    fun loadMedia(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val metadata = withContext(Dispatchers.IO) {
                    MediaHelper.extractMetadata(context, uri)
                }

                if (metadata.isVideo && !MediaHelper.verifyAudioTrackExists(context, uri)) {
                    _uiToastMessage.value = "No audio stream found in this video."
                    return@launch
                }

                _currentFileUri.value = uri
                _mediaMetadata.value = metadata
                _analysisResult.value = null
                _processedFile.value = null
                _processingState.value = ProcessingProgressState()

                previewManager.setMediaSources(uri, null)
                _uiToastMessage.value = "Loaded: ${metadata.fileName}"
                analyzeSilence()
            } catch (e: Exception) {
                _uiToastMessage.value = "Failed to load media: ${e.message}"
            }
        }
    }

    fun loadSyntheticDubbingDemo() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val demoFile = withContext(Dispatchers.IO) {
                    SyntheticDubbingSampleGenerator.createSampleDubbingFile(context)
                }
                val demoUri = Uri.fromFile(demoFile)
                loadMedia(demoUri)
                _uiToastMessage.value = "Loaded RD Studio Dubbing Sample"
            } catch (e: Exception) {
                _uiToastMessage.value = "Error creating sample: ${e.message}"
            }
        }
    }

    fun selectPreset(preset: Preset) {
        _currentPreset.value = preset
        _uiToastMessage.value = "Preset applied: ${preset.name}"
    }

    fun updateThreshold(thresholdDb: Float) {
        val clamped = thresholdDb.coerceIn(-80.0f, 0.0f)
        _currentPreset.value = _currentPreset.value.copy(thresholdDb = clamped)
    }

    fun updateMinSilenceDuration(durationSec: Float) {
        val clamped = durationSec.coerceIn(0.01f, 10.0f)
        _currentPreset.value = _currentPreset.value.copy(minSilenceDurationSec = clamped)
    }

    fun updateMaxSilenceDuration(durationSec: Float?) {
        _currentPreset.value = _currentPreset.value.copy(maxSilenceDurationSec = durationSec)
    }

    fun updateAction(action: SilenceAction) {
        _currentPreset.value = _currentPreset.value.copy(action = action)
    }

    fun updateRemainingSilence(durationSec: Float) {
        val clamped = durationSec.coerceIn(0.0f, 5.0f)
        _currentPreset.value = _currentPreset.value.copy(remainingSilenceSec = clamped)
    }

    fun saveCustomPreset(name: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _uiToastMessage.value = "Please enter a valid preset name."
                return@launch
            }
            val newPreset = _currentPreset.value.copy(
                id = "custom_${UUID.randomUUID()}",
                name = name.trim(),
                isBuiltIn = false
            )
            repository.savePreset(newPreset)
            _currentPreset.value = newPreset
            _uiToastMessage.value = "Preset '$name' saved!"
        }
    }

    fun deleteCustomPreset(id: String) {
        viewModelScope.launch {
            repository.deletePreset(id)
            if (_currentPreset.value.id == id) {
                _currentPreset.value = Preset.RD_STUDIO_DEFAULT
            }
            _uiToastMessage.value = "Preset deleted."
        }
    }

    fun resetDefaultPresets() {
        viewModelScope.launch {
            repository.resetDefaultPresets()
            _currentPreset.value = Preset.RD_STUDIO_DEFAULT
            _uiToastMessage.value = "Presets reset to RD Studio defaults."
        }
    }

    fun analyzeSilence() {
        val uri = _currentFileUri.value ?: run {
            _uiToastMessage.value = "Please import an audio or video file first."
            return
        }

        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisProgressPercent.value = 0
            _progressivePeaks.value = null
            try {
                val context = getApplication<Application>()
                val result = AudioSilenceDetector.analyzeSilence(
                    context = context,
                    inputUri = uri,
                    preset = _currentPreset.value,
                    onProgress = { progress ->
                        _analysisProgressPercent.value = progress.progressPercent
                        progress.currentPeaks?.let { peaks ->
                            _progressivePeaks.value = peaks.clone()
                        }
                    }
                )
                _analysisResult.value = result
                _progressivePeaks.value = null
                _uiToastMessage.value = "Detected ${result.totalRegionsCount} silence regions."
            } catch (e: Exception) {
                _uiToastMessage.value = "Analysis failed: ${e.message}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun processSilence() {
        val uri = _currentFileUri.value ?: run {
            _uiToastMessage.value = "Please import an audio or video file first."
            return
        }
        val metadata = _mediaMetadata.value ?: return

        isCancelledFlag = false
        processingJob?.cancel()

        processingJob = viewModelScope.launch {
            try {
                // If not analyzed yet, run analysis first
                var analysis = _analysisResult.value
                if (analysis == null) {
                    _processingState.value = ProcessingProgressState(
                        stage = ProcessingStage.ANALYZING,
                        progressPercent = 5,
                        currentFileName = metadata.fileName
                    )
                    analysis = AudioSilenceDetector.analyzeSilence(
                        context = getApplication(),
                        inputUri = uri,
                        preset = _currentPreset.value,
                        onProgress = { p ->
                            _processingState.value = _processingState.value.copy(
                                progressPercent = (p.progressPercent * 0.15).toInt()
                            )
                        }
                    )
                    _analysisResult.value = analysis
                }

                val outputFile = AudioSilenceProcessor.processAndTruncateSilence(
                    context = getApplication(),
                    inputUri = uri,
                    fileName = metadata.fileName,
                    silenceRegions = analysis.regions,
                    preset = _currentPreset.value,
                    exportConfig = _exportConfig.value,
                    onProgress = { state ->
                        _processingState.value = state
                    },
                    isCancelled = { isCancelledFlag }
                )

                _processedFile.value = outputFile
                previewManager.setMediaSources(uri, outputFile)
                previewManager.switchMode(useProcessed = true)

                // Log to Room history
                repository.logHistory(
                    fileName = metadata.fileName,
                    presetName = _currentPreset.value.name,
                    originalDurationMs = analysis.originalDurationMs,
                    outputDurationMs = analysis.estimatedOutputDurationMs,
                    detectedSilenceMs = analysis.totalSilenceDurationMs,
                    status = "Completed",
                    outputFilePath = outputFile.absolutePath
                )

                _uiToastMessage.value = "Silence truncation completed!"
            } catch (e: InterruptedException) {
                _processingState.value = ProcessingProgressState(
                    stage = ProcessingStage.CANCELLED,
                    errorMessage = "Operation cancelled."
                )
                _uiToastMessage.value = "Processing cancelled."
            } catch (e: Exception) {
                _processingState.value = ProcessingProgressState(
                    stage = ProcessingStage.FAILED,
                    errorMessage = e.message ?: "Unknown media processing error",
                    technicalDetails = e.stackTraceToString()
                )
                _uiToastMessage.value = "Processing failed: ${e.message}"
            }
        }
    }

    fun cancelProcessing() {
        isCancelledFlag = true
        processingJob?.cancel()
        _processingState.value = ProcessingProgressState(stage = ProcessingStage.CANCELLED)
        _uiToastMessage.value = "Processing cancelled."
    }

    fun updateExportFormat(format: ExportFormat) {
        _exportConfig.value = _exportConfig.value.copy(format = format)
    }

    fun updateExportSampleRate(sampleRate: Int) {
        _exportConfig.value = _exportConfig.value.copy(sampleRate = sampleRate)
    }

    fun updateExportChannels(channels: String) {
        _exportConfig.value = _exportConfig.value.copy(channelMode = channels)
    }

    fun exportToDestination(destinationUri: Uri) {
        val processed = _processedFile.value ?: run {
            _uiToastMessage.value = "No processed audio to export. Run silence removal first."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cr = getApplication<Application>().contentResolver
                cr.openOutputStream(destinationUri)?.use { outStream ->
                    FileInputStream(processed).use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
                withContext(Dispatchers.Main) {
                    _uiToastMessage.value = "Exported successfully!"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiToastMessage.value = "Export failed: ${e.message}"
                }
            }
        }
    }

    // Batch operations
    fun addBatchFiles(uris: List<Uri>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val newItems = mutableListOf<BatchMediaItem>()
            withContext(Dispatchers.IO) {
                uris.forEach { uri ->
                    val meta = MediaHelper.extractMetadata(context, uri)
                    newItems.add(
                        BatchMediaItem(
                            id = UUID.randomUUID().toString(),
                            uri = uri,
                            name = meta.fileName,
                            sizeBytes = meta.fileSizeBytes,
                            durationMs = meta.durationMs,
                            status = ProcessingStage.IDLE
                        )
                    )
                }
            }
            _batchQueue.value = _batchQueue.value + newItems
            _uiToastMessage.value = "Added ${newItems.size} files to batch queue."
        }
    }

    fun clearBatch() {
        _batchQueue.value = emptyList()
    }

    fun processAllBatch() {
        val items = _batchQueue.value
        if (items.isEmpty()) {
            _uiToastMessage.value = "Batch queue is empty."
            return
        }

        viewModelScope.launch {
            val context = getApplication<Application>()
            for (item in items) {
                if (item.status == ProcessingStage.COMPLETED) continue

                // Update item status to Analyzing
                updateBatchItemStatus(item.id, ProcessingStage.ANALYZING, 10)
                try {
                    val analysis = AudioSilenceDetector.analyzeSilence(
                        context = context,
                        inputUri = item.uri,
                        preset = _currentPreset.value,
                        onProgress = { p ->
                            updateBatchItemStatus(item.id, ProcessingStage.ANALYZING, (p.progressPercent * 0.3).toInt())
                        }
                    )
                    updateBatchItemStatus(item.id, ProcessingStage.TRUNCATING, 40)

                    val outFile = AudioSilenceProcessor.processAndTruncateSilence(
                        context = context,
                        inputUri = item.uri,
                        fileName = item.name,
                        silenceRegions = analysis.regions,
                        preset = _currentPreset.value,
                        exportConfig = _exportConfig.value,
                        onProgress = { p ->
                            updateBatchItemStatus(item.id, p.stage, p.progressPercent)
                        },
                        isCancelled = { isCancelledFlag }
                    )

                    updateBatchItemStatus(
                        item.id,
                        ProcessingStage.COMPLETED,
                        100,
                        outputFile = outFile.absolutePath
                    )

                    repository.logHistory(
                        fileName = item.name,
                        presetName = _currentPreset.value.name,
                        originalDurationMs = analysis.originalDurationMs,
                        outputDurationMs = analysis.estimatedOutputDurationMs,
                        detectedSilenceMs = analysis.totalSilenceDurationMs,
                        status = "Completed",
                        outputFilePath = outFile.absolutePath
                    )
                } catch (e: Exception) {
                    updateBatchItemStatus(
                        item.id,
                        ProcessingStage.FAILED,
                        0,
                        errorMessage = e.message ?: "Failed"
                    )
                }
            }
            _uiToastMessage.value = "Batch processing finished."
        }
    }

    private fun updateBatchItemStatus(
        id: String,
        stage: ProcessingStage,
        percent: Int,
        outputFile: String? = null,
        errorMessage: String? = null
    ) {
        _batchQueue.value = _batchQueue.value.map { item ->
            if (item.id == id) {
                item.copy(
                    status = stage,
                    progressPercent = percent,
                    outputFilePath = outputFile ?: item.outputFilePath,
                    errorMessage = errorMessage ?: item.errorMessage
                )
            } else item
        }
    }

    override fun onCleared() {
        super.onCleared()
        previewManager.release()
    }
}
