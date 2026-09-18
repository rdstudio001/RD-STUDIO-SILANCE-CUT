package com.example.engine

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.MediaMetadata
import java.io.File

object MediaHelper {

    fun extractMetadata(context: Context, uri: Uri): MediaMetadata {
        val contentResolver = context.contentResolver
        var displayName = "audio_track"
        var fileSize: Long = 0

        // Query content resolver for display name and size
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {}

        if (fileSize <= 0) {
            try {
                contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    fileSize = pfd.statSize
                }
            } catch (_: Exception) {}
        }

        val retriever = MediaMetadataRetriever()
        var durationMs: Long = 0
        var sampleRate = 44100
        var channelCount = 2
        var bitDepth = 16
        var mimeType = "audio/wav"
        var isVideo = false

        try {
            retriever.setDataSource(context, uri)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (durStr != null) {
                durationMs = durStr.toLongOrNull() ?: 0L
            }

            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            if (mime != null) {
                mimeType = mime
                if (mime.startsWith("video/")) {
                    isVideo = true
                }
            }

            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            if (hasVideo == "yes") {
                isVideo = true
            }

            val srStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
            if (srStr != null) {
                sampleRate = srStr.toIntOrNull() ?: 44100
            }

            val ccStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)
            // Retrieve track specific metadata via MediaExtractor for precision
        } catch (_: Exception) {
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        // Use MediaExtractor to inspect actual audio track details
        val extractor = MediaExtractor()
        var hasAudio = false
        var audioFormatStr = if (isVideo) "Video (Audio extracted)" else "Audio"
        var audioCodec = "PCM"

        try {
            extractor.setDataSource(context, uri, null)
            val numTracks = extractor.trackCount
            for (i in 0 until numTracks) {
                val format = extractor.getTrackFormat(i)
                val trackMime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (trackMime.startsWith("audio/")) {
                    hasAudio = true
                    audioCodec = trackMime.removePrefix("audio/").uppercase()
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    if (format.containsKey(MediaFormat.KEY_DURATION)) {
                        val trackDurMs = format.getLong(MediaFormat.KEY_DURATION) / 1000L
                        if (trackDurMs > 0) durationMs = trackDurMs
                    }
                    if (format.containsKey("bit-depth")) {
                        bitDepth = format.getInteger("bit-depth")
                    }
                    break
                }
            }
        } catch (_: Exception) {
        } finally {
            try { extractor.release() } catch (_: Exception) {}
        }

        // Detect format extension
        val ext = displayName.substringAfterLast('.', "").uppercase()
        if (ext.isNotEmpty()) {
            audioFormatStr = ext
        }

        return MediaMetadata(
            fileName = displayName,
            fileUri = uri.toString(),
            formatName = audioFormatStr,
            fileSizeBytes = fileSize,
            durationMs = durationMs,
            sampleRate = sampleRate,
            channelCount = channelCount,
            audioCodec = audioCodec,
            bitDepth = bitDepth,
            isVideo = isVideo
        )
    }

    fun verifyAudioTrackExists(context: Context, uri: Uri): Boolean {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val numTracks = extractor.trackCount
            for (i in 0 until numTracks) {
                val format = extractor.getTrackFormat(i)
                val trackMime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (trackMime.startsWith("audio/")) {
                    return true
                }
            }
        } catch (_: Exception) {
            return false
        } finally {
            try { extractor.release() } catch (_: Exception) {}
        }
        return false
    }
}
