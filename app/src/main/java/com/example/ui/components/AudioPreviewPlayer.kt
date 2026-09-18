package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AudioPreviewManager.PlayerState
import com.example.model.formatDuration
import com.example.ui.theme.AudioWaveGreen
import com.example.ui.theme.RdRedPrimary
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioTextTertiary

@Composable
fun AudioPreviewPlayer(
    playerState: PlayerState,
    hasProcessedAudio: Boolean,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSwitchMode: (Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioCardSurface)
            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("audio_preview_player_card")
    ) {
        // Top Header: Title & A/B Mode Toggle (Original vs Clean Cut)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = if (playerState.isProcessedMode) AudioWaveGreen else RdRedPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "A/B PREVIEW",
                    color = StudioTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Segmented A/B Switcher
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(StudioCardElevated)
                    .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                    .padding(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Original Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (!playerState.isProcessedMode) RdRedPrimary else Color.Transparent)
                        .clickable { onSwitchMode(false) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("preview_original_toggle")
                ) {
                    Text(
                        text = "Original",
                        color = if (!playerState.isProcessedMode) Color.White else StudioTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (!playerState.isProcessedMode) FontWeight.Bold else FontWeight.Normal
                    )
                }

                // Processed (Clean Dub) Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (playerState.isProcessedMode) AudioWaveGreen else Color.Transparent
                        )
                        .clickable(enabled = hasProcessedAudio) { onSwitchMode(true) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("preview_processed_toggle")
                ) {
                    Text(
                        text = if (hasProcessedAudio) "Clean Cut" else "Clean (Pending)",
                        color = if (playerState.isProcessedMode) {
                            Color.Black
                        } else if (hasProcessedAudio) {
                            StudioTextSecondary
                        } else {
                            StudioTextTertiary
                        },
                        fontSize = 11.sp,
                        fontWeight = if (playerState.isProcessedMode) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Seek Slider & Time Readouts
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDuration(playerState.currentPositionMs),
                color = StudioTextPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(48.dp)
            )

            Slider(
                value = if (playerState.durationMs > 0) {
                    playerState.currentPositionMs.toFloat() / playerState.durationMs
                } else 0f,
                onValueChange = { ratio ->
                    val target = (ratio * playerState.durationMs).toLong()
                    onSeekTo(target)
                },
                colors = SliderDefaults.colors(
                    thumbColor = if (playerState.isProcessedMode) AudioWaveGreen else RdRedPrimary,
                    activeTrackColor = if (playerState.isProcessedMode) AudioWaveGreen else RdRedPrimary,
                    inactiveTrackColor = StudioBorder
                ),
                modifier = Modifier.weight(1f).testTag("preview_seek_slider")
            )

            Text(
                text = formatDuration(playerState.durationMs),
                color = StudioTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Adaptive Controls: Split gracefully on Mobile Portrait vs Landscape
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isNarrowScreen = maxWidth < 450.dp

            if (isNarrowScreen) {
                // Portrait Mobile: 2 tidy rows
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Play/Pause, Stop, and Status Label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(19.dp))
                                    .background(if (playerState.isProcessedMode) AudioWaveGreen else RdRedPrimary)
                                    .clickable { onTogglePlayPause() }
                                    .testTag("preview_play_pause_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                    tint = if (playerState.isProcessedMode) Color.Black else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            IconButton(
                                onClick = onStop,
                                modifier = Modifier.size(34.dp).testTag("preview_stop_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop",
                                    tint = StudioTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Text(
                            text = if (playerState.isProcessedMode) "PROCESSED DUB" else "ORIGINAL AUDIO",
                            color = if (playerState.isProcessedMode) AudioWaveGreen else RdRedPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Row 2: Speed Chips on left, Volume on right
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { speed ->
                                val isSpeedSelected = kotlin.math.abs(playerState.playbackSpeed - speed) < 0.05f
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSpeedSelected) StudioCardElevated else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (isSpeedSelected) RdRedPrimary else StudioBorder,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable { onSpeedChange(speed) }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                        .testTag("speed_${speed}x")
                                ) {
                                    Text(
                                        text = "${speed}x",
                                        color = if (isSpeedSelected) RdRedPrimary else StudioTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSpeedSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(110.dp)
                        ) {
                            Icon(
                                imageVector = if (playerState.volume > 0.5f) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                                contentDescription = "Volume",
                                tint = StudioTextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Slider(
                                value = playerState.volume,
                                onValueChange = onVolumeChange,
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = StudioTextPrimary,
                                    activeTrackColor = StudioTextPrimary,
                                    inactiveTrackColor = StudioBorder
                                ),
                                modifier = Modifier.testTag("preview_volume_slider")
                            )
                        }
                    }
                }
            } else {
                // Landscape / Expanded Screen: Single spacious row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(21.dp))
                                .background(if (playerState.isProcessedMode) AudioWaveGreen else RdRedPrimary)
                                .clickable { onTogglePlayPause() }
                                .testTag("preview_play_pause_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                tint = if (playerState.isProcessedMode) Color.Black else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        IconButton(
                            onClick = onStop,
                            modifier = Modifier.size(36.dp).testTag("preview_stop_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = StudioTextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Playback Speed Chips
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { speed ->
                            val isSpeedSelected = kotlin.math.abs(playerState.playbackSpeed - speed) < 0.05f
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSpeedSelected) StudioCardElevated else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSpeedSelected) RdRedPrimary else StudioBorder,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { onSpeedChange(speed) }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                    .testTag("speed_${speed}x")
                            ) {
                                Text(
                                    text = "${speed}x",
                                    color = if (isSpeedSelected) RdRedPrimary else StudioTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSpeedSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Volume Control
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.width(120.dp)
                    ) {
                        Icon(
                            imageVector = if (playerState.volume > 0.5f) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                            contentDescription = "Volume",
                            tint = StudioTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Slider(
                            value = playerState.volume,
                            onValueChange = onVolumeChange,
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = StudioTextPrimary,
                                activeTrackColor = StudioTextPrimary,
                                inactiveTrackColor = StudioBorder
                            ),
                            modifier = Modifier.testTag("preview_volume_slider")
                        )
                    }
                }
            }
        }
    }
}
