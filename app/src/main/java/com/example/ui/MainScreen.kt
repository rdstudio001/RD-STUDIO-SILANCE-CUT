package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ProcessingStage
import com.example.ui.components.AboutDialog
import com.example.ui.components.ActionButtonsBar
import com.example.ui.components.AudioPreviewPlayer
import com.example.ui.components.BatchQueueDialog
import com.example.ui.components.ExportPanel
import com.example.ui.components.FileImportCard
import com.example.ui.components.HeaderBar
import com.example.ui.components.HistoryDialog
import com.example.ui.components.PresetSelectorBar
import com.example.ui.components.ProcessingProgressCard
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SilenceDetectionControls
import com.example.ui.components.WaveformView
import com.example.ui.theme.RdRedPrimary
import com.example.ui.theme.StudioBlack
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioTextTertiary

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentPreset by viewModel.currentPreset.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val metadata by viewModel.mediaMetadata.collectAsStateWithLifecycle()
    val analysisResult by viewModel.analysisResult.collectAsStateWithLifecycle()
    val progressivePeaks by viewModel.progressivePeaks.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val analysisProgressPercent by viewModel.analysisProgressPercent.collectAsStateWithLifecycle()
    val processingState by viewModel.processingState.collectAsStateWithLifecycle()
    val exportConfig by viewModel.exportConfig.collectAsStateWithLifecycle()
    val processedFile by viewModel.processedFile.collectAsStateWithLifecycle()
    val batchQueue by viewModel.batchQueue.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val playerState by viewModel.previewManager.state.collectAsStateWithLifecycle()
    val toastMessage by viewModel.uiToastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissToast()
        }
    }

    // Dialog visibility states
    var showAboutDialog by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // File Pickers
    val singleFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadMedia(it) }
    }

    val batchFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addBatchFiles(uris)
        }
    }

    val exportFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/wav")
    ) { destinationUri: Uri? ->
        destinationUri?.let { viewModel.exportToDestination(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HeaderBar(
                batchCount = batchQueue.size,
                onOpenPresets = { /* Presets are already in the dashboard bar */ },
                onOpenBatch = { showBatchDialog = true },
                onOpenSettings = { showSettingsDialog = true },
                onOpenHistory = { showHistoryDialog = true },
                onOpenAbout = { showAboutDialog = true },
                onLoadDemo = { viewModel.loadSyntheticDubbingDemo() }
            )
        },
        containerColor = StudioBlack,
        modifier = modifier.fillMaxSize().testTag("main_screen")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StudioBlack),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1000.dp) // Optimized for both tablet and phone screens
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. File Import / Media Info Card
                item {
                    FileImportCard(
                        metadata = metadata,
                        onBrowseFiles = {
                            singleFilePicker.launch("*/*")
                        }
                    )
                }

                // 2. Waveform View (peaks and detected silence regions)
                item {
                    val peaks = analysisResult?.waveformPeaks ?: progressivePeaks ?: FloatArray(0)
                    val regions = analysisResult?.regions ?: emptyList()
                    val durationMs = metadata?.durationMs ?: 0L

                    WaveformView(
                        waveformPeaks = peaks,
                        silenceRegions = regions,
                        durationMs = durationMs,
                        currentPositionMs = playerState.currentPositionMs,
                        onSeekTo = { targetMs ->
                            viewModel.previewManager.seekTo(targetMs)
                        },
                        thresholdDb = currentPreset.thresholdDb,
                        isPlaying = playerState.isPlaying,
                        isAnalyzing = isAnalyzing,
                        analysisProgressPercent = analysisProgressPercent
                    )
                }

                // 3. Preset Selector Bar
                item {
                    PresetSelectorBar(
                        selectedPreset = currentPreset,
                        presets = presets,
                        onSelectPreset = { preset -> viewModel.selectPreset(preset) },
                        onSavePreset = { name -> viewModel.saveCustomPreset(name) },
                        onDeletePreset = { id -> viewModel.deleteCustomPreset(id) },
                        onResetDefaults = { viewModel.resetDefaultPresets() }
                    )
                }

                // 4. Silence Detection Controls
                item {
                    SilenceDetectionControls(
                        preset = currentPreset,
                        onThresholdChange = { viewModel.updateThreshold(it) },
                        onMinDurationChange = { viewModel.updateMinSilenceDuration(it) },
                        onMaxDurationChange = { viewModel.updateMaxSilenceDuration(it) },
                        onActionChange = { viewModel.updateAction(it) },
                        onRemainingSilenceChange = { viewModel.updateRemainingSilence(it) }
                    )
                }

                // 5. Action Buttons & Analysis Breakdown
                item {
                    ActionButtonsBar(
                        analysisResult = analysisResult,
                        isAnalyzing = isAnalyzing,
                        isProcessing = processingState.stage in listOf(
                            ProcessingStage.ANALYZING,
                            ProcessingStage.TRUNCATING,
                            ProcessingStage.ENCODING,
                            ProcessingStage.VERIFYING
                        ),
                        hasLoadedMedia = metadata != null,
                        onAnalyzeClick = { viewModel.analyzeSilence() },
                        onProcessClick = { viewModel.processSilence() }
                    )
                }

                // 6. Processing Progress Card (when active or complete)
                item {
                    ProcessingProgressCard(
                        progressState = processingState,
                        onCancelClick = { viewModel.cancelProcessing() }
                    )
                }

                // 7. Audio Preview Player (Original vs Processed)
                item {
                    AudioPreviewPlayer(
                        playerState = playerState,
                        hasProcessedAudio = processedFile != null,
                        onTogglePlayPause = { viewModel.previewManager.togglePlayPause() },
                        onStop = { viewModel.previewManager.stop() },
                        onSeekTo = { targetMs -> viewModel.previewManager.seekTo(targetMs) },
                        onSwitchMode = { useProcessed -> viewModel.previewManager.switchMode(useProcessed) },
                        onSpeedChange = { speed -> viewModel.previewManager.setSpeed(speed) },
                        onVolumeChange = { vol -> viewModel.previewManager.setVolume(vol) }
                    )
                }

                // 8. Export Audio Settings & Action Panel
                item {
                    ExportPanel(
                        exportConfig = exportConfig,
                        hasProcessedOutput = processedFile != null,
                        onFormatChange = { viewModel.updateExportFormat(it) },
                        onSampleRateChange = { viewModel.updateExportSampleRate(it) },
                        onChannelsChange = { viewModel.updateExportChannels(it) },
                        onExportClick = {
                            val defaultName = "${metadata?.fileName?.substringBeforeLast('.') ?: "audio"}_silence_cut.wav"
                            exportFilePicker.launch(defaultName)
                        }
                    )
                }

                // Footer branding
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "RD STUDIO — AUTO SILENCE REMOVER",
                            color = StudioTextTertiary,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Owner: Shahneel Khan • rdstudio.online • Professional Dubbing Solutions",
                            color = StudioTextTertiary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showBatchDialog) {
        BatchQueueDialog(
            queue = batchQueue,
            onAddFiles = { batchFilePicker.launch("*/*") },
            onProcessAll = { viewModel.processAllBatch() },
            onClearBatch = { viewModel.clearBatch() },
            onDismiss = { showBatchDialog = false }
        )
    }

    if (showHistoryDialog) {
        HistoryDialog(
            history = history,
            onClearHistory = { /* clear in VM if needed */ },
            onDismiss = { showHistoryDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(onDismiss = { showSettingsDialog = false })
    }
}
