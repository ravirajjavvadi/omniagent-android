package com.omniagent.app.ui.features.chat

import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniagent.app.core.model.ChatMessage
import com.omniagent.app.ui.common.TypingText
import com.omniagent.app.ui.common.CodeCanvas
import com.omniagent.app.ui.theme.OmniColors
import com.omniagent.app.viewmodel.OmniAgentViewModel
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent

// Response length options
enum class ResponseLength(val label: String, val maxTokens: Int) {
    BRIEF("Brief", 128),
    BALANCED("Balanced", 256),
    DETAILED("Detailed", 512)
}

@Composable
fun ChatScreen(viewModel: OmniAgentViewModel, localModelPath: String? = null) {
    val clipboardManager = LocalClipboardManager.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val currentSessionTitle by viewModel.currentSessionTitle.collectAsState()
    val context = LocalContext.current
    val messages by viewModel.chatMessages.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.chatInput.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val reasoningSteps by viewModel.reasoningSteps.collectAsState()
    
    // UI state for scrolling
    val listState = rememberLazyListState()
    
    // Auto-scroll when messages change, processing starts, or text streams
    LaunchedEffect(messages.size, uiState.isProcessing, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    // Model selection state
    var selectedModel by remember { mutableStateOf(localModelPath ?: "") }
    var showModelSelector by remember { mutableStateOf(false) }
    
    // Response length state
    var responseLength by remember { mutableStateOf(ResponseLength.BALANCED) }
    var showLengthSelector by remember { mutableStateOf(false) }
    
    // Regenerate state
    var lastUserMessage by remember { mutableStateOf("") }

    // Model options
    val modelOptions = listOf(
        "qwen2.5_0_5b" to "Qwen 0.5B (Fast)",
        "qwen2.5_1_5b" to "Qwen 1.5B (Balanced)", 
        "gemma_2_2b" to "Gemma 2.2B (Advanced)"
    )

    // Resolve the internal key or external path to a valid engine path
    val resolvedModelPath = remember(selectedModel, localModelPath) {
        if (selectedModel.startsWith("/") || selectedModel.contains(":\\")) {
            selectedModel
        } else if (selectedModel.isNotEmpty()) {
            context.getExternalFilesDir(null)?.absolutePath + "/$selectedModel.gguf"
        } else {
            localModelPath // Fallback to auto-detected path
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            Toast.makeText(context, "File attached: ${it.lastPathSegment}", Toast.LENGTH_SHORT).show()
        }
    }
    
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceRecording()
        } else {
            Toast.makeText(context, "Permission denied for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    var showThinkingPopup by remember { mutableStateOf(false) }

    if (showThinkingPopup) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showThinkingPopup = false },
            containerColor = OmniColors.SurfaceElevated,
            dragHandle = { BottomSheetDefaults.DragHandle(color = OmniColors.Accent) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "AI Processing Kernel",
                    style = MaterialTheme.typography.headlineSmall,
                    color = OmniColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                reasoningSteps.forEachIndexed { index, step ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "${index + 1}.",
                            color = OmniColors.Accent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(
                            text = step,
                            color = OmniColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                if (reasoningSteps.isEmpty()) {
                    Text(
                        text = "Analyzing neural weights and generating contextually optimal response...",
                        color = OmniColors.TextTertiary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    // Model Selector Dialog
    if (showModelSelector) {
        AlertDialog(
            onDismissRequest = { showModelSelector = false },
            title = { Text("Select AI Model", color = OmniColors.TextPrimary) },
            text = {
                Column {
                    modelOptions.forEach { (id, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedModel = id
                                    showModelSelector = false 
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedModel == id,
                                onClick = { 
                                    selectedModel = id
                                    showModelSelector = false 
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(name, color = OmniColors.TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelSelector = false }) { Text("Cancel") }
            },
            containerColor = OmniColors.SurfaceElevated
        )
    }

    // Length Selector Dialog
    if (showLengthSelector) {
        AlertDialog(
            onDismissRequest = { showLengthSelector = false },
            title = { Text("Response Length", color = OmniColors.TextPrimary) },
            text = {
                Column {
                    ResponseLength.entries.forEach { length ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    responseLength = length
                                    showLengthSelector = false 
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = responseLength == length,
                                onClick = { 
                                    responseLength = length
                                    showLengthSelector = false 
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("${length.label} (~${length.maxTokens} tokens)", color = OmniColors.TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLengthSelector = false }) { Text("Cancel") }
            },
            containerColor = OmniColors.SurfaceElevated
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.background(OmniColors.Surface)
            ) {
                ChatHistorySidebar(
                    history = chatHistory,
                    currentSessionId = currentSessionId,
                    onSessionClick = { 
                        viewModel.switchSession(it)
                        scope.launch { drawerState.close() }
                    },
                    onNewChat = {
                        viewModel.createNewSession()
                        scope.launch { drawerState.close() }
                    },
                    onRename = { id, title -> viewModel.renameSession(id, title) },
                    onDelete = { id -> viewModel.deleteSession(id) }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            OmniColors.Background,
                            OmniColors.Surface.copy(alpha = 0.95f),
                            OmniColors.Background
                        )
                    )
                )
                .imePadding()
                .padding(16.dp)
        ) {
            // Chat Header with Model & Length Selectors
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Default.Menu, contentDescription = "History", tint = OmniColors.Accent)
                }
                Text(
                    text = currentSessionTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = OmniColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                // Model Selector (Icon + Label)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showModelSelector = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = OmniColors.Accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    val currentId = if (selectedModel.startsWith("/") || selectedModel.contains(":\\")) {
                        selectedModel.substringAfterLast("/").substringAfterLast("\\").substringBeforeLast(".gguf")
                    } else {
                        selectedModel
                    }
                    Text(
                        text = modelOptions.find { it.first == currentId }?.second?.substringBefore(" (") ?: "Select Model",
                        style = MaterialTheme.typography.labelMedium,
                        color = OmniColors.Accent
                    )
                }
                
                Spacer(Modifier.width(8.dp))

                // Length Selector (Icon + Label)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showLengthSelector = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = OmniColors.Accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = responseLength.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = OmniColors.Accent
                    )
                }
                
                // Regenerate Button
                if (messages.isNotEmpty() && !messages.last().isUser && lastUserMessage.isNotEmpty()) {
                    IconButton(onClick = { 
                        viewModel.sendMessage(lastUserMessage, resolvedModelPath, responseLength.maxTokens)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = OmniColors.Accent)
                    }
                }
                
                IconButton(onClick = { viewModel.createNewSession() }) {
                    Icon(Icons.Default.Add, contentDescription = "New Chat", tint = OmniColors.Accent)
                }
            }
            
            // Active model indicator
            Row(
                modifier = Modifier.padding(start = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentId = if (selectedModel.startsWith("/") || selectedModel.contains(":\\")) {
                    selectedModel.substringAfterLast("/").substringAfterLast("\\").substringBeforeLast(".gguf")
                } else {
                    selectedModel
                }
                val activeModelName = modelOptions.find { it.first == currentId }?.second ?: (if (selectedModel.isNotEmpty()) "Offline AI Active" else "No Brain Selected")
                
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = OmniColors.Accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = activeModelName,
                    style = MaterialTheme.typography.labelSmall,
                    color = OmniColors.Accent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = OmniColors.SurfaceElevated,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = responseLength.label,
                        color = OmniColors.TextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = if (localModelPath != null) "⚡ Offline AI Active — Full answers available" 
                       else "⚠ Basic mode — Download an AI model in Settings for full answers",
                style = MaterialTheme.typography.bodySmall,
                color = if (localModelPath != null) OmniColors.Accent else OmniColors.TextTertiary,
                modifier = Modifier.padding(start = 48.dp, bottom = 8.dp)
            )

        // Messages List
        SelectionContainer(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                reverseLayout = false
            ) {
                items(messages) { message ->
                    ChatBubble(message, onCopy = {
                        clipboardManager.setText(AnnotatedString(message.text))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    })
                }
                if (uiState.isProcessing) {
                    item {
                        TextButton(
                            onClick = { showThinkingPopup = true },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = OmniColors.Accent
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "AI Kernel is thinking...",
                                    color = OmniColors.Accent,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = OmniColors.Accent.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "8 THREADS",
                                        color = OmniColors.Accent,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { viewModel.updateChatInput(it) },
                placeholder = { Text("Ask anything...") },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = OmniColors.SurfaceElevated,
                    unfocusedContainerColor = OmniColors.SurfaceElevated,
                    focusedTextColor = OmniColors.TextPrimary,
                    unfocusedTextColor = OmniColors.TextPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Voice Assistant Button
            IconButton(
                onClick = {
                    if (isRecording) {
                        viewModel.stopVoiceRecording()
                    } else {
                        val permissionCheck = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        )
                        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                            viewModel.startVoiceRecording()
                        } else {
                            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                modifier = Modifier
                    .background(
                        if (isRecording) OmniColors.Accent.copy(alpha = 0.8f) 
                        else OmniColors.SurfaceElevated, 
                        RoundedCornerShape(24.dp)
                    )
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = if (isRecording) Color.White else OmniColors.Primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (uiState.isProcessing) {
                        viewModel.stopResponse()
                    } else if (inputText.isNotBlank()) {
                        lastUserMessage = inputText
                        viewModel.sendMessage(inputText, resolvedModelPath, responseLength.maxTokens)
                        viewModel.updateChatInput("")
                    }
                },
                modifier = Modifier
                    .background(
                        if (uiState.isProcessing) Color.Red.copy(alpha = 0.8f) 
                        else MaterialTheme.colorScheme.primary, 
                        RoundedCornerShape(24.dp)
                    )
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isProcessing) Icons.Default.Stop else Icons.Default.Send,
                    contentDescription = if (uiState.isProcessing) "Stop" else "Send",
                    tint = Color.White
                )
            }
        }
    }
}
}

@Composable
fun ChatBubble(message: ChatMessage, onCopy: () -> Unit) {


    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgColor = if (message.isUser) OmniColors.Primary.copy(alpha = 0.15f) else OmniColors.SurfaceElevated.copy(alpha = 0.8f)
    val borderColor = if (message.isUser) OmniColors.Primary.copy(alpha = 0.3f) else OmniColors.Border
    val textColor = OmniColors.TextPrimary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        val parts = message.text.split("```")
        
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Code block
                val language = part.lineSequence().firstOrNull() ?: ""
                val code = if (language.isNotBlank() && language.length < 15) {
                    part.removePrefix(language).trim()
                } else {
                    part.trim()
                }
                CodeCanvas(code = code, language = if (language.length < 15) language else "Code")
            } else if (part.isNotBlank()) {
                // Normal text
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (message.isUser) 16.dp else 2.dp,
                                bottomEnd = if (message.isUser) 2.dp else 16.dp
                            )
                        )
                        .background(bgColor)
                        .border(
                            width = 1.dp,
                            color = borderColor,
                            shape = if (!message.isUser) 
                                RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
                            else 
                                RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
                        )
                        .padding(12.dp)
                        .widthIn(max = 280.dp)
                ) {
                    Column {
                        Text(
                            text = part.trim(),
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        if (!message.isUser && index == parts.lastIndex && message.classification != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Offline AI • ${message.classification.moduleName}",
                                    color = OmniColors.TextTertiary.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.ContentCopy, "Copy", tint = OmniColors.TextTertiary.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ChatHistorySidebar(
    history: List<com.omniagent.app.core.model.ChatSession>,
    currentSessionId: String,
    onSessionClick: (com.omniagent.app.core.model.ChatSession) -> Unit,
    onNewChat: () -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var showRenameDialog by remember { mutableStateOf<com.omniagent.app.core.model.ChatSession?>(null) }

    if (showRenameDialog != null) {
        RenameDialog(
            currentTitle = showRenameDialog!!.title,
            onDismiss = { showRenameDialog = null },
            onConfirm = { newTitle ->
                onRename(showRenameDialog!!.id, newTitle)
                showRenameDialog = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Chat History",
            style = MaterialTheme.typography.titleLarge,
            color = OmniColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = onNewChat,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = OmniColors.Accent.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Add, "New", tint = OmniColors.Accent)
            Spacer(Modifier.width(8.dp))
            Text("New Chat", color = OmniColors.Accent)
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Grouping logic
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        val today = history.filter { now - it.lastTimestamp < dayMs }
        val yesterday = history.filter { it.lastTimestamp in (now - 2 * dayMs)..(now - dayMs) }
        val older = history.filter { now - it.lastTimestamp > 2 * dayMs }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (today.isNotEmpty()) {
                item { HistoryCategory("Today") }
                items(today) { session ->
                    HistoryItem(session, currentSessionId == session.id, onSessionClick, { showRenameDialog = session }, onDelete)
                }
            }
            if (yesterday.isNotEmpty()) {
                item { HistoryCategory("Yesterday") }
                items(yesterday) { session ->
                    HistoryItem(session, currentSessionId == session.id, onSessionClick, { showRenameDialog = session }, onDelete)
                }
            }
            if (older.isNotEmpty()) {
                item { HistoryCategory("Older") }
                items(older) { session ->
                    HistoryItem(session, currentSessionId == session.id, onSessionClick, { showRenameDialog = session }, onDelete)
                }
            }
        }
    }
}

@Composable
fun HistoryCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = OmniColors.TextTertiary,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    )
}

@Composable
fun HistoryItem(
    session: com.omniagent.app.core.model.ChatSession,
    isSelected: Boolean,
    onSessionClick: (com.omniagent.app.core.model.ChatSession) -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: (String) -> Unit
) {
    Surface(
        onClick = { onSessionClick(session) },
        color = if (isSelected) OmniColors.Accent.copy(alpha = 0.2f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ChatBubbleOutline, null, tint = OmniColors.TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                session.title,
                color = if (isSelected) OmniColors.TextPrimary else OmniColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                IconButton(onClick = onRenameClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Edit, null, tint = OmniColors.Accent, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { onDeleteClick(session.id) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun RenameDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Chat") },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = OmniColors.SurfaceElevated,
        titleContentColor = OmniColors.TextPrimary,
        textContentColor = OmniColors.TextSecondary
    )
}
