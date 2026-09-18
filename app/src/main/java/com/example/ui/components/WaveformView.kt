package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SilenceRegion
import com.example.model.formatDuration
import com.example.ui.theme.AudioSilenceRed
import com.example.ui.theme.AudioSilenceRedSolid
import com.example.ui.theme.AudioWaveBlue
import com.example.ui.theme.AudioWaveGreen
import com.example.ui.theme.RdRedPrimary
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioDarkCharcoal
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioTextTertiary
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Adaptive downsampling algorithm:
 * Downsamples high-resolution waveform peaks into a fixed number of display bars
 * for the visible time window [windowStartMs, windowEndMs].
 * Uses peak-envelope (max absolute peak) downsampling to preserve all transient speech
 * consonants, plosives, and quick dialogue bursts without aliasing or dropping spikes.
 */
internal fun downsamplePeaksForWindow(
    peaks: FloatArray,
    totalDurationMs: Long,
    windowStartMs: Long,
    windowEndMs: Long,
    targetBarCount: Int
): FloatArray {
    if (peaks.isEmpty() || targetBarCount <= 0 || totalDurationMs <= 0) {
        return FloatArray(0)
    }
    val result = FloatArray(targetBarCount)
    val totalBuckets = peaks.size
    val visibleDuration = max(1L, windowEndMs - windowStartMs)

    for (bar in 0 until targetBarCount) {
        val barStartMs = windowStartMs + (bar.toFloat() / targetBarCount * visibleDuration).toLong()
        val barEndMs = windowStartMs + ((bar + 1).toFloat() / targetBarCount * visibleDuration).toLong()

        val startBucket = ((barStartMs.toFloat() / totalDurationMs) * totalBuckets).toInt().coerceIn(0, totalBuckets - 1)
        val endBucket = ((barEndMs.toFloat() / totalDurationMs) * totalBuckets).toInt().coerceIn(0, totalBuckets - 1)

        var maxPeak = 0.035f // Minimum baseline visibility
        val low = min(startBucket, endBucket)
        val high = max(startBucket, endBucket)
        for (b in low..high) {
            val p = peaks[b]
            if (p > maxPeak) maxPeak = p
        }
        result[bar] = maxPeak.coerceIn(0.035f, 1.0f)
    }
    return result
}

@Composable
fun WaveformView(
    waveformPeaks: FloatArray,
    silenceRegions: List<SilenceRegion>,
    durationMs: Long,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    thresholdDb: Float = -20.0f,
    isPlaying: Boolean = false,
    isAnalyzing: Boolean = false,
    analysisProgressPercent: Int = 0,
    modifier: Modifier = Modifier
) {
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var scrollOffsetRatio by remember { mutableFloatStateOf(0.0f) } // 0.0 to 1.0
    var isFollowPlayhead by remember { mutableStateOf(true) }

    // Auto-follow playhead during playback when zoomed in
    LaunchedEffect(currentPositionMs, isPlaying, isFollowPlayhead, zoomLevel) {
        if (isPlaying && isFollowPlayhead && zoomLevel > 1.0f && durationMs > 0) {
            val visibleDuration = (durationMs / zoomLevel).toFloat()
            val maxScrollable = durationMs - visibleDuration
            if (maxScrollable > 0) {
                val targetStart = currentPositionMs - (visibleDuration / 2f)
                val targetRatio = (targetStart / maxScrollable).coerceIn(0f, 1f)
                scrollOffsetRatio = targetRatio
            }
        }
    }

    // Dynamic Level Metering & dBFS Calculation
    val instantaneousPeak = remember(currentPositionMs, waveformPeaks, durationMs, isPlaying) {
        if (durationMs <= 0 || waveformPeaks.isEmpty() || !isPlaying) {
            0.0f
        } else {
            val ratio = (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f)
            val idx = (ratio * (waveformPeaks.size - 1)).toInt()
            waveformPeaks.getOrNull(idx) ?: 0.0f
        }
    }

    val animatedMeterLevel by animateFloatAsState(
        targetValue = if (isPlaying) instantaneousPeak.coerceIn(0.02f, 1.0f) else 0.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "audioMeterAnim"
    )

    val currentDbFs = if (animatedMeterLevel > 0.001f) {
        (20.0 * log10(animatedMeterLevel.toDouble())).toFloat().coerceIn(-60.0f, 0.0f)
    } else {
        -60.0f
    }

    val isCurrentlyInSilence = remember(currentPositionMs, silenceRegions) {
        silenceRegions.any { currentPositionMs in it.startMs..it.endMs }
    }

    // Total silence and dialogue statistics
    val totalSilenceMs = remember(silenceRegions) {
        silenceRegions.sumOf { it.durationMs }
    }
    val silenceRatioPercent = remember(totalSilenceMs, durationMs) {
        if (durationMs > 0) ((totalSilenceMs.toFloat() / durationMs) * 100f).toInt().coerceIn(0, 100) else 0
    }
    val speechRatioPercent = 100 - silenceRatioPercent

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioCardSurface)
            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("waveform_card")
    ) {
        // 1. Header Bar: Title, Live Status & Zoom Navigation Controls
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isNarrow = maxWidth < 480.dp

            if (isNarrow) {
                // Mobile Portrait: 2 clean rows
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = RdRedPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AUDIO WAVEFORM MONITOR",
                                color = StudioTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        if (isAnalyzing) {
                            Text(
                                text = "Scanning... $analysisProgressPercent%",
                                color = AudioWaveBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Zoom Controls row on mobile
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(1.0f, 2.0f, 4.0f, 8.0f).forEach { z ->
                                val isSelected = (zoomLevel == z)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) RdRedPrimary else StudioCardElevated)
                                        .border(1.dp, if (isSelected) RdRedPrimary else StudioBorder, RoundedCornerShape(4.dp))
                                        .clickable {
                                            zoomLevel = z
                                            if (z == 1.0f) scrollOffsetRatio = 0.0f
                                        }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "${z.toInt()}x",
                                        color = if (isSelected) Color.White else StudioTextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    zoomLevel = (zoomLevel / 1.5f).coerceAtLeast(1.0f)
                                    if (zoomLevel == 1.0f) scrollOffsetRatio = 0.0f
                                },
                                modifier = Modifier.size(28.dp).testTag("zoom_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomOut,
                                    contentDescription = "Zoom Out",
                                    tint = StudioTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    zoomLevel = (zoomLevel * 1.5f).coerceAtMost(16.0f)
                                },
                                modifier = Modifier.size(28.dp).testTag("zoom_in_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = "Zoom In",
                                    tint = StudioTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    zoomLevel = 1.0f
                                    scrollOffsetRatio = 0.0f
                                    isFollowPlayhead = true
                                },
                                modifier = Modifier.size(28.dp).testTag("zoom_fit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset Zoom to Fit",
                                    tint = if (zoomLevel > 1.0f) AudioWaveBlue else StudioTextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Landscape / Expanded: Single wide row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = RdRedPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "REAL-TIME AUDIO WAVEFORM",
                                color = StudioTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            if (isAnalyzing) {
                                Text(
                                    text = "Scanning waveform & silence intervals... $analysisProgressPercent%",
                                    color = AudioWaveBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(1.0f, 2.0f, 4.0f, 8.0f).forEach { z ->
                            val isSelected = (zoomLevel == z)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) RdRedPrimary else StudioCardElevated)
                                    .border(1.dp, if (isSelected) RdRedPrimary else StudioBorder, RoundedCornerShape(4.dp))
                                    .clickable {
                                        zoomLevel = z
                                        if (z == 1.0f) scrollOffsetRatio = 0.0f
                                    }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${z.toInt()}x",
                                    color = if (isSelected) Color.White else StudioTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                zoomLevel = (zoomLevel / 1.5f).coerceAtLeast(1.0f)
                                if (zoomLevel == 1.0f) scrollOffsetRatio = 0.0f
                            },
                            modifier = Modifier.size(28.dp).testTag("zoom_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "Zoom Out",
                                tint = StudioTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                zoomLevel = (zoomLevel * 1.5f).coerceAtMost(16.0f)
                            },
                            modifier = Modifier.size(28.dp).testTag("zoom_in_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = "Zoom In",
                                tint = StudioTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                zoomLevel = 1.0f
                                scrollOffsetRatio = 0.0f
                                isFollowPlayhead = true
                            },
                            modifier = Modifier.size(28.dp).testTag("zoom_fit_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Zoom to Fit",
                                tint = if (zoomLevel > 1.0f) AudioWaveBlue else StudioTextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Real-Time VU Meter & Audio Level Status Strip
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(StudioDarkCharcoal)
                .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            val isNarrowMeter = maxWidth < 480.dp

            if (isNarrowMeter) {
                // Mobile Portrait: Status on top, Meter bar + Follow below
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = when {
                                            !isPlaying -> StudioTextTertiary
                                            isCurrentlyInSilence -> AudioSilenceRedSolid
                                            else -> AudioWaveGreen
                                        },
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    !isPlaying -> "PAUSED"
                                    isCurrentlyInSilence -> "SILENCE"
                                    else -> "SPEECH"
                                },
                                color = when {
                                    !isPlaying -> StudioTextTertiary
                                    isCurrentlyInSilence -> AudioSilenceRedSolid
                                    else -> AudioWaveGreen
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${String.format("%.1f", currentDbFs)} dBFS",
                                color = StudioTextPrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Follow Playhead Toggle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isFollowPlayhead) StudioCardElevated else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isFollowPlayhead) AudioWaveBlue else StudioBorder,
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { isFollowPlayhead = !isFollowPlayhead }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isFollowPlayhead) "Follow: ON" else "Follow: OFF",
                                color = if (isFollowPlayhead) AudioWaveBlue else StudioTextTertiary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Meter Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(StudioBlack)
                            .border(0.5.dp, StudioBorder, RoundedCornerShape(3.dp))
                    ) {
                        val normalizedMeter = ((currentDbFs + 60.0f) / 60.0f).coerceIn(0.0f, 1.0f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = normalizedMeter)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            AudioWaveGreen,
                                            StatusWarning,
                                            RdRedPrimary
                                        )
                                    )
                                )
                        )
                    }
                }
            } else {
                // Landscape: Single row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = when {
                                        !isPlaying -> StudioTextTertiary
                                        isCurrentlyInSilence -> AudioSilenceRedSolid
                                        else -> AudioWaveGreen
                                    },
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                !isPlaying -> "PAUSED"
                                isCurrentlyInSilence -> "SILENCE DETECTED"
                                else -> "ACTIVE SPEECH"
                            },
                            color = when {
                                !isPlaying -> StudioTextTertiary
                                isCurrentlyInSilence -> AudioSilenceRedSolid
                                else -> AudioWaveGreen
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${String.format("%.1f", currentDbFs)} dBFS",
                            color = StudioTextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .padding(horizontal = 12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(StudioBlack)
                            .border(0.5.dp, StudioBorder, RoundedCornerShape(3.dp))
                    ) {
                        val normalizedMeter = ((currentDbFs + 60.0f) / 60.0f).coerceIn(0.0f, 1.0f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = normalizedMeter)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            AudioWaveGreen,
                                            StatusWarning,
                                            RdRedPrimary
                                        )
                                    )
                                )
                        )

                        val thresholdNormalized = ((thresholdDb + 60.0f) / 60.0f).coerceIn(0.0f, 1.0f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(2.dp)
                                .align(Alignment.CenterStart)
                                .padding(start = (thresholdNormalized * 100).dp)
                                .background(Color.White)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isFollowPlayhead) StudioCardElevated else Color.Transparent)
                            .border(
                                1.dp,
                                if (isFollowPlayhead) AudioWaveBlue else StudioBorder,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { isFollowPlayhead = !isFollowPlayhead }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isFollowPlayhead) "Follow: ON" else "Follow: OFF",
                            color = if (isFollowPlayhead) AudioWaveBlue else StudioTextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Interactive Main Waveform Canvas with Pinch-to-Zoom and Horizontal Navigation
        val visibleDuration = if (zoomLevel > 1.0f && durationMs > 0) (durationMs / zoomLevel).toFloat() else durationMs.toFloat()
        val windowStartMs = if (zoomLevel > 1.0f && durationMs > 0) {
            (scrollOffsetRatio * (durationMs - visibleDuration)).coerceAtLeast(0f)
        } else 0f
        val windowEndMs = windowStartMs + visibleDuration

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StudioBlack)
                .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                .pointerInput(waveformPeaks, zoomLevel, durationMs, scrollOffsetRatio) {
                    // Tap to seek
                    detectTapGestures { offset ->
                        if (durationMs > 0) {
                            val canvasWidth = size.width
                            val clickedRatio = (offset.x / canvasWidth).coerceIn(0f, 1f)
                            val targetMs = (windowStartMs + clickedRatio * visibleDuration).toLong().coerceIn(0, durationMs)
                            onSeekTo(targetMs)
                        }
                    }
                }
                .pointerInput(zoomLevel) {
                    // Horizontal drag to pan & Pinch to zoom
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (zoom != 1.0f) {
                            zoomLevel = (zoomLevel * zoom).coerceIn(1.0f, 16.0f)
                        }
                        if (zoomLevel > 1.0f && pan.x != 0f) {
                            val dragRatio = -pan.x / size.width
                            scrollOffsetRatio = (scrollOffsetRatio + dragRatio).coerceIn(0.0f, 1.0f)
                            isFollowPlayhead = false
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val midY = canvasHeight / 2f

                // A. Background Grid Lines & Center Line
                drawLine(
                    color = Color(0xFF1E222D),
                    start = Offset(0f, midY),
                    end = Offset(canvasWidth, midY),
                    strokeWidth = 1f
                )

                // Dotted Threshold Reference Line (Amplitude equivalent of thresholdDb)
                val thresholdAmp = (10.0.pow(thresholdDb.toDouble() / 20.0)).toFloat().coerceIn(0.02f, 1.0f)
                val thresholdYOffset = thresholdAmp * (midY * 0.85f)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                // Upper threshold guide line
                drawLine(
                    color = Color(0x66E50914),
                    start = Offset(0f, midY - thresholdYOffset),
                    end = Offset(canvasWidth, midY - thresholdYOffset),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )
                // Lower threshold guide line
                drawLine(
                    color = Color(0x66E50914),
                    start = Offset(0f, midY + thresholdYOffset),
                    end = Offset(canvasWidth, midY + thresholdYOffset),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )

                if (durationMs <= 0 || waveformPeaks.isEmpty()) {
                    return@Canvas
                }

                // B. Visual Distinction for Detected Silent Sections
                silenceRegions.forEach { region ->
                    if (region.endMs >= windowStartMs && region.startMs <= windowEndMs) {
                        val relStart = (region.startMs - windowStartMs).coerceAtLeast(0f)
                        val relEnd = (region.endMs - windowStartMs).coerceAtMost(visibleDuration)

                        val xStart = (relStart / visibleDuration) * canvasWidth
                        val xEnd = (relEnd / visibleDuration) * canvasWidth
                        val boxWidth = max(3f, xEnd - xStart)

                        // 1. Translucent crimson red overlay block
                        drawRect(
                            color = Color(0x35E50914),
                            topLeft = Offset(xStart, 0f),
                            size = Size(boxWidth, canvasHeight)
                        )

                        // 2. Clear boundary demarcation cut lines
                        drawLine(
                            color = Color(0xFFE50914),
                            start = Offset(xStart, 0f),
                            end = Offset(xStart, canvasHeight),
                            strokeWidth = 1.5f
                        )
                        drawLine(
                            color = Color(0xFFE50914),
                            end = Offset(xEnd, canvasHeight),
                            start = Offset(xEnd, 0f),
                            strokeWidth = 1.5f
                        )

                        // 3. Top cut indicator tab if wide enough
                        if (boxWidth > 32f) {
                            drawRoundRect(
                                color = Color(0xFFB91C1C),
                                topLeft = Offset(xStart + 2f, 4f),
                                size = Size(min(boxWidth - 4f, 48f), 12f),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                        }
                    }
                }

                // C. Adaptive Downsampled Waveform Peak Bars
                // Partition screen width into fixed display bars (~200 to 280 bars)
                val targetBarCount = (canvasWidth / 3.5f).toInt().coerceIn(80, 260)
                val downsampledPeaks = downsamplePeaksForWindow(
                    peaks = waveformPeaks,
                    totalDurationMs = durationMs,
                    windowStartMs = windowStartMs.toLong(),
                    windowEndMs = windowEndMs.toLong(),
                    targetBarCount = targetBarCount
                )

                val barSlotWidth = canvasWidth / targetBarCount
                val barStrokeWidth = max(1.5f, barSlotWidth * 0.72f)

                for (i in 0 until targetBarCount) {
                    val peak = downsampledPeaks[i]
                    val barHeight = peak * (midY * 0.90f)
                    val x = (i + 0.5f) * barSlotWidth

                    // Determine if this specific bar falls inside any silence region
                    val barTimeMs = (windowStartMs + (i.toFloat() / targetBarCount * visibleDuration)).toLong()
                    val inSilence = silenceRegions.any { barTimeMs in it.startMs..it.endMs }

                    val barBrush = if (inSilence) {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFFF6B6B),
                                Color(0xFFE50914),
                                Color(0xFFFF6B6B)
                            ),
                            startY = midY - barHeight,
                            endY = midY + barHeight
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF00D2FF),
                                AudioWaveBlue,
                                Color(0xFF00D2FF)
                            ),
                            startY = midY - barHeight,
                            endY = midY + barHeight
                        )
                    }

                    // Draw symmetrical studio waveform bar
                    drawLine(
                        brush = barBrush,
                        start = Offset(x, midY - barHeight),
                        end = Offset(x, midY + barHeight),
                        strokeWidth = barStrokeWidth,
                        cap = StrokeCap.Round
                    )
                }

                // D. Dynamic Playhead Laser Line & Glowing Beacon
                if (currentPositionMs in windowStartMs.toLong()..windowEndMs.toLong()) {
                    val relPlayhead = (currentPositionMs - windowStartMs).toFloat()
                    val playheadX = (relPlayhead / visibleDuration) * canvasWidth

                    // Vertical laser line
                    drawLine(
                        color = Color.White,
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, canvasHeight),
                        strokeWidth = 2f
                    )

                    // Top playhead glowing beacon
                    drawCircle(
                        color = RdRedPrimary,
                        radius = 6f,
                        center = Offset(playheadX, 7f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = Offset(playheadX, 7f)
                    )

                    // Bottom playhead indicator
                    drawCircle(
                        color = RdRedPrimary,
                        radius = 4f,
                        center = Offset(playheadX, canvasHeight - 7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Interactive Mini-Map / Scrubber Navigation Bar (Full File Overview)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRACK OVERVIEW & NAVIGATION",
                    color = StudioTextTertiary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${silenceRegions.size} Silence Cuts (${formatDuration(totalSilenceMs)}) • Voice: $speechRatioPercent%",
                    color = AudioSilenceRedSolid,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Mini-Map Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(StudioDarkCharcoal)
                    .border(1.dp, StudioBorder, RoundedCornerShape(4.dp))
                    .pointerInput(durationMs, zoomLevel) {
                        detectTapGestures { offset ->
                            if (durationMs > 0 && zoomLevel > 1.0f) {
                                val clickedRatio = (offset.x / size.width).coerceIn(0f, 1f)
                                val visibleDuration = durationMs / zoomLevel
                                val maxScrollable = durationMs - visibleDuration
                                if (maxScrollable > 0) {
                                    val targetStartMs = (clickedRatio * durationMs) - (visibleDuration / 2f)
                                    scrollOffsetRatio = (targetStartMs / maxScrollable).coerceIn(0f, 1f)
                                    isFollowPlayhead = false
                                }
                            }
                        }
                    }
                    .pointerInput(durationMs, zoomLevel) {
                        detectDragGestures { _, dragAmount ->
                            if (zoomLevel > 1.0f) {
                                val dragRatio = dragAmount.x / size.width
                                scrollOffsetRatio = (scrollOffsetRatio + dragRatio).coerceIn(0.0f, 1.0f)
                                isFollowPlayhead = false
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val mY = h / 2f

                    // 1. Mini-map downsampled audio peaks (100 bars)
                    if (waveformPeaks.isNotEmpty() && durationMs > 0) {
                        val miniBars = 100
                        val miniPeaks = downsamplePeaksForWindow(
                            peaks = waveformPeaks,
                            totalDurationMs = durationMs,
                            windowStartMs = 0L,
                            windowEndMs = durationMs,
                            targetBarCount = miniBars
                        )
                        val barW = w / miniBars
                        for (b in 0 until miniBars) {
                            val pk = miniPeaks[b]
                            val bH = pk * (mY * 0.85f)
                            val x = (b + 0.5f) * barW

                            val bTimeMs = ((b.toFloat() / miniBars) * durationMs).toLong()
                            val inSil = silenceRegions.any { bTimeMs in it.startMs..it.endMs }

                            drawLine(
                                color = if (inSil) Color(0x88E50914) else Color(0x5538BDF8),
                                start = Offset(x, mY - bH),
                                end = Offset(x, mY + bH),
                                strokeWidth = max(1f, barW * 0.6f)
                            )
                        }
                    }

                    // 2. Mini-map silence regions (crimson ticks)
                    silenceRegions.forEach { reg ->
                        if (durationMs > 0) {
                            val sX = (reg.startMs.toFloat() / durationMs) * w
                            val eX = (reg.endMs.toFloat() / durationMs) * w
                            val rW = max(2f, eX - sX)
                            drawRect(
                                color = Color(0x66E50914),
                                topLeft = Offset(sX, 0f),
                                size = Size(rW, h)
                            )
                        }
                    }

                    // 3. Mini-map playhead indicator
                    if (durationMs > 0) {
                        val phX = (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) * w
                        drawLine(
                            color = Color.White,
                            start = Offset(phX, 0f),
                            end = Offset(phX, h),
                            strokeWidth = 1.5f
                        )
                    }

                    // 4. Highlighted Viewport Rectangle (shows currently visible zoomed window)
                    if (durationMs > 0 && zoomLevel > 1.0f) {
                        val viewLeft = (windowStartMs / durationMs).coerceIn(0f, 1f) * w
                        val viewRight = (windowEndMs / durationMs).coerceIn(0f, 1f) * w
                        val viewW = max(8f, viewRight - viewLeft)

                        // Highlight box
                        drawRoundRect(
                            color = Color(0x3300D2FF),
                            topLeft = Offset(viewLeft, 0f),
                            size = Size(viewW, h),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                        // Accent borders
                        drawLine(
                            color = Color(0xFF00D2FF),
                            start = Offset(viewLeft, 0f),
                            end = Offset(viewLeft, h),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = Color(0xFF00D2FF),
                            start = Offset(viewRight, 0f),
                            end = Offset(viewRight, h),
                            strokeWidth = 2f
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Time Ruler & Position Readout Bar
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isNarrow = maxWidth < 420.dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isNarrow) formatDuration(windowStartMs.toLong()) else "Start: ${formatDuration(windowStartMs.toLong())}",
                    color = StudioTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${formatDuration(currentPositionMs)} / ${formatDuration(durationMs)}",
                    color = StudioTextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (isNarrow) formatDuration(windowEndMs.toLong().coerceAtMost(durationMs)) else "End: ${formatDuration(windowEndMs.toLong().coerceAtMost(durationMs))}",
                    color = StudioTextTertiary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
