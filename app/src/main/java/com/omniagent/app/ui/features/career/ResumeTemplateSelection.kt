package com.omniagent.app.ui.features.career

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniagent.app.ui.theme.OmniColors

data class ResumeTemplate(
    val id: Int,
    val name: String,
    val description: String,
    val primaryColor: Color,
    val accentColor: Color
)

val RESUME_TEMPLATES = listOf(
    ResumeTemplate(0, "Basic", "Clean and standard B&W layout.", Color.Black, Color.Gray),
    ResumeTemplate(1, "Modern", "Sleek look with cool accent colors.", Color(0xFF3B82F6), Color(0xFF93C5FD)),
    ResumeTemplate(2, "Creative", "Stand out with bold headers.", Color(0xFF8B5CF6), Color(0xFFC4B5FD)),
    ResumeTemplate(3, "Executive", "Professional for high-level roles.", Color(0xFF1F2937), Color(0xFFD1D5DB))
)

@Composable
fun TemplateSelectionForm(selectedId: Int, onSelect: (Int) -> Unit) {
    Column {
        Text("Select Template", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OmniColors.TextPrimary)
        Text("Choose the style that fits your career goals.", style = MaterialTheme.typography.bodyMedium, color = OmniColors.TextTertiary)
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().height(400.dp)
        ) {
            items(RESUME_TEMPLATES) { template ->
                TemplateCard(
                    template = template,
                    isSelected = selectedId == template.id,
                    onClick = { onSelect(template.id) }
                )
            }
        }
    }
}

@Composable
fun TemplateCard(template: ResumeTemplate, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) OmniColors.Accent else OmniColors.Border,
                shape = RoundedCornerShape(12.dp)
            ),
        color = OmniColors.Surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Mock layout thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(template.primaryColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.width(40.dp).height(4.dp).background(template.primaryColor))
                        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(template.accentColor.copy(alpha = 0.5f)))
                        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(template.accentColor.copy(alpha = 0.5f)))
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(template.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = OmniColors.TextPrimary)
                Text(template.description, style = MaterialTheme.typography.labelSmall, color = OmniColors.TextTertiary, lineHeight = 12.sp)
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = OmniColors.Accent,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp)
                )
            }
        }
    }
}
