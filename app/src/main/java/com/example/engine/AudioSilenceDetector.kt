package com.example.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import com.example.model.Preset
import com.example.model.SilenceAction
import com.example.model.SilenceAnalysisResult
import com.example.model.SilenceRegion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class AnalysisProgress(
    val progressPercent: Int,
    val processedMs: Long,
    val totalMs: Long,
    val currentPeaks: FloatArray? = null
)

object AudioSilenceDetector {

    suspend fun analyzeSilence(
        context: Context,
        inputUri: Uri,
        preset: Preset,
        onProgress: (AnalysisProgress) -> Unit
    ): SilenceAnalysisResult = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null

        try {
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
                throw IllegalStateException("No audio track found in selected media file.")
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

            val bufferInfo = MediaCodec.BufferInfo()
            var isEos = false

            // Analysis parameters: 20ms RMS windows
            val frameWindowMs = 20L
            val framesPerWindow = ((sampleRate * frameWindowMs) / 1000L).toInt()
            val samplesPerWindow = framesPerWindow * channelCount

            var windowSumSquares = 0.0
            var windowSampleCount = 0
            var totalAudioFramesProcessed = 0L

            // Detected intervals tracking
            val rawCandidateIntervals = mutableListOf<SilenceRegion>()
            var currentSilenceStartMs: Long? = null

            // Waveform peak downsampling (2400 buckets for fine-grained display)
            val waveformBucketCount = 2400
            val waveformPeaks = FloatArray(waveformBucketCount)

            val thresholdDb = preset.thresholdDb
            val minSilenceMs = (preset.minSilenceDurationSec * 1000.0).toLong()
            val maxSilenceMs = preset.maxSilenceDurationSec?.let { (it * 1000.0).toLong() }

            // Speech protection padding: 60ms lead-in and lead-out padding preserved around speech
            // This prevents cutting the trailing consonant or opening plosive of words
            val speechPaddingMs = 60L

            val inputBufferTimeoutUs = 10000L
            val outputBufferTimeoutUs = 10000L

            var lastReportedPercent = -1

            while (!isEos && coroutineContext.isActive) {
                // Feed input buffers to decoder
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

                // Process output buffers from decoder
                var outIndex = decoder.dequeueOutputBuffer(bufferInfo, outputBufferTimeoutUs)
                while (outIndex >= 0) {
                    val outBuffer = decoder.getOutputBuffer(outIndex)
                    if (outBuffer != null && bufferInfo.size > 0) {
                        outBuffer.position(bufferInfo.offset)
                        outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        outBuffer.order(ByteOrder.LITTLE_ENDIAN)

                        val shortBuffer = outBuffer.asShortBuffer()

                        while (shortBuffer.remaining() >= channelCount) {
                            // Accumulate samples across channels for this frame
                            for (ch in 0 until channelCount) {
                                val sample = shortBuffer.get()
                                val sampleDouble = sample.toDouble()
                                windowSumSquares += sampleDouble * sampleDouble
                                windowSampleCount++
                            }
                            totalAudioFramesProcessed++

                            // When RMS window completes
                            if (windowSampleCount >= samplesPerWindow) {
                                val rms = sqrt(windowSumSquares / windowSampleCount)
                                val dbFs = if (rms > 0.0) {
                                    20.0 * log10(rms / 32768.0)
                                } else {
                                    -100.0
                                }

                                // Frame timestamp calculated from exact frame count: 100% synchronized with processor
                                val frameTimeMs = (totalAudioFramesProcessed * 1000L) / sampleRate

                                // Waveform peak bucket
                                val bucketIdx = if (totalDurationMs > 0) {
                                    ((frameTimeMs * waveformBucketCount) / totalDurationMs).toInt().coerceIn(0, waveformBucketCount - 1)
                                } else 0
                                val normalizedPeak = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
                                if (normalizedPeak > waveformPeaks[bucketIdx]) {
                                    waveformPeaks[bucketIdx] = normalizedPeak
                                }

                                // Silence determination with speech hysteresis:
                                // To enter silence: dBFS must be <= thresholdDb
                                val isSilent = dbFs <= thresholdDb
                                if (isSilent) {
                                    if (currentSilenceStartMs == null) {
                                        currentSilenceStartMs = frameTimeMs
                                    }
                                } else {
                                    if (currentSilenceStartMs != null) {
                                        val start = currentSilenceStartMs!!
                                        val end = frameTimeMs
                                        // Record raw silence interval
                                        if ((end - start) >= minSilenceMs) {
                                            rawCandidateIntervals.add(SilenceRegion(start, end))
                                        }
                                        currentSilenceStartMs = null
                                    }
                                }

                                windowSumSquares = 0.0
                                windowSampleCount = 0
                            }
                        }

                        val currentStreamTimeMs = (totalAudioFramesProcessed * 1000L) / sampleRate
                        if (totalDurationMs > 0) {
                            val currentPercent = ((currentStreamTimeMs * 100) / totalDurationMs).toInt().coerceIn(0, 99)
                            if (currentPercent != lastReportedPercent) {
                                lastReportedPercent = currentPercent
                                onProgress(AnalysisProgress(currentPercent, currentStreamTimeMs, totalDurationMs, waveformPeaks))
                            }
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

            val finalDurationMs = max(totalDurationMs, (totalAudioFramesProcessed * 1000L) / sampleRate)

            // Close trailing silence region if active
            if (currentSilenceStartMs != null) {
                val start = currentSilenceStartMs!!
                val end = finalDurationMs
                if ((end - start) >= minSilenceMs) {
                    rawCandidateIntervals.add(SilenceRegion(start, end))
                }
            }

            // Step 1: Merge close silence intervals (< 120ms apart) to prevent stuttering & repeated syllable cuts
            val mergedIntervals = mutableListOf<SilenceRegion>()
            for (region in rawCandidateIntervals) {
                if (mergedIntervals.isEmpty()) {
                    mergedIntervals.add(region)
                } else {
                    val prev = mergedIntervals.last()
                    val gapMs = region.startMs - prev.endMs
                    if (gapMs < 120L) {
                        // Merge into single continuous interval
                        mergedIntervals[mergedIntervals.size - 1] = SilenceRegion(prev.startMs, max(prev.endMs, region.endMs))
                    } else {
                        mergedIntervals.add(region)
                    }
                }
            }

            // Step 2: Apply Speech Protection Padding (Attack & Release cushions)
            // Silence must protect 60ms after speech ends and 60ms before speech resumes
            // This prevents cutting half-words and preserves natural breath/plosive decays
            val paddedIntervals = mutableListOf<SilenceRegion>()
            for (region in mergedIntervals) {
                // If silence is at the very beginning of the track (startMs < 100ms), don't pad front
                val safeStartMs = if (region.startMs < 100L) region.startMs else region.startMs + speechPaddingMs
                // If silence is at the very end of the track, don't pad back
                val safeEndMs = if (region.endMs >= finalDurationMs - 100L) region.endMs else region.endMs - speechPaddingMs

                // Only keep if the safe cuttable portion meets minimum duration
                if (safeEndMs - safeStartMs >= minSilenceMs) {
                    paddedIntervals.add(SilenceRegion(safeStartMs, safeEndMs))
                }
            }

            // Step 3: Apply optional maxSilenceDurationSec filter
            val filteredIntervals = if (maxSilenceMs != null && maxSilenceMs > 0) {
                paddedIntervals.filter { it.durationMs <= maxSilenceMs }
            } else {
                paddedIntervals
            }

            var totalSilenceMs = 0L
            var estimatedOutputMs = finalDurationMs
            val remainingSilenceMs = (preset.remainingSilenceSec * 1000.0).toLong()

            filteredIntervals.forEach { region ->
                totalSilenceMs += region.durationMs
                when (preset.action) {
                    SilenceAction.TRUNCATE -> {
                        val toRemove = (region.durationMs - remainingSilenceMs).coerceAtLeast(0L)
                        estimatedOutputMs -= toRemove
                    }
                    SilenceAction.REMOVE_COMPLETELY -> {
                        estimatedOutputMs -= region.durationMs
                    }
                    SilenceAction.KEEP_NATURAL -> {
                        val naturalKeep = min(region.durationMs, remainingSilenceMs)
                        val toRemove = (region.durationMs - naturalKeep).coerceAtLeast(0L)
                        estimatedOutputMs -= toRemove
                    }
                }
            }

            estimatedOutputMs = estimatedOutputMs.coerceAtLeast(100L)
            onProgress(AnalysisProgress(100, finalDurationMs, finalDurationMs, waveformPeaks))

            SilenceAnalysisResult(
                totalRegionsCount = filteredIntervals.size,
                totalSilenceDurationMs = totalSilenceMs,
                originalDurationMs = finalDurationMs,
                estimatedOutputDurationMs = estimatedOutputMs,
                regions = filteredIntervals,
                waveformPeaks = waveformPeaks
            )
        } finally {
            try { decoder?.stop() } catch (_: Exception) {}
            try { decoder?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }
    }
}
