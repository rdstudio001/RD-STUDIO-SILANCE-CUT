package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProcessingProgressState
import com.example.model.ProcessingStage
import com.example.model.formatBytes
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
fun ProcessingProgressCard(
    progressState: ProcessingProgressState,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (progressState.stage == ProcessingStage.IDLE) return

    var isCancelConfirmationOpen by remember { mutableStateOf(false) }

    if (isCancelConfirmationOpen) {
        AlertDialog(
            onDismissRequest = { isCancelConfirmationOpen = false },
            title = { Text("Cancel Processing?", color = StudioTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to cancel silence processing for this file?", color = StudioTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isCancelConfirmationOpen = false
                        onCancelClick()
                    }
                ) {
                    Text("Yes, Cancel", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isCancelConfirmationOpen = false }) {
                    Text("Continue Processing", color = StudioTextSecondary)
                }
            },
            containerColor = StudioCardElevated
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioCardSurface)
            .border(
                1.dp,
                if (progressState.stage == ProcessingStage.COMPLETED) AudioWaveGreen else RdRedPrimary,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
            .testTag("processing_progress_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = if (progressState.stage == ProcessingStage.COMPLETED) AudioWaveGreen else RdRedPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = progressState.stage.displayName,
                        color = StudioTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (progressState.currentFileName.isNotBlank()) {
                        Text(
                            text = progressState.currentFileName,
                            color = StudioTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Cancel Button if active
            if (progressState.stage in listOf(
                    ProcessingStage.ANALYZING,
                    ProcessingStage.TRUNCATING,
                    ProcessingStage.ENCODING,
                    ProcessingStage.VERIFYING
                )
            ) {
                OutlinedButton(
                    onClick = { isCancelConfirmationOpen = true },
                    modifier = Modifier.testTag("cancel_processing_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel", color = Color(0xFFEF4444), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Linear Progress Indicator
        LinearProgressIndicator(
            progress = { progressState.progressPercent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (progressState.stage == ProcessingStage.COMPLETED) AudioWaveGreen else RdRedPrimary,
            trackColor = StudioBorder
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Progress Details Metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${progressState.progressPercent}% Completed",
                color = StudioTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            if (progressState.stage in listOf(ProcessingStage.ANALYZING, ProcessingStage.TRUNCATING)) {
                val elapsedFormatted = formatDuration(progressState.elapsedSeconds * 1000L)
                val remainingFormatted = formatDuration(progressState.estimatedRemainingSeconds * 1000L)
                Text(
                    text = "Elapsed: $elapsedFormatted | ETA: $remainingFormatted",
                    color = StudioTextSecondary,
                    fontSize = 11.sp
                )
            } else if (progressState.stage == ProcessingStage.COMPLETED) {
                Text(
                    text = "Output: ${formatBytes(progressState.outputSizeBytes)}",
                    color = AudioWaveGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Error message view if failed
        if (progressState.stage == ProcessingStage.FAILED && progressState.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x33EF4444), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "Error: ${progressState.errorMessage}",
                    color = Color(0xFFFCA5A5),
                    fontSize = 12.sp
                )
            }
        }
    }
}
