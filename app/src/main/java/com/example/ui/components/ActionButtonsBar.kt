package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.example.model.SilenceAnalysisResult
import com.example.ui.theme.AudioSilenceRedSolid
import com.example.ui.theme.AudioWaveGreen
import com.example.ui.theme.RdRedPrimary
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioTextTertiary

@Composable
fun ActionButtonsBar(
    analysisResult: SilenceAnalysisResult?,
    isAnalyzing: Boolean,
    isProcessing: Boolean,
    hasLoadedMedia: Boolean,
    onAnalyzeClick: () -> Unit,
    onProcessClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioCardSurface)
            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("action_buttons_card")
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isNarrowScreen = maxWidth < 480.dp

            if (isNarrowScreen) {
                // Portrait Mobile: Stacked Action Buttons with full width and clear text
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Primary Truncate Silence Button
                    Button(
                        onClick = onProcessClick,
                        enabled = hasLoadedMedia && !isAnalyzing && !isProcessing,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RdRedPrimary,
                            disabledContainerColor = Color(0xFF3E1215)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("remove_silence_primary_button")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing Audio...", fontSize = 13.sp, color = Color.White)
                        } else {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TRUNCATE SILENCE",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Secondary Scan / Analyze Button
                    OutlinedButton(
                        onClick = onAnalyzeClick,
                        enabled = hasLoadedMedia && !isAnalyzing && !isProcessing,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = StudioTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("analyze_silence_button")
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = RdRedPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning Audio...", fontSize = 12.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.QueryStats,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan & Preview Silence", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                // Landscape / Tablet: Side by side Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onAnalyzeClick,
                        enabled = hasLoadedMedia && !isAnalyzing && !isProcessing,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = StudioTextPrimary
                        ),
                        modifier = Modifier
                            .weight(0.42f)
                            .height(48.dp)
                            .testTag("analyze_silence_button")
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = RdRedPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scanning...", fontSize = 13.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.QueryStats,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze Silence", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = onProcessClick,
                        enabled = hasLoadedMedia && !isAnalyzing && !isProcessing,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RdRedPrimary,
                            disabledContainerColor = Color(0xFF3E1215)
                        ),
                        modifier = Modifier
                            .weight(0.58f)
                            .height(48.dp)
                            .testTag("remove_silence_primary_button")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing Audio...", fontSize = 13.sp, color = Color.White)
                        } else {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TRUNCATE SILENCE",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        // Analysis Breakdown Card (Adaptive 2x2 on portrait mobile, 4-row on wide screens)
        if (analysisResult != null) {
            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioCardElevated)
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ANALYSIS BREAKDOWN",
                        color = StudioTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = "${analysisResult.totalRegionsCount} Silent Regions",
                        color = AudioSilenceRedSolid,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isMobile = maxWidth < 480.dp

                    if (isMobile) {
                        // 2x2 Grid for Mobile: generous space, zero text squishing
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    StatMetric(label = "Original Duration", value = analysisResult.formattedOriginalDuration)
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    StatMetric(label = "Detected Silence", value = analysisResult.formattedSilenceDuration, valueColor = AudioSilenceRedSolid)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    StatMetric(label = "Estimated Output", value = analysisResult.formattedEstimatedOutputDuration)
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    StatMetric(label = "Time Saved", value = analysisResult.formattedSilenceReduction, valueColor = AudioWaveGreen)
                                }
                            }
                        }
                    } else {
                        // 4 Columns for Landscape / Wide screens
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatMetric(label = "Original Duration", value = analysisResult.formattedOriginalDuration)
                            StatMetric(label = "Detected Silence", value = analysisResult.formattedSilenceDuration, valueColor = AudioSilenceRedSolid)
                            StatMetric(label = "Estimated Output", value = analysisResult.formattedEstimatedOutputDuration)
                            StatMetric(label = "Time Saved", value = analysisResult.formattedSilenceReduction, valueColor = AudioWaveGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetric(
    label: String,
    value: String,
    valueColor: Color = StudioTextPrimary
) {
    Column {
        Text(
            text = label,
            color = StudioTextTertiary,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
