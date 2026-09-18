package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioExportConfig
import com.example.model.ExportFormat
import com.example.ui.theme.AudioWaveGreen
import com.example.ui.theme.RdRedPrimary
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioTextTertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportPanel(
    exportConfig: AudioExportConfig,
    hasProcessedOutput: Boolean,
    onFormatChange: (ExportFormat) -> Unit,
    onSampleRateChange: (Int) -> Unit,
    onChannelsChange: (String) -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioCardSurface)
            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .testTag("export_panel")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    tint = AudioWaveGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AUDIO EXPORT SETTINGS",
                    color = StudioTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Text(
                text = "Audio Only",
                color = StudioTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Format Selector
        Text(
            text = "Export Format",
            color = StudioTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExportFormat.values().forEach { fmt ->
                val isSelected = exportConfig.format == fmt
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) RdRedPrimary else StudioCardElevated)
                        .border(
                            1.dp,
                            if (isSelected) RdRedPrimary else StudioBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onFormatChange(fmt) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("format_${fmt.extension}")
                ) {
                    Text(
                        text = fmt.displayName,
                        color = if (isSelected) Color.White else StudioTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sample Rate selector (Original, 44100, 48000, 96000)
        Text(
            text = "Sample Rate",
            color = StudioTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0 to "Original", 44100 to "44.1 kHz", 48000 to "48.0 kHz", 96000 to "96.0 kHz").forEach { (sr, label) ->
                val isSelected = exportConfig.sampleRate == sr
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) RdRedPrimary else StudioCardElevated)
                        .border(
                            1.dp,
                            if (isSelected) RdRedPrimary else StudioBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onSampleRateChange(sr) }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else StudioTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Export Action Button
        Button(
            onClick = onExportClick,
            enabled = hasProcessedOutput,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AudioWaveGreen,
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF1E293B),
                disabledContentColor = StudioTextTertiary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("export_audio_button")
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "EXPORT PROCESSED AUDIO",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
