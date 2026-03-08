package com.omniagent.app.ui.features.career

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniagent.app.ui.theme.OmniColors
import com.omniagent.app.viewmodel.OmniAgentViewModel

@Composable
fun AtsAuditScreen(viewModel: OmniAgentViewModel, onBack: () -> Unit) {
    var resumeText by remember { mutableStateOf("") }
    val auditResult by viewModel.careerAuditResult.collectAsState()
    val isProcessing by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.Background)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OmniColors.TextPrimary)
            }
            Text(
                text = "ATS AI Audit",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Paste your existing resume text below for a Beast-mode scan.",
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextTertiary
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = resumeText,
            onValueChange = { resumeText = it },
            modifier = Modifier.fillMaxWidth().height(250.dp),
            placeholder = { Text("Paste resume here...") },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.runCareerAudit(resumeText) },
            modifier = Modifier.fillMaxWidth(),
            enabled = resumeText.isNotBlank() && !isProcessing.isProcessing,
            colors = ButtonDefaults.buttonColors(containerColor = OmniColors.Accent)
        ) {
            if (isProcessing.isProcessing) {
                CircularProgressIndicator(size = 20.dp, color = OmniColors.Background)
            } else {
                Text("Run AI Audit")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        auditResult?.let { result ->
            AuditResultContent(result)
        }
    }
}

@Composable
fun JobTailorScreen(viewModel: OmniAgentViewModel, onBack: () -> Unit) {
    var resumeText by remember { mutableStateOf("") }
    var jdText by remember { mutableStateOf("") }
    val tailorResult by viewModel.careerTailorResult.collectAsState()
    val isProcessing by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.Background)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OmniColors.TextPrimary)
            }
            Text(
                text = "Job Tailor",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Resume", style = MaterialTheme.typography.titleSmall, color = OmniColors.Secondary)
        OutlinedTextField(
            value = resumeText,
            onValueChange = { resumeText = it },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            placeholder = { Text("Paste resume...") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Job Description", style = MaterialTheme.typography.titleSmall, color = OmniColors.Accent)
        OutlinedTextField(
            value = jdText,
            onValueChange = { jdText = it },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            placeholder = { Text("Paste JD...") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.runCareerTailor(resumeText, jdText) },
            modifier = Modifier.fillMaxWidth(),
            enabled = resumeText.isNotBlank() && jdText.isNotBlank() && !isProcessing.isProcessing,
            colors = ButtonDefaults.buttonColors(containerColor = OmniColors.Secondary)
        ) {
            Text("Tailor Resume (AI)")
        }

        Spacer(modifier = Modifier.height(32.dp))

        tailorResult?.let { result ->
            TailorResultContent(result)
        }
    }
}

@Composable
fun AuditResultContent(result: com.omniagent.app.core.model.EngineResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OmniColors.Surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Score: ${(result.structured_analysis["ats_score"] as? Number)?.toInt() ?: 0}/100",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = OmniColors.Accent
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Recommendations", style = MaterialTheme.typography.titleMedium, color = OmniColors.TextPrimary)
            (result.structured_analysis["recommendations"] as? List<*>)?.forEach { rec ->
                Text("• $rec", style = MaterialTheme.typography.bodySmall, color = OmniColors.TextSecondary, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
fun TailorResultContent(result: com.omniagent.app.core.model.EngineResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = OmniColors.Surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tailored Summary Suggestion", style = MaterialTheme.typography.titleMedium, color = OmniColors.Secondary)
            Text(
                result.structured_analysis["tailored_summary_suggestion"]?.toString() ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Improvement Steps", style = MaterialTheme.typography.titleMedium, color = OmniColors.TextPrimary)
             (result.structured_analysis["improvement_steps"] as? List<*>)?.forEach { step ->
                Text("• $step", style = MaterialTheme.typography.bodySmall, color = OmniColors.TextSecondary, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}
