package com.omniagent.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omniagent.app.engine.ModelDownloadManager
import com.omniagent.app.engine.DownloadState
import com.omniagent.app.ui.features.dashboard.DashboardScreen
import com.omniagent.app.ui.features.admin.AdminDashboardScreen
import com.omniagent.app.ui.features.splash.SplashScreen
import com.omniagent.app.core.model.UserRole
import com.omniagent.app.ui.theme.OmniAgentTheme
import com.omniagent.app.viewmodel.OmniAgentViewModel
import com.omniagent.app.viewmodel.OmniAgentViewModelFactory

/**
 * MainActivity — Entry point for the OmniAgent control platform.
 * Uses Jetpack Compose for the entire UI.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OmniAgentTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(
                        onSplashFinished = { showSplash = false }
                    )
                } else {
                    // Get DI container from Application
                    val appContainer = (applicationContext as OmniAgentApplication).container
                
                    // Inject repository into ViewModel via factory
                    val viewModel: OmniAgentViewModel = viewModel(
                        factory = OmniAgentViewModelFactory(
                            repository = appContainer.analysisRepository,
                            application = applicationContext as OmniAgentApplication
                        )
                    )

                    // Model Download Manager for offline AI brain
                    val downloadManager = remember { ModelDownloadManager(applicationContext) }
                    val downloadState by downloadManager.downloadState.collectAsStateWithLifecycle()

                    // Request Notification Permission for Android 13+
                    val context = androidx.compose.ui.platform.LocalContext.current
                    LaunchedEffect(Unit) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                (context as? androidx.activity.ComponentActivity)?.requestPermissions(
                                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                                    1001
                                )
                            }
                        }
                    }

                    // Determine initial tab on first splash finish
                    LaunchedEffect(showSplash) {
                        if (!showSplash && !downloadManager.isAnyModelDownloaded()) {
                            viewModel.switchTab(com.omniagent.app.viewmodel.DashboardTab.MODEL_SELECTION)
                        }
                    }

                    // Handle Widget Intents
                    LaunchedEffect(intent) {
                        intent?.getStringExtra("target_tab")?.let { tabName ->
                            try {
                                val tab = com.omniagent.app.viewmodel.DashboardTab.valueOf(tabName)
                                viewModel.switchTab(tab)
                            } catch (e: Exception) {
                                // Default tab
                            }
                        }
                        
                        // Handle Specialized Action Triggers (e.g., SCAN from widget)
                        intent?.getStringExtra("trigger_action")?.let { action ->
                            if (action == "SCAN") {
                                viewModel.analyzeInput("vulnerability scan")
                            }
                        }
                    }

                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val classificationResult by viewModel.classificationResult.collectAsStateWithLifecycle()
                    val engineResult by viewModel.engineResult.collectAsStateWithLifecycle()
                    val reasoningSteps by viewModel.reasoningSteps.collectAsStateWithLifecycle()
                    val logs by viewModel.recentLogs.collectAsStateWithLifecycle(initialValue = emptyList())

                    DashboardScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        classificationResult = classificationResult,
                        engineResult = engineResult,
                        reasoningSteps = reasoningSteps,
                        logs = logs,
                        onAnalyze = { viewModel.analyzeInput(it) },
                        onSwitchTab = { viewModel.switchTab(it) },
                        onClearResults = { viewModel.clearResults() },
                        onClearLogs = { viewModel.clearAllLogs() },
                        onDecryptLog = { viewModel.decryptLogResult(it) },
                        downloadManager = downloadManager,
                        downloadState = downloadState,
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                    )
                }
            }
        }
    }
}

