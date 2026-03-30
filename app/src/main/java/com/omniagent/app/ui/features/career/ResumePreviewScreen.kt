package com.omniagent.app.ui.features.career

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniagent.app.core.model.ResumeData
import com.omniagent.app.ui.theme.OmniColors
import com.omniagent.app.core.util.ResumePdfExporter

@Composable
fun ResumePreviewScreen(resumeData: ResumeData) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(OmniColors.Background)
    ) {
        Text(
            "Live Preview", 
            style = MaterialTheme.typography.titleLarge, 
            fontWeight = FontWeight.Bold, 
            color = OmniColors.TextPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Final Resume Card (Preview)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            color = Color.White,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header (based on template)
                val template = RESUME_TEMPLATES.find { it.id == resumeData.templateId } ?: RESUME_TEMPLATES[0]
                
                Text(
                    text = resumeData.fullName.ifBlank { "Full Name" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = template.primaryColor
                )
                
                Text(
                    text = "${resumeData.email} | ${resumeData.phone}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = template.accentColor, thickness = 2.dp)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content Sections
                ResumeSectionPreview("EXPERIENCE", "${resumeData.jobTitle} at ${resumeData.company}")
                Text(resumeData.experienceDescription, style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                
                Spacer(modifier = Modifier.height(16.dp))
                ResumeSectionPreview("EDUCATION", resumeData.education)
                
                Spacer(modifier = Modifier.height(16.dp))
                ResumeSectionPreview("SKILLS", resumeData.skills)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { ResumePdfExporter.exportResume(context, resumeData) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OmniColors.Accent)
        ) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Download PDF", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ResumeSectionPreview(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Gray,
            letterSpacing = 1.sp
        )
        if (content.isNotBlank()) {
            Text(
                text = content,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}
