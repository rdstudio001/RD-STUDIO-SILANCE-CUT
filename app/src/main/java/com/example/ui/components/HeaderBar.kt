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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RdRedContainer
import com.example.ui.theme.RdRedPrimary
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardSurface
import com.example.ui.theme.StudioDarkCharcoal
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@Composable
fun HeaderBar(
    batchCount: Int,
    onOpenPresets: () -> Unit,
    onOpenBatch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAbout: () -> Unit,
    onLoadDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioDarkCharcoal,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, StudioBorder, RoundedCornerShape(0.dp))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            val isPortraitCompact = maxWidth < 600.dp

            if (isPortraitCompact) {
                // Adaptive 2-Row Mobile Portrait Layout: Zero clipping or text crushing
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Logo, Studio Name, and Demo Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(RdRedPrimary, Color(0xFF9E0B12))
                                        )
                                    )
                                    .border(1.dp, Color(0xFFFF4D5B), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "RD Studio Logo",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "RD STUDIO",
                                        color = RdRedPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(RdRedContainer, RoundedCornerShape(4.dp))
                                            .border(1.dp, RdRedPrimary.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "PRO DUB",
                                            color = Color(0xFFFF9AA2),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = "Auto Silence Remover",
                                    color = StudioTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Load Sample Dubbing Demo Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StudioCardSurface)
                                .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                                .clickable { onLoadDemo() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("load_demo_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = "Load Sample",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Demo Take",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Row 2: Navigation & Utility Icons evenly spaced across screen width
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Batch processing queue
                        IconButton(
                            onClick = onOpenBatch,
                            modifier = Modifier.size(36.dp).testTag("open_batch_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (batchCount > 0) {
                                        Badge(containerColor = RdRedPrimary) {
                                            Text(batchCount.toString(), fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = "Batch Queue",
                                    tint = if (batchCount > 0) RdRedPrimary else StudioTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Presets
                        IconButton(
                            onClick = onOpenPresets,
                            modifier = Modifier.size(36.dp).testTag("open_presets_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Presets",
                                tint = StudioTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // History
                        IconButton(
                            onClick = onOpenHistory,
                            modifier = Modifier.size(36.dp).testTag("open_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = StudioTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Settings
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.size(36.dp).testTag("open_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = StudioTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // About
                        IconButton(
                            onClick = onOpenAbout,
                            modifier = Modifier.size(36.dp).testTag("open_about_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "About RD Studio",
                                tint = StudioTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                // Expanded / Landscape Layout (Single Row)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(RdRedPrimary, Color(0xFF9E0B12))
                                    )
                                )
                                .border(1.dp, Color(0xFFFF4D5B), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "RD Studio Logo",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "RD STUDIO",
                                    color = RdRedPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(RdRedContainer, RoundedCornerShape(4.dp))
                                        .border(1.dp, RdRedPrimary.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRO DUB",
                                        color = Color(0xFFFF9AA2),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = "AUTO SILENCE REMOVER",
                                color = StudioTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            Text(
                                text = "Professional Dialogue Silence Processing",
                                color = StudioTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Action Buttons in Landscape
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(StudioCardSurface)
                                .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
                                .clickable { onLoadDemo() }
                                .padding(horizontal = 10.dp, vertical = 7.dp)
                                .testTag("load_demo_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Science,
                                    contentDescription = "Load Sample",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Dubbing Demo",
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(
                            onClick = onOpenBatch,
                            modifier = Modifier.testTag("open_batch_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (batchCount > 0) {
                                        Badge(containerColor = RdRedPrimary) {
                                            Text(batchCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = "Batch Queue",
                                    tint = if (batchCount > 0) RdRedPrimary else StudioTextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = onOpenPresets,
                            modifier = Modifier.testTag("open_presets_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Presets",
                                tint = StudioTextSecondary
                            )
                        }

                        IconButton(
                            onClick = onOpenHistory,
                            modifier = Modifier.testTag("open_history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = StudioTextSecondary
                            )
                        }

                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("open_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = StudioTextSecondary
                            )
                        }

                        IconButton(
                            onClick = onOpenAbout,
                            modifier = Modifier.testTag("open_about_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "About RD Studio",
                                tint = StudioTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
