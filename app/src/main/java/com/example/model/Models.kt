package com.example.model

import android.net.Uri

enum class SilenceAction(val label: String, val description: String) {
    TRUNCATE("Truncate / Compress Silence", "Audacity-style: keep remaining silence duration, compress silent pause"),
    REMOVE_COMPLETELY("Remove Silence Completely", "Completely remove detected silence"),
    KEEP_NATURAL("Keep Natural Silence", "Process only silence beyond configured amount")
}

data class Preset(
    val id: String,
    val name: String,
    val thresholdDb: Float = -20.0f,
    val minSilenceDurationSec: Float = 0.07f,
    val maxSilenceDurationSec: Float? = null,
    val action: SilenceAction = SilenceAction.TRUNCATE,
    val remainingSilenceSec: Float = 0.20f,
    val isBuiltIn: Boolean = false
) {
    companion object {
        // Built-in presets required by RD Studio specification:
        val RD_STUDIO_DEFAULT = Preset(
            id = "preset_rd_studio_default",
            name = "RD Studio — Dialogue Silence Cut",
            thresholdDb = -34.0f,
            minSilenceDurationSec = 0.25f,
            maxSilenceDurationSec = null,
            action = SilenceAction.TRUNCATE,
            remainingSilenceSec = 0.15f,
            isBuiltIn = true
        )

        val NATURAL_DIALOGUE = Preset(
            id = "preset_natural_dialogue",
            name = "Natural Dialogue",
            thresholdDb = -38.0f,
            minSilenceDurationSec = 0.35f,
            maxSilenceDurationSec = null,
            action = SilenceAction.TRUNCATE,
            remainingSilenceSec = 0.25f,
            isBuiltIn = true
        )

        val STANDARD_DUBBING = Preset(
            id = "preset_standard_dubbing",
            name = "Standard Dubbing",
            thresholdDb = -32.0f,
            minSilenceDurationSec = 0.20f,
            maxSilenceDurationSec = null,
            action = SilenceAction.TRUNCATE,
            remainingSilenceSec = 0.12f,
            isBuiltIn = true
        )

        val AGGRESSIVE_SILENCE_CUT = Preset(
            id = "preset_aggressive_cut",
            name = "Aggressive Silence Cut",
            thresholdDb = -28.0f,
            minSilenceDurationSec = 0.15f,
            maxSilenceDurationSec = null,
            action = SilenceAction.TRUNCATE,
            remainingSilenceSec = 0.05f,
            isBuiltIn = true
        )

        val BUILT_IN_PRESETS = listOf(
            RD_STUDIO_DEFAULT,
            NATURAL_DIALOGUE,
            STANDARD_DUBBING,
            AGGRESSIVE_SILENCE_CUT
        )
    }
}

data class MediaMetadata(
    val fileName: String,
    val fileUri: String,
    val formatName: String,
    val fileSizeBytes: Long,
    val durationMs: Long,
    val sampleRate: Int = 44100,
    val channelCount: Int = 2,
    val audioCodec: String = "PCM",
    val bitDepth: Int = 16,
    val isVideo: Boolean = false
) {
    val formattedDuration: String
        get() = formatDuration(durationMs)

    val formattedSize: String
        get() = formatBytes(fileSizeBytes)
}

data class SilenceRegion(
    val startMs: Long,
    val endMs: Long
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0)
}

data class SilenceAnalysisResult(
    val totalRegionsCount: Int,
    val totalSilenceDurationMs: Long,
    val originalDurationMs: Long,
    val estimatedOutputDurationMs: Long,
    val regions: List<SilenceRegion>,
    val waveformPeaks: FloatArray = FloatArray(0)
) {
    val estimatedSilenceReductionMs: Long
        get() = (originalDurationMs - estimatedOutputDurationMs).coerceAtLeast(0)

    val formattedOriginalDuration: String get() = formatDuration(originalDurationMs)
    val formattedSilenceDuration: String get() = formatDuration(totalSilenceDurationMs)
    val formattedEstimatedOutputDuration: String get() = formatDuration(estimatedOutputDurationMs)
    val formattedSilenceReduction: String get() = formatDuration(estimatedSilenceReductionMs)
}

enum class ExportFormat(val extension: String, val displayName: String, val bitDepth: Int) {
    WAV_16("wav", "WAV (16-bit PCM)", 16),
    WAV_24("wav", "WAV (24-bit PCM)", 24),
    WAV_32_FLOAT("wav", "WAV (32-bit Float)", 32),
    AAC_M4A("m4a", "AAC / M4A (256 kbps)", 16),
    FLAC("flac", "FLAC (Lossless)", 16)
}

data class AudioExportConfig(
    val format: ExportFormat = ExportFormat.WAV_16,
    val sampleRate: Int = 0, // 0 = Keep Original
    val channelMode: String = "Original" // "Original", "Mono", "Stereo"
)

enum class ProcessingStage(val displayName: String) {
    IDLE("Ready"),
    ANALYZING("Scanning silence energy..."),
    TRUNCATING("Truncating silence & crossfading..."),
    ENCODING("Writing audio stream..."),
    VERIFYING("Verifying output integrity..."),
    COMPLETED("Processing Complete"),
    FAILED("Processing Failed"),
    CANCELLED("Cancelled")
}

data class ProcessingProgressState(
    val stage: ProcessingStage = ProcessingStage.IDLE,
    val progressPercent: Int = 0,
    val currentFileName: String = "",
    val elapsedSeconds: Long = 0,
    val estimatedRemainingSeconds: Long = 0,
    val processedDurationMs: Long = 0,
    val totalDurationMs: Long = 0,
    val detectedSilenceMs: Long = 0,
    val outputSizeBytes: Long = 0,
    val outputFilePath: String? = null,
    val errorMessage: String? = null,
    val technicalDetails: String? = null
)

data class BatchMediaItem(
    val id: String,
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val durationMs: Long = 0,
    val status: ProcessingStage = ProcessingStage.IDLE,
    val progressPercent: Int = 0,
    val outputFilePath: String? = null,
    val errorMessage: String? = null
)

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "00:00:00"
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.2f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}
