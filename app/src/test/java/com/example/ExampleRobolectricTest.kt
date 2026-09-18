package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.SyntheticDubbingSampleGenerator
import com.example.model.Preset
import com.example.model.SilenceAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app_name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RD Studio Auto Silence Remover", appName)
  }

  @Test
  fun `verify RD Studio default preset values`() {
    val defaultPreset = Preset.RD_STUDIO_DEFAULT
    assertEquals(-34.0f, defaultPreset.thresholdDb, 0.01f)
    assertEquals(0.25f, defaultPreset.minSilenceDurationSec, 0.01f)
    assertEquals(0.15f, defaultPreset.remainingSilenceSec, 0.01f)
    assertEquals(SilenceAction.TRUNCATE, defaultPreset.action)
    assertTrue(defaultPreset.isBuiltIn)
  }

  @Test
  fun `verify synthetic dubbing sample audio generator creates valid WAV`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sampleFile = SyntheticDubbingSampleGenerator.createSampleDubbingFile(context)
    assertNotNull(sampleFile)
    assertTrue(sampleFile.exists())
    assertTrue(sampleFile.length() > 44) // Contains valid WAV header + audio payload
  }

  @Test
  fun `verify waveform downsampling preserves transient peak envelope on large files`() {
    // Simulate 1 hour audio with 2400 buckets
    val totalBuckets = 2400
    val peaks = FloatArray(totalBuckets) { 0.05f }
    // Insert a transient consonant spike at 25% of duration
    peaks[600] = 0.95f

    // Downsample whole window to 100 screen bars
    val downsampled = com.example.ui.components.downsamplePeaksForWindow(
      peaks = peaks,
      totalDurationMs = 3600000L,
      windowStartMs = 0L,
      windowEndMs = 3600000L,
      targetBarCount = 100
    )

    assertEquals(100, downsampled.size)
    // The bar corresponding to index 25 (which maps to 25% of the file) must capture the 0.95f transient spike
    assertEquals(0.95f, downsampled[25], 0.01f)
  }

  @Test
  fun `verify waveform downsampling handles zoom window slicing gracefully`() {
    val peaks = FloatArray(100) { (it / 100f) }
    val downsampled = com.example.ui.components.downsamplePeaksForWindow(
      peaks = peaks,
      totalDurationMs = 10000L,
      windowStartMs = 5000L,
      windowEndMs = 10000L,
      targetBarCount = 50
    )

    assertEquals(50, downsampled.size)
    // Starting bar at 50% should have value >= 0.5f
    assertTrue(downsampled[0] >= 0.5f)
  }
}

