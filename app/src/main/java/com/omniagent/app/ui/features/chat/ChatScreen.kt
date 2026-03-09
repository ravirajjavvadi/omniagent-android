package com.omniagent.app.ui.features.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
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
import com.omniagent.app.ui.theme.OmniColors
import com.omniagent.app.viewmodel.OmniAgentViewModel

@Composable
fun ChatScreen(viewModel: OmniAgentViewModel, localModelPath: String? = null) {
    val messages by viewModel.chatMessages.collectAsState()
    val isProcessing by viewModel.uiState.collectAsState()
    val reasoningSteps by viewModel.reasoningSteps.collectAsState()
    val inputText by viewModel.chatInput.collectAsState()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.Background)
            .imePadding()
            .padding(16.dp)
    ) {
        // Chat Header
        Text(
            text = "OmniAgent Chat",
            style = MaterialTheme.typography.headlineMedium,
            color = OmniColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Text(
            text = if (localModelPath != null) "⚡ Offline AI Active — Full answers available" 
                   else "⚠ Basic mode — Download an AI model in Settings for full answers",
            style = MaterialTheme.typography.bodySmall,
            color = if (localModelPath != null) OmniColors.Accent else OmniColors.TextTertiary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            reverseLayout = false
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
            if (isProcessing.isProcessing) {
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
                                    text = "10 THREADS",
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

            IconButton(
                onClick = {
                    if (isProcessing.isProcessing) {
                        viewModel.stopResponse()
                    } else if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText, localModelPath)
                        viewModel.updateChatInput("")
                    }
                },
                modifier = Modifier
                    .background(
                        if (isProcessing.isProcessing) Color.Red.copy(alpha = 0.8f) 
                        else MaterialTheme.colorScheme.primary, 
                        RoundedCornerShape(24.dp)
                    )
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = if (isProcessing.isProcessing) Icons.Default.Stop else Icons.Default.Send,
                    contentDescription = if (isProcessing.isProcessing) "Stop" else "Send",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgColor = if (message.isUser) OmniColors.PrimaryDim else OmniColors.SurfaceElevated
    val textColor = OmniColors.TextPrimary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 0.dp,
                        bottomEnd = if (message.isUser) 0.dp else 16.dp
                    )
                )
                .background(bgColor)
                .padding(12.dp)
                .widthIn(max = 280.dp)
        ) {
            Column {
                // Use plain Text for BOTH user and AI messages.
                // AI streaming already provides the word-by-word effect naturally.
                // TypingText was causing a crash by restarting the full animation
                // on every single token, creating hundreds of competing coroutines.
                Text(
                    text = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )

                // Show a subtle blinking cursor while AI is still generating
                if (!message.isUser && message.text.isNotBlank() && message.classification != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Offline AI • ${message.classification.moduleName}",
                        color = OmniColors.TextTertiary.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
