package com.omniagent.app.ui.common

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniagent.app.ui.theme.OmniColors
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

/**
 * Module Action Card — Large clickable widgets for the home screen grid.
 */
@Composable
fun ModuleActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        color = OmniColors.Surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape) // More professional circular icon background
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium, // Better typography
                    color = OmniColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = OmniColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Command Input Panel — The primary user input component.
 * Designed to look like a terminal command line.
 */
@Composable
fun CommandInputPanel(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isProcessing) OmniColors.Warning else OmniColors.Border,
        animationSpec = tween(300),
        label = "borderColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Label
        Text(
            text = "COMMAND INPUT",
            style = MaterialTheme.typography.labelSmall,
            color = OmniColors.TextTertiary,
            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
        )

        // Input field with terminal styling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OmniColors.Surface, RoundedCornerShape(12.dp))
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Terminal prompt indicator
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isProcessing) OmniColors.Warning
                        else OmniColors.Secondary
                    )
            )

            Text(
                text = "›",
                color = OmniColors.TextTertiary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 14.dp),
                textStyle = TextStyle(
                    color = OmniColors.TextPrimary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(OmniColors.Primary),
                singleLine = false,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = "Enter command or paste content to analyze...",
                                color = OmniColors.TextTertiary,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Submit button
            IconButton(
                onClick = onSubmit,
                enabled = value.isNotBlank() && !isProcessing,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(40.dp)
                    .background(
                        if (value.isNotBlank() && !isProcessing) OmniColors.Primary
                        else OmniColors.SurfaceBright,
                        RoundedCornerShape(8.dp)
                    )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = OmniColors.Warning,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Analyze",
                        tint = if (value.isNotBlank()) OmniColors.Background else OmniColors.TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Module Routing Indicator — Shows which engine was selected.
 */
@Composable
fun ModuleRoutingIndicator(
    moduleName: String,
    moduleKey: String,
    confidence: Double,
    confidenceLevel: String,
    processingTimeMs: Long,
    modifier: Modifier = Modifier
) {
    val moduleColor = OmniColors.getModuleColor(moduleKey)
    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(OmniColors.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, OmniColors.Border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Active indicator dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(moduleColor.copy(alpha = pulseAlpha))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = moduleName,
                    style = MaterialTheme.typography.titleMedium,
                    color = moduleColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Routed to $moduleKey engine",
                    style = MaterialTheme.typography.bodySmall,
                    color = OmniColors.TextTertiary
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            // Confidence badge
            Surface(
                color = when (confidenceLevel) {
                    "HIGH" -> OmniColors.Secondary.copy(alpha = 0.15f)
                    "MEDIUM" -> OmniColors.Warning.copy(alpha = 0.15f)
                    else -> OmniColors.Danger.copy(alpha = 0.15f)
                },
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "${(confidence * 100).toInt()}% $confidenceLevel",
                    style = MaterialTheme.typography.labelSmall,
                    color = when (confidenceLevel) {
                        "HIGH" -> OmniColors.Secondary
                        "MEDIUM" -> OmniColors.Warning
                        else -> OmniColors.Danger
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Text(
                text = "${processingTimeMs}ms",
                style = MaterialTheme.typography.labelSmall,
                color = OmniColors.TextTertiary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Dashboard Card — Reusable styled card for content sections.
 */
@Composable
fun DashboardCard(
    title: String,
    icon: ImageVector,
    iconColor: Color = OmniColors.Primary,
    modifier: Modifier = Modifier,
    headerAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(OmniColors.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, OmniColors.Border, RoundedCornerShape(12.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = OmniColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            headerAction?.invoke()
        }

        Divider(color = OmniColors.Border, thickness = 0.5.dp)

        // Content
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

/**
 * Metric Display — Shows a key metric with label.
 */
@Composable
fun MetricDisplay(
    label: String,
    value: String,
    color: Color = OmniColors.Primary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = OmniColors.TextTertiary
        )
    }
}

/**
 * Status Badge — Shows a colored status indicator.
 */
@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Score Bar — Visual score display with progress bar.
 */
@Composable
fun ScoreBar(
    label: String,
    score: Float,
    maxScore: Float = 100f,
    color: Color = OmniColors.Primary,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary
            )
            Text(
                text = "${score.toInt()}/${maxScore.toInt()}",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = (score / maxScore).coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = OmniColors.SurfaceBright
        )
    }
}

/**
 * Tab Bar — Bottom navigation for the dashboard.
 */
@Composable
fun DashboardTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        "Output" to Icons.Default.Dashboard,
        "Reasoning" to Icons.Default.Psychology,
        "Growth" to Icons.Default.TrendingUp,
        "Logs" to Icons.Default.History,
        "Settings" to Icons.Default.Settings
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(OmniColors.Surface)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEachIndexed { index, (label, icon) ->
            val isSelected = selectedTab == index
            val color by animateColorAsState(
                targetValue = if (isSelected) OmniColors.Primary else OmniColors.TextTertiary,
                label = "tabColor"
            )

            Column(
                modifier = Modifier
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontSize = 10.sp
                )

                if (isSelected) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(OmniColors.Primary, OmniColors.Accent)
                                )
                            )
                    )
                }
            }
        }
    }
}

/**
 * Demo Presentation Bar — One-click trigger for hackathon judging.
 */
@Composable
fun DemoPresentationBar(
    onDemoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val demos = listOf(
        "Coding" to "coding",
        "Cybersec" to "cyber",
        "Resume" to "resume",
        "Startup" to "startup"
    )

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = "DEMO PRESENTATION FLOW",
            style = MaterialTheme.typography.labelSmall,
            color = OmniColors.Accent,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(demos) { (label, type) ->
                Button(
                    onClick = { onDemoClick(type) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OmniColors.SurfaceBright,
                        contentColor = OmniColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        Icons.Default.PlayCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = OmniColors.Primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/**
 * Branded Export Button for Reports.
 */
@Composable
fun ExportButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = OmniColors.Secondary
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Export AI Report", fontWeight = FontWeight.Bold)
    }
}
