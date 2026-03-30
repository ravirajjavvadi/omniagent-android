package com.omniagent.app.ui.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import java.io.File
import com.omniagent.app.engine.ModelDownloadManager
import com.omniagent.app.engine.ModelInfo
import com.omniagent.app.engine.DownloadState
import com.omniagent.app.ui.theme.OmniColors
import com.omniagent.app.viewmodel.OmniAgentViewModel

@Composable
fun ModelSelectionScreen(
    viewModel: OmniAgentViewModel,
    downloadManager: ModelDownloadManager,
    downloadState: DownloadState,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Offline AI Setup", color = OmniColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OmniColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = OmniColors.Surface)
            )
        },
        containerColor = OmniColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Memory,
                contentDescription = "AI Engine",
                tint = OmniColors.Accent,
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Intelligence Engine",
                style = MaterialTheme.typography.headlineMedium,
                color = OmniColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "OmniAgent requires an AI Brain to operate entirely offline. Select a model based on your phone's capabilities (RAM/Space).",
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextSecondary,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            downloadManager.availableModels.forEach { model ->
                ModelCard(
                    model = model,
                    downloadState = downloadState,
                    isDownloaded = downloadManager.isModelFullyDownloaded(model.id),
                    onDownload = { downloadManager.startDownload(model.id) },
                    onSelect = { 
                        viewModel.selectModel(model.id)
                        onBack() // This navigates back to Chat
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ModelCard(
    model: ModelInfo,
    downloadState: DownloadState,
    isDownloaded: Boolean,
    onDownload: () -> Unit,
    onSelect: () -> Unit
) {
    val context = LocalContext.current
    val isRActiveModel = downloadState.modelId == model.id
    val isDownloading = isRActiveModel && downloadState.status == "DOWNLOADING"
    val isSuccess = isRActiveModel && downloadState.status == "SUCCESS"
    val isFailed = isRActiveModel && downloadState.status == "FAILED"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = OmniColors.Surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = OmniColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Size: ~${model.sizeMb} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = OmniColors.TextTertiary
                    )
                }
                
                
                when {
                    isDownloading -> {
                        Text(
                            text = "${downloadState.progress}%",
                            color = OmniColors.Accent,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    isDownloaded || isSuccess -> {
                        Button(
                            onClick = onSelect,
                            colors = ButtonDefaults.buttonColors(containerColor = OmniColors.Primary)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Activate", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Activate Brain")
                        }
                    }
                    isFailed -> {
                        TextButton(onClick = onDownload) {
                            Text("Retry", color = Color.Red)
                        }
                    }
                    else -> {
                        IconButton(onClick = onDownload) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Download", tint = OmniColors.Primary)
                        }
                    }
                }
            }

            if (isDownloading) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = if (downloadState.progress > 0) downloadState.progress / 100f else 0.01f,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = OmniColors.Accent,
                    trackColor = OmniColors.SurfaceElevated
                )
                Text(
                    text = "${downloadState.downloadedBytes / (1024 * 1024)} MB / ${downloadState.totalBytes / (1024 * 1024)} MB",
                    style = MaterialTheme.typography.labelSmall,
                    color = OmniColors.TextTertiary,
                    modifier = Modifier.padding(top = 4.dp).align(Alignment.End)
                )
            }
            
            if (isFailed) {
                Text(
                    text = "Download failed. Check internet connection and space.",
                    color = Color.Red,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
