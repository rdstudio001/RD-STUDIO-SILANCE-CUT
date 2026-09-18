package com.example.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.example.model.AudioExportConfig
import com.example.model.ExportFormat
import com.example.model.Preset
import com.example.model.ProcessingProgressState
import com.example.model.ProcessingStage
import com.example.model.SilenceAction
import com.example.model.SilenceRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

object AudioSilenceProcessor {

    suspend fun processAndTruncateSilence(
        context: Context,
        inputUri: Uri,
        fileName: String,
        silenceRegions: List<SilenceRegion>,
        preset: Preset,
        exportConfig: AudioExportConfig,
        onProgress: (ProcessingProgressState) -> Unit,
        isCancelled: () -> Boolean
    ): File = withContext(Dispatchers.IO) {
        val startTimeMs = System.currentTimeMillis()
        val tempId = UUID.randomUUID().toString().take(8)
        val cleanBaseName = fileName.substringBeforeLast('.')
        val outputFileName = "${cleanBaseName}_silence_removed_${tempId}.wav"
        val outputFile = File(context.cacheDir, outputFileName)

        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var fos: FileOutputStream? = null

        try {
            onProgress(
                ProcessingProgressState(
                    stage = ProcessingStage.ANALYZING,
                    progressPercent = 5,
                    currentFileName = fileName,
                    elapsedSeconds = 0
                )
            )

            extractor.setDataSource(context, inputUri, null)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                throw IllegalStateException("No audio stream found in this video or audio file.")
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: ""
            val sampleRate = audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val durationUs = if (audioFormat.containsKey(MediaFormat.KEY_DURATION)) {
                audioFormat.getLong(MediaFormat.KEY_DURATION)
            } else {
                0L
            }
            val totalDurationMs = durationUs / 1000L

            decoder = MediaCodec.createDecoderByType(mime)
            decoder.configure(audioFormat, null, null, 0)
            decoder.start()

            fos = FileOutputStream(outputFile)
            // Determine export bit depth
            val bitsPerSample = when (exportConfig.format) {
                ExportFormat.WAV_24 -> 24
                ExportFormat.WAV_32_FLOAT -> 32
                else -> 16
            }

            // Write initial placeholder WAV header (will update total bytes at end)
            SyntheticDubbingSampleGenerator.writeWavHeader(
                out = fos,
                totalAudioLen = 0L,
                sampleRate = sampleRate,
                channels = channelCount,
                bitsPerSample = bitsPerSample
            )

            val bufferInfo = MediaCodec.BufferInfo()
            var isEos = false
            var totalFramesRead = 0L
            var totalWrittenAudioBytes = 0L

            val remainingSilenceMs = (preset.remainingSilenceSec * 1000.0).toLong()

            // Pre-index silence regions sorted by start time
            val sortedRegions = silenceRegions.sortedBy { it.startMs }
            var currentRegionIdx = 0

            // Crossfade sample buffer (smooth 5ms boundary)
            val crossfadeSampleCount = ((sampleRate * 0.005) * channelCount).toInt()

            val inputBufferTimeoutUs = 10000L
            val outputBufferTimeoutUs = 10000L

            // 64 KB streaming write buffer to protect RAM and disk efficiency
            val writeBufferSize = 65536
            val writeBuffer = ByteBuffer.allocate(writeBufferSize).order(ByteOrder.LITTLE_ENDIAN)

            onProgress(
                ProcessingProgressState(
                    stage = ProcessingStage.TRUNCATING,
                    progressPercent = 10,
                    currentFileName = fileName,
                    elapsedSeconds = (System.currentTimeMillis() - startTimeMs) / 1000,
                    totalDurationMs = totalDurationMs
                )
            )

            var lastReportTimeMs = System.currentTimeMillis()

            while (!isEos && coroutineContext.isActive) {
                if (isCancelled()) {
                    throw InterruptedException("Processing was cancelled by user.")
                }

                // Feed input decoder buffers
                val inIndex = decoder.dequeueInputBuffer(inputBufferTimeoutUs)
                if (inIndex >= 0) {
                    val inBuffer = decoder.getInputBuffer(inIndex)
                    if (inBuffer != null) {
                        inBuffer.clear()
                        val sampleSize = extractor.readSampleData(inBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEos = true
                        } else {
                            val sampleTime = extractor.sampleTime
                            decoder.queueInputBuffer(inIndex, 0, sampleSize, sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                // Drain decoded PCM buffers
                var outIndex = decoder.dequeueOutputBuffer(bufferInfo, outputBufferTimeoutUs)
                while (outIndex >= 0) {
                    if (isCancelled()) {
                        throw InterruptedException("Processing was cancelled by user.")
                    }

                    val outBuffer = decoder.getOutputBuffer(outIndex)
                    if (outBuffer != null && bufferInfo.size > 0) {
                        outBuffer.position(bufferInfo.offset)
                        outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        outBuffer.order(ByteOrder.LITTLE_ENDIAN)

                        val shortBuffer = outBuffer.asShortBuffer()
                        val frameShorts = channelCount

                        while (shortBuffer.remaining() >= frameShorts) {
                            // Accurate millisecond timestamp from exact cumulative frame count
                            val sampleMs = (totalFramesRead * 1000L) / sampleRate
                            totalFramesRead++

                            // Advance region index if past current region
                            while (currentRegionIdx < sortedRegions.size && sampleMs >= sortedRegions[currentRegionIdx].endMs) {
                                currentRegionIdx++
                            }

                            val inSilenceRegion = if (currentRegionIdx < sortedRegions.size) {
                                val reg = sortedRegions[currentRegionIdx]
                                sampleMs >= reg.startMs && sampleMs < reg.endMs
                            } else {
                                false
                            }

                            var shouldWriteFrame = true

                            if (inSilenceRegion) {
                                val reg = sortedRegions[currentRegionIdx]
                                val offsetInRegionMs = sampleMs - reg.startMs

                                when (preset.action) {
                                    SilenceAction.TRUNCATE -> {
                                        // Keep up to remainingSilenceMs, drop the rest
                                        shouldWriteFrame = (offsetInRegionMs < remainingSilenceMs)
                                    }
                                    SilenceAction.REMOVE_COMPLETELY -> {
                                        shouldWriteFrame = false
                                    }
                                    SilenceAction.KEEP_NATURAL -> {
                                        // Retain natural initial pause
                                        shouldWriteFrame = (offsetInRegionMs < remainingSilenceMs)
                                    }
                                }
                            }

                            if (shouldWriteFrame) {
                                for (ch in 0 until frameShorts) {
                                    val sampleVal = shortBuffer.get()

                                    if (writeBuffer.remaining() < 4) {
                                        fos.write(writeBuffer.array(), 0, writeBuffer.position())
                                        totalWrittenAudioBytes += writeBuffer.position()
                                        writeBuffer.clear()
                                    }

                                    when (bitsPerSample) {
                                        16 -> {
                                            writeBuffer.putShort(sampleVal)
                                        }
                                        24 -> {
                                            // 24-bit PCM: 3 bytes Little Endian
                                            val sample24 = sampleVal.toInt() shl 8
                                            writeBuffer.put((sample24 and 0xFF).toByte())
                                            writeBuffer.put(((sample24 shr 8) and 0xFF).toByte())
                                            writeBuffer.put(((sample24 shr 16) and 0xFF).toByte())
                                        }
                                        32 -> {
                                            // 32-bit Float PCM
                                            val floatSample = (sampleVal.toFloat() / 32768.0f).coerceIn(-1.0f, 1.0f)
                                            writeBuffer.putFloat(floatSample)
                                        }
                                        else -> {
                                            writeBuffer.putShort(sampleVal)
                                        }
                                    }
                                }
                            } else {
                                // Skip frame completely
                                for (ch in 0 until frameShorts) {
                                    shortBuffer.get()
                                }
                            }
                        }

                        // Periodic progress reporting every 150ms
                        val now = System.currentTimeMillis()
                        if (now - lastReportTimeMs > 150) {
                            lastReportTimeMs = now
                            val elapsedSec = (now - startTimeMs) / 1000
                            val currentProgressMs = (totalFramesRead * 1000L) / sampleRate
                            val percent = if (totalDurationMs > 0) {
                                ((currentProgressMs * 85) / totalDurationMs).toInt().coerceIn(10, 95)
                            } else {
                                50
                            }

                            val estimatedTotalSec = if (percent > 0) (elapsedSec * 100) / percent else 0L
                            val remainingSec = (estimatedTotalSec - elapsedSec).coerceAtLeast(0L)

                            onProgress(
                                ProcessingProgressState(
                                    stage = ProcessingStage.TRUNCATING,
                                    progressPercent = percent,
                                    currentFileName = fileName,
                                    elapsedSeconds = elapsedSec,
                                    estimatedRemainingSeconds = remainingSec,
                                    processedDurationMs = currentProgressMs,
                                    totalDurationMs = totalDurationMs,
                                    outputSizeBytes = totalWrittenAudioBytes + writeBuffer.position()
                                )
                            )
                        }
                    }

                    decoder.releaseOutputBuffer(outIndex, false)

                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEos = true
                        break
                    }

                    outIndex = decoder.dequeueOutputBuffer(bufferInfo, 0L)
                }
            }

            // Flush remaining buffer
            if (writeBuffer.position() > 0) {
                fos.write(writeBuffer.array(), 0, writeBuffer.position())
                totalWrittenAudioBytes += writeBuffer.position()
                writeBuffer.clear()
            }
            fos.flush()
            fos.close()
            fos = null

            onProgress(
                ProcessingProgressState(
                    stage = ProcessingStage.VERIFYING,
                    progressPercent = 96,
                    currentFileName = fileName,
                    elapsedSeconds = (System.currentTimeMillis() - startTimeMs) / 1000,
                    totalDurationMs = totalDurationMs,
                    outputSizeBytes = totalWrittenAudioBytes
                )
            )

            // Update WAV Header with actual final audio byte length
            RandomAccessFile(outputFile, "rw").use { raf ->
                val totalDataLen = totalWrittenAudioBytes + 36
                raf.seek(4)
                raf.write(
                    byteArrayOf(
                        (totalDataLen and 0xffL).toByte(),
                        (totalDataLen shr 8 and 0xffL).toByte(),
                        (totalDataLen shr 16 and 0xffL).toByte(),
                        (totalDataLen shr 24 and 0xffL).toByte()
                    )
                )
                raf.seek(40)
                raf.write(
                    byteArrayOf(
                        (totalWrittenAudioBytes and 0xffL).toByte(),
                        (totalWrittenAudioBytes shr 8 and 0xffL).toByte(),
                        (totalWrittenAudioBytes shr 16 and 0xffL).toByte(),
                        (totalWrittenAudioBytes shr 24 and 0xffL).toByte()
                    )
                )
            }

            // Quality verification
            if (!outputFile.exists() || outputFile.length() <= 44) {
                throw IllegalStateException("Generated output file is empty or missing.")
            }

            val totalElapsed = (System.currentTimeMillis() - startTimeMs) / 1000

            onProgress(
                ProcessingProgressState(
                    stage = ProcessingStage.COMPLETED,
                    progressPercent = 100,
                    currentFileName = fileName,
                    elapsedSeconds = totalElapsed,
                    estimatedRemainingSeconds = 0,
                    processedDurationMs = totalDurationMs,
                    totalDurationMs = totalDurationMs,
                    outputSizeBytes = outputFile.length(),
                    outputFilePath = outputFile.absolutePath
                )
            )

            outputFile
        } catch (e: Exception) {
            try { fos?.close() } catch (_: Exception) {}
            if (outputFile.exists()) {
                outputFile.delete()
            }
            throw e
        } finally {
            try { decoder?.stop() } catch (_: Exception) {}
            try { decoder?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }
    }
}
