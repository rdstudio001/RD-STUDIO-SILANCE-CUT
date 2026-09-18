package com.example.engine

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

object SyntheticDubbingSampleGenerator {

    /**
     * Generates a realistic dubbing voice-over WAV file with dialogue speech segments
     * and silence pauses.
     * Total duration ~ 12 seconds with multiple distinct dialogue phrases and silent pauses.
     */
    fun createSampleDubbingFile(context: Context): File {
        val sampleFile = File(context.cacheDir, "rd_studio_sample_dubbing.wav")
        if (sampleFile.exists() && sampleFile.length() > 1000) {
            return sampleFile
        }

        val sampleRate = 44100
        val channels = 2
        val bitsPerSample = 16

        // Script structure:
        // [0.0 - 0.4s] Initial silence (0.4s)
        // [0.4 - 2.2s] Dialogue phrase 1: "RD Studio Voice-over Take One"
        // [2.2 - 3.8s] Long Pause (1.6s) -> will be detected as silence
        // [3.8 - 5.5s] Dialogue phrase 2: "Dialogue line anime dubbing sequence"
        // [5.5 - 7.0s] Silence pause (1.5s) -> will be detected as silence
        // [7.0 - 8.8s] Dialogue phrase 3: "Dubbing artist recording clean take"
        // [8.8 - 10.2s] Silence pause (1.4s) -> will be detected as silence
        // [10.2 - 12.0s] Dialogue phrase 4: "Final line. Cut and process."
        // [12.0 - 12.5s] Tail silence (0.5s)
        val totalDurationSec = 12.5
        val totalFrames = (sampleRate * totalDurationSec).toInt()
        val bytesPerFrame = channels * (bitsPerSample / 8)
        val totalAudioBytes = totalFrames * bytesPerFrame

        FileOutputStream(sampleFile).use { fos ->
            writeWavHeader(fos, totalAudioBytes.toLong(), sampleRate, channels, bitsPerSample)

            val bufferSize = 4096
            val byteBuffer = ByteBuffer.allocate(bufferSize).order(ByteOrder.LITTLE_ENDIAN)

            var frameIndex = 0
            while (frameIndex < totalFrames) {
                byteBuffer.clear()
                while (byteBuffer.remaining() >= bytesPerFrame && frameIndex < totalFrames) {
                    val timeSec = frameIndex.toDouble() / sampleRate
                    val isDialogue = isDialogueSegment(timeSec)

                    val sampleVal: Short = if (isDialogue) {
                        // Multi-harmonic synthesized vocal formant (vocal chord simulation)
                        val fundamental = 160.0 // Male/neutral voice pitch
                        val f1 = 500.0 // Formant 1
                        val f2 = 1500.0 // Formant 2
                        val amp = 14000.0 // Comfortable speech level (-7 dBFS)

                        // Amplitude modulation (syllable envelope)
                        val syllableMod = (0.7 + 0.3 * sin(2.0 * Math.PI * 4.0 * timeSec))
                        val wave = (
                            0.5 * sin(2.0 * Math.PI * fundamental * timeSec) +
                            0.3 * sin(2.0 * Math.PI * f1 * timeSec) +
                            0.2 * sin(2.0 * Math.PI * f2 * timeSec)
                        ) * amp * syllableMod

                        wave.toInt().coerceIn(-32767, 32767).toShort()
                    } else {
                        // Ambient studio floor noise (-55 dBFS, well below -20 dB threshold)
                        val noise = (Math.random() * 80.0 - 40.0).toInt().toShort()
                        noise
                    }

                    // Stereo: Left and Right channels
                    byteBuffer.putShort(sampleVal)
                    byteBuffer.putShort(sampleVal)
                    frameIndex++
                }
                fos.write(byteBuffer.array(), 0, byteBuffer.position())
            }
        }

        return sampleFile
    }

    private fun isDialogueSegment(timeSec: Double): Boolean {
        return (timeSec in 0.4..2.2) ||
               (timeSec in 3.8..5.5) ||
               (timeSec in 7.0..8.8) ||
               (timeSec in 10.2..12.0)
    }

    fun writeWavHeader(
        out: FileOutputStream,
        totalAudioLen: Long,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte() // RIFF/WAVE header
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xffL).toByte()
        header[5] = (totalDataLen shr 8 and 0xffL).toByte()
        header[6] = (totalDataLen shr 16 and 0xffL).toByte()
        header[7] = (totalDataLen shr 24 and 0xffL).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // 'fmt ' chunk
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte()
        header[27] = (sampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xffL).toByte()
        header[29] = (byteRate shr 8 and 0xffL).toByte()
        header[30] = (byteRate shr 16 and 0xffL).toByte()
        header[31] = (byteRate shr 24 and 0xffL).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte() // block align
        header[33] = 0
        header[34] = bitsPerSample.toByte() // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xffL).toByte()
        header[41] = (totalAudioLen shr 8 and 0xffL).toByte()
        header[42] = (totalAudioLen shr 16 and 0xffL).toByte()
        header[43] = (totalAudioLen shr 24 and 0xffL).toByte()

        out.write(header, 0, 44)
    }
}
