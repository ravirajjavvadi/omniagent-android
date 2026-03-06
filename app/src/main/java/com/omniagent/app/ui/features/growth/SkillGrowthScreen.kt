package com.omniagent.app.ui.features.growth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniagent.app.core.model.AnalysisLog

/**
 * Skill Growth Tracking Dashboard.
 * Tracks module usage over time and visualizes improvement areas with graph-based progress.
 */
@Composable
fun SkillGrowthScreen(logs: List<AnalysisLog>) {
    val moduleUsage = logs.groupingBy { it.classifiedModule }.eachCount()
    val totalAnalyses = logs.size
    val avgConfidence = if (logs.isNotEmpty()) logs.map { it.confidence }.average() else 0.0

    // Animate bar growth
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                "Skill Growth Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Track your improvement across AI-powered analysis modules",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Stats Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Analyses",
                    value = "$totalAnalyses",
                    gradient = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Avg Confidence",
                    value = "${String.format("%.1f", avgConfidence * 100)}%",
                    gradient = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Modules Used",
                    value = "${moduleUsage.size}",
                    gradient = listOf(Color(0xFF10B981), Color(0xFF34D399))
                )
            }
        }

        // Module Usage Bar Chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Module Usage Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val maxCount = (moduleUsage.values.maxOrNull() ?: 1).toFloat()
                    moduleUsage.forEach { (module, count) ->
                        ModuleBarChart(
                            moduleName = module.replaceFirstChar { it.uppercase() },
                            count = count,
                            maxCount = maxCount,
                            animatedFraction = animatedProgress.value
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        // Confidence Trend (Line Chart)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Confidence Trend Over Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ConfidenceTrendChart(
                        logs = logs.takeLast(20).reversed(),
                        animatedFraction = animatedProgress.value
                    )
                }
            }
        }

        // Improvement Areas
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Detected Improvement Areas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val lowConfLogs = logs.filter { it.confidence < 0.6 }
                    val areas = lowConfLogs.groupingBy { it.classifiedModule }.eachCount()

                    if (areas.isEmpty()) {
                        Text(
                            "🎉 Great job! Your analyses are all above 60% confidence!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF10B981)
                        )
                    } else {
                        areas.forEach { (module, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "⚠️ ${module.replaceFirstChar { it.uppercase() }}",
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "$count low-confidence runs",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    gradient: List<Color>
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradient))
                .padding(12.dp)
        ) {
            Column {
                Text(title, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun ModuleBarChart(
    moduleName: String,
    count: Int,
    maxCount: Float,
    animatedFraction: Float
) {
    val fraction = (count / maxCount) * animatedFraction
    val barColor = when (moduleName.lowercase()) {
        "coding" -> Color(0xFF6366F1)
        "cybersecurity" -> Color(0xFFEF4444)
        "resume" -> Color(0xFF10B981)
        "startup" -> Color(0xFFF59E0B)
        else -> Color(0xFF8B5CF6)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(moduleName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("$count", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Brush.horizontalGradient(listOf(barColor, barColor.copy(alpha = 0.6f))))
            )
        }
    }
}

@Composable
fun ConfidenceTrendChart(
    logs: List<AnalysisLog>,
    animatedFraction: Float
) {
    if (logs.isEmpty()) {
        Text("No data yet. Run some analyses to see your confidence trend!", style = MaterialTheme.typography.bodySmall)
        return
    }

    val accentColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val width = size.width
        val height = size.height
        val padding = 8f

        // Draw grid lines
        for (i in 0..4) {
            val y = padding + (height - 2 * padding) * i / 4
            drawLine(
                color = gridColor.copy(alpha = 0.3f),
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1f
            )
        }

        if (logs.size < 2) return@Canvas

        val points = logs.mapIndexed { index, log ->
            val x = padding + (width - 2 * padding) * index / (logs.size - 1)
            val y = height - padding - (log.confidence.toFloat() * (height - 2 * padding) * animatedFraction)
            Offset(x, y)
        }

        // Draw line path
        val path = Path()
        points.forEachIndexed { index, point ->
            if (index == 0) path.moveTo(point.x, point.y)
            else path.lineTo(point.x, point.y)
        }
        drawPath(
            path = path,
            color = accentColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // Draw dots
        points.forEach { point ->
            drawCircle(
                color = accentColor,
                radius = 5f,
                center = point
            )
        }
    }
}
