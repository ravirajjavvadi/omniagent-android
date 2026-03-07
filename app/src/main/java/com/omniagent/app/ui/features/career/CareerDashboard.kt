package com.omniagent.app.ui.features.career

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniagent.app.ui.theme.OmniColors
import com.omniagent.app.viewmodel.OmniAgentViewModel

@Composable
fun CareerDashboard(viewModel: OmniAgentViewModel) {
    var showBuilder by remember { mutableStateOf(false) }

    Crossfade(targetState = showBuilder, label = "CareerNav") { isBuilder ->
        if (isBuilder) {
            ResumeBuilderForm(
                viewModel = viewModel,
                onBack = { showBuilder = false }
            )
        } else {
            CareerHubOverview(onNavigateToBuilder = { showBuilder = true })
        }
    }
}

@Composable
fun CareerHubOverview(onNavigateToBuilder: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.Background)
            .padding(16.dp)
    ) {
        HeaderSection()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        ScoreSection()
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Intelligence Modules",
            style = MaterialTheme.typography.titleMedium,
            color = OmniColors.TextSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        val modules = listOf(
            CareerModule(
                title = "Smart Builder",
                description = "Build a beast resume from scratch using AI tips.",
                icon = Icons.Default.Construction,
                color = OmniColors.Primary,
                onClick = onNavigateToBuilder
            ),
            CareerModule(
                title = "ATS AI Audit",
                description = "Check your score against top industry standards.",
                icon = Icons.Default.Analytics,
                color = OmniColors.Accent
            ),
            CareerModule(
                title = "Job Tailor",
                description = "Instantly align your CV with any JD.",
                icon = Icons.Default.AutoAwesome,
                color = OmniColors.Secondary
            ),
            CareerModule(
                title = "PDF Export",
                description = "Generate a professional PDF and save it.",
                icon = Icons.Default.PictureAsPdf,
                color = Color(0xFFFF5252)
            )
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(modules) { module ->
                GlassCard(module)
            }
        }
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Career Hub",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary
            )
            Text(
                text = "Beast Mode Active",
                style = MaterialTheme.typography.labelMedium,
                color = OmniColors.Accent,
                fontWeight = FontWeight.Medium
            )
        }
        
        Surface(
            color = OmniColors.Surface,
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.Stars,
                contentDescription = null,
                tint = OmniColors.Accent,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun ScoreSection() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(16.dp),
        color = OmniColors.Surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Animated Gradient Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                OmniColors.Primary.copy(alpha = 0.1f),
                                OmniColors.Accent.copy(alpha = 0.05f)
                            )
                        )
                    )
            )
            
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = 0.85f,
                        color = OmniColors.Accent,
                        strokeWidth = 8.dp,
                        modifier = Modifier.size(80.dp)
                    )
                    Text(
                        text = "85",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = OmniColors.TextPrimary
                    )
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Column {
                    Text(
                        text = "Overall ATS Rank",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OmniColors.TextSecondary
                    )
                    Text(
                        text = "EXCELLENT",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = OmniColors.Accent
                    )
                    Text(
                        text = "You are in the top 10% of candidates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OmniColors.TextTertiary
                    )
                }
            }
        }
    }
}

@Composable
fun GlassCard(module: CareerModule) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { module.onClick() },
        color = OmniColors.Surface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = module.icon,
                contentDescription = null,
                tint = module.color,
                modifier = Modifier.size(32.dp)
            )
            
            Column {
                Text(
                    text = module.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmniColors.TextPrimary
                )
                Text(
                    text = module.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OmniColors.TextTertiary,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

data class CareerModule(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit = {}
)

private fun Modifier.fillWeight(f: Float) = this.then(Modifier.fillMaxWidth().fillMaxHeight())
private val CircleShape = RoundedCornerShape(100.dp)
