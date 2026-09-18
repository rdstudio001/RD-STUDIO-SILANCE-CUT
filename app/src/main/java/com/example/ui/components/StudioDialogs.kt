package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ProcessingHistoryEntity
import com.example.model.BatchMediaItem
import com.example.model.ProcessingStage
import com.example.model.formatBytes
import com.example.model.formatDuration
import com.example.ui.theme.AudioSilenceRedSolid
import com.example.ui.theme.AudioWaveGreen
import com.example.ui.theme.RdRedPrimary
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioTextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(RdRedPrimary, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("RD", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("RD STUDIO", color = StudioTextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("Auto Silence Remover", color = StudioTextSecondary, fontSize = 12.sp)
                }
            }
        },
        text = {
            Column {
                Text(
                    text = "Professional desktop-grade silence truncation and removal engine engineered specifically for dubbing, anime, and voice-over studios.",
                    color = StudioTextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioCardElevated, RoundedCornerShape(8.dp))
                        .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Owner:", color = StudioTextSecondary, fontSize = 12.sp)
                            Text("Shahneel Khan", color = StudioTextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Website:", color = StudioTextSecondary, fontSize = 12.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://rdstudio.online"))
                                    context.startActivity(intent)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = RdRedPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "rdstudio.online",
                                    color = RdRedPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    textDecoration = TextDecoration.Underline
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Focus:", color = StudioTextSecondary, fontSize = 12.sp)
                            Text("Urdu & Hindi Dubbing Studio", color = StudioTextPrimary, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Version:", color = StudioTextSecondary, fontSize = 12.sp)
                            Text("1.0.0 Pro Edition", color = StudioTextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = RdRedPrimary)
            ) {
                Text("Close", color = Color.White)
            }
        },
        containerColor = StudioCardSurface
    )
}

@Composable
fun BatchQueueDialog(
    queue: List<BatchMediaItem>,
    onAddFiles: () -> Unit,
    onProcessAll: () -> Unit,
    onClearBatch: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Batch Processing Queue (${queue.size})",
                    color = StudioTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (queue.isNotEmpty()) {
                    TextButton(onClick = onClearBatch) {
                        Text("Clear", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                if (queue.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No media files in queue.\nClick 'Add Files' to queue multi-file dubbing sessions.",
                            color = StudioTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(queue) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StudioCardElevated, RoundedCornerShape(8.dp))
                                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.name,
                                        color = StudioTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    StatusBadge(item.status)
                                }

                                if (item.status == ProcessingStage.TRUNCATING || item.status == ProcessingStage.ANALYZING) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { item.progressPercent / 100f },
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = RdRedPrimary,
                                        trackColor = StudioBorder
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onAddFiles,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Files", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onProcessAll,
                        enabled = queue.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = RdRedPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Process All", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = StudioTextPrimary)
            }
        },
        containerColor = StudioCardSurface
    )
}

@Composable
fun HistoryDialog(
    history: List<ProcessingHistoryEntity>,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Processing History (${history.size})", color = StudioTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) {
                        Text("Clear All", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                if (history.isEmpty()) {
                    Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                        Text("No completed jobs yet.", color = StudioTextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(history) { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(StudioCardElevated, RoundedCornerShape(8.dp))
                                    .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.fileName,
                                        color = StudioTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = dateFormat.format(Date(item.timestamp)),
                                        color = StudioTextTertiary,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Preset: ${item.presetName}",
                                        color = StudioTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "Cut: ${formatDuration(item.detectedSilenceMs)}",
                                        color = AudioSilenceRedSolid,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = StudioTextPrimary)
            }
        },
        containerColor = StudioCardSurface
    )
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    var confirmOverwrite by remember { mutableStateOf(true) }
    var autoCleanTemp by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Studio Engine Settings", color = StudioTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Confirm Before Overwrite", color = StudioTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Prompt before replacing existing exported files", color = StudioTextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = confirmOverwrite,
                        onCheckedChange = { confirmOverwrite = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RdRedPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Clean Temporary WAVs", color = StudioTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Purge internal audio cache on app restart", color = StudioTextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = autoCleanTemp,
                        onCheckedChange = { autoCleanTemp = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RdRedPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StudioCardElevated, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("AUDIO ENGINE SPECS", color = StudioTextTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Chunk buffer size: 64 KB streaming ring", color = StudioTextSecondary, fontSize = 11.sp)
                        Text("Boundary crossfade: 5 ms cosine ramp", color = StudioTextSecondary, fontSize = 11.sp)
                        Text("Max memory allocation: Fixed minimal RAM", color = StudioTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = RdRedPrimary)) {
                Text("Done", color = Color.White)
            }
        },
        containerColor = StudioCardSurface
    )
}

@Composable
private fun StatusBadge(status: ProcessingStage) {
    val (color, text) = when (status) {
        ProcessingStage.COMPLETED -> AudioWaveGreen to "Done"
        ProcessingStage.TRUNCATING, ProcessingStage.ANALYZING -> Color(0xFF38BDF8) to "Processing"
        ProcessingStage.FAILED -> Color(0xFFEF4444) to "Failed"
        else -> StudioTextSecondary to "Pending"
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
