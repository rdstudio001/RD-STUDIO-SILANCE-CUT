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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Preset
import com.example.model.SilenceAction
import com.example.ui.theme.RdRedPrimary
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioBorderLight
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioTextTertiary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SilenceDetectionControls(
    preset: Preset,
    onThresholdChange: (Float) -> Unit,
    onMinDurationChange: (Float) -> Unit,
    onMaxDurationChange: (Float?) -> Unit,
    onActionChange: (SilenceAction) -> Unit,
    onRemainingSilenceChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogMessage by remember { mutableStateOf<String?>(null) }

    // Action dropdown expansion state
    var isActionDropdownOpen by remember { mutableStateOf(false) }

    // Maximum silence duration toggle state
    val hasMaxDuration = preset.maxSilenceDurationSec != null

    if (infoDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { infoDialogTitle = null },
            title = { Text(infoDialogTitle!!, color = StudioTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(infoDialogMessage!!, color = StudioTextSecondary) },
            confirmButton = {
                TextButton(onClick = { infoDialogTitle = null }) {
                    Text("Got It", color = RdRedPrimary)
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
            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .testTag("silence_detection_panel")
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = RdRedPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SILENCE DETECTION CONTROLS",
                    color = StudioTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Low speech warning badge if threshold is aggressive (> -16 dB)
            if (preset.thresholdDb > -16.0f) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0x33F59E0B), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Warning",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Aggressive Threshold",
                        color = Color(0xFFF59E0B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Threshold Control (-80 dB to 0 dB)
        var thresholdText by remember(preset.thresholdDb) {
            mutableStateOf(String.format("%.2f", preset.thresholdDb))
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Threshold",
                        color = StudioTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = {
                            infoDialogTitle = "Threshold (dB)"
                            infoDialogMessage = "Audio level below this threshold can be considered silence. Lower values (e.g. -30 dB) are less aggressive and preserve whispers. Default: -20.00 dB."
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Help",
                            tint = StudioTextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Numeric input box
                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { str ->
                        thresholdText = str
                        str.toFloatOrNull()?.let { onThresholdChange(it) }
                    },
                    suffix = { Text("dB", color = StudioTextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StudioTextPrimary,
                        unfocusedTextColor = StudioTextPrimary,
                        focusedBorderColor = RdRedPrimary,
                        unfocusedBorderColor = StudioBorder
                    ),
                    modifier = Modifier.width(96.dp).height(46.dp).testTag("threshold_input")
                )
            }

            Slider(
                value = preset.thresholdDb,
                onValueChange = {
                    onThresholdChange(it)
                },
                valueRange = -80f..0f,
                colors = SliderDefaults.colors(
                    thumbColor = RdRedPrimary,
                    activeTrackColor = RdRedPrimary,
                    inactiveTrackColor = StudioBorder
                ),
                modifier = Modifier.testTag("threshold_slider")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Minimum Silence Duration (e.g. 0.07 sec)
        var minDurText by remember(preset.minSilenceDurationSec) {
            mutableStateOf(String.format("%.2f", preset.minSilenceDurationSec))
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Minimum Silence Duration",
                        color = StudioTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = {
                            infoDialogTitle = "Minimum Silence Duration"
                            infoDialogMessage = "Only silent pauses at least this long will be processed. Prevents cutting micro natural breath pauses between syllables. Default: 0.07 sec."
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Help",
                            tint = StudioTextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = minDurText,
                    onValueChange = { str ->
                        minDurText = str
                        str.toFloatOrNull()?.let { onMinDurationChange(it) }
                    },
                    suffix = { Text("sec", color = StudioTextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StudioTextPrimary,
                        unfocusedTextColor = StudioTextPrimary,
                        focusedBorderColor = RdRedPrimary,
                        unfocusedBorderColor = StudioBorder
                    ),
                    modifier = Modifier.width(96.dp).height(46.dp).testTag("min_duration_input")
                )
            }

            Slider(
                value = preset.minSilenceDurationSec,
                onValueChange = {
                    onMinDurationChange(it)
                },
                valueRange = 0.02f..2.0f,
                colors = SliderDefaults.colors(
                    thumbColor = RdRedPrimary,
                    activeTrackColor = RdRedPrimary,
                    inactiveTrackColor = StudioBorder
                ),
                modifier = Modifier.testTag("min_duration_slider")
            )

            // Quick preset chips for Minimum Silence Duration
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                listOf(0.03f, 0.05f, 0.07f, 0.10f, 0.20f, 0.50f, 1.00f).forEach { sec ->
                    DurationChip(
                        label = "${String.format("%.2f", sec)}s",
                        isSelected = kotlin.math.abs(preset.minSilenceDurationSec - sec) < 0.005f,
                        onClick = { onMinDurationChange(sec) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Maximum Silence Duration (Optional)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Limit Maximum Silence",
                    color = StudioTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = {
                        infoDialogTitle = "Maximum Silence Duration"
                        infoDialogMessage = "Optional upper limit. When enabled, silences longer than this value will not be modified, preserving scene transitions or deliberate long dramatic pauses."
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Help",
                        tint = StudioTextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Switch(
                checked = hasMaxDuration,
                onCheckedChange = { checked ->
                    if (checked) onMaxDurationChange(5.0f) else onMaxDurationChange(null)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = RdRedPrimary,
                    uncheckedTrackColor = StudioBorder
                ),
                modifier = Modifier.testTag("max_duration_toggle")
            )
        }

        if (hasMaxDuration) {
            var maxDurText by remember(preset.maxSilenceDurationSec) {
                mutableStateOf(String.format("%.1f", preset.maxSilenceDurationSec ?: 5.0f))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Max Silence Threshold",
                    color = StudioTextSecondary,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = maxDurText,
                    onValueChange = { str ->
                        maxDurText = str
                        str.toFloatOrNull()?.let { onMaxDurationChange(it) }
                    },
                    suffix = { Text("sec", color = StudioTextSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StudioTextPrimary,
                        unfocusedTextColor = StudioTextPrimary,
                        focusedBorderColor = RdRedPrimary,
                        unfocusedBorderColor = StudioBorder
                    ),
                    modifier = Modifier.width(96.dp).height(46.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Action Dropdown
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Silence Action",
                color = StudioTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioCardElevated)
                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .clickable { isActionDropdownOpen = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("action_dropdown")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = preset.action.label,
                            color = StudioTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = preset.action.description,
                            color = StudioTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select Action",
                        tint = StudioTextSecondary
                    )
                }

                DropdownMenu(
                    expanded = isActionDropdownOpen,
                    onDismissRequest = { isActionDropdownOpen = false },
                    modifier = Modifier.background(StudioCardElevated)
                ) {
                    SilenceAction.values().forEach { action ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = action.label,
                                        color = if (preset.action == action) RdRedPrimary else StudioTextPrimary,
                                        fontWeight = if (preset.action == action) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = action.description,
                                        color = StudioTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            onClick = {
                                onActionChange(action)
                                isActionDropdownOpen = false
                            }
                        )
                    }
                }
            }
        }

        // 5. Remaining Silence Control (Visible for Truncate & Keep Natural)
        if (preset.action != SilenceAction.REMOVE_COMPLETELY) {
            Spacer(modifier = Modifier.height(14.dp))

            var remainingText by remember(preset.remainingSilenceSec) {
                mutableStateOf(String.format("%.2f", preset.remainingSilenceSec))
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Remaining Silence",
                            color = StudioTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = {
                                infoDialogTitle = "Remaining Silence"
                                infoDialogMessage = "Amount of silence preserved after truncation. Do not cut every silent region to zero: preserving 0.20 sec natural pause is vital for dubbing and dialogue realism."
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Help",
                                tint = StudioTextTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = remainingText,
                        onValueChange = { str ->
                            remainingText = str
                            str.toFloatOrNull()?.let { onRemainingSilenceChange(it) }
                        },
                        suffix = { Text("sec", color = StudioTextSecondary, fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = StudioTextPrimary,
                            unfocusedTextColor = StudioTextPrimary,
                            focusedBorderColor = RdRedPrimary,
                            unfocusedBorderColor = StudioBorder
                        ),
                        modifier = Modifier.width(96.dp).height(46.dp).testTag("remaining_silence_input")
                    )
                }

                Slider(
                    value = preset.remainingSilenceSec,
                    onValueChange = {
                        onRemainingSilenceChange(it)
                    },
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = RdRedPrimary,
                        activeTrackColor = RdRedPrimary,
                        inactiveTrackColor = StudioBorder
                    ),
                    modifier = Modifier.testTag("remaining_silence_slider")
                )

                // Quick preset chips for Remaining Silence
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    listOf(0.05f, 0.10f, 0.15f, 0.20f, 0.30f, 0.50f).forEach { sec ->
                        DurationChip(
                            label = "${String.format("%.2f", sec)}s",
                            isSelected = kotlin.math.abs(preset.remainingSilenceSec - sec) < 0.005f,
                            onClick = { onRemainingSilenceChange(sec) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) RdRedPrimary else StudioCardElevated)
            .border(
                1.dp,
                if (isSelected) RdRedPrimary else StudioBorder,
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else StudioTextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
