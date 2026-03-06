package com.omniagent.app.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay

/**
 * A Text composable that simulates a terminal typing effect.
 */
@Composable
fun TypingText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    delayMillis: Long = 20
) {
    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(text) {
        displayedText = ""
        text.forEach { char ->
            delay(delayMillis)
            displayedText += char
        }
    }

    Text(
        text = displayedText,
        modifier = modifier,
        color = color,
        style = style
    )
}
