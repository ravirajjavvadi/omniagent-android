package com.omniagent.app.ui.features.resume

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniagent.app.ui.theme.OmniColors
import com.omniagent.app.viewmodel.OmniAgentViewModel

@Composable
fun ResumeTailorScreen(viewModel: OmniAgentViewModel) {
    var resumeText by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    val tailoringResult by viewModel.tailoringResult.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.Background)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Resume Tailoring",
            style = MaterialTheme.typography.headlineMedium,
            color = OmniColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Align your resume with specific job requirements using offline AI.",
            style = MaterialTheme.typography.bodySmall,
            color = OmniColors.TextTertiary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Resume Input
        OutlinedTextField(
            value = resumeText,
            onValueChange = { resumeText = it },
            label = { Text("Your Resume (Paste Content)") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = OmniColors.Surface,
                unfocusedContainerColor = OmniColors.Surface,
                focusedTextColor = OmniColors.TextPrimary,
                unfocusedTextColor = OmniColors.TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Job Description Input
        OutlinedTextField(
            value = jobDescription,
            onValueChange = { jobDescription = it },
            label = { Text("Job Description") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = OmniColors.Surface,
                unfocusedContainerColor = OmniColors.Surface,
                focusedTextColor = OmniColors.TextPrimary,
                unfocusedTextColor = OmniColors.TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.tailorResume(resumeText, jobDescription) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isProcessing && resumeText.isNotBlank() && jobDescription.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = OmniColors.Primary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tailor My Resume")
        }

        if (uiState.isProcessing) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OmniColors.Primary)
            }
        }

        tailoringResult?.let { result ->
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Tailoring Results",
                style = MaterialTheme.typography.titleLarge,
                color = OmniColors.Primary,
                fontWeight = FontWeight.Bold
            )
            
            // Display Reasoning
            result.reasoning.forEach { step ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("•", color = OmniColors.Secondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(step, style = MaterialTheme.typography.bodyMedium, color = OmniColors.TextPrimary)
                }
            }
        }
    }
}
