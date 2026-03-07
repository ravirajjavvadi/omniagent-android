package com.omniagent.app.ui.features.career

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniagent.app.ui.theme.OmniColors
import com.omniagent.app.viewmodel.OmniAgentViewModel

@Composable
fun ResumeBuilderForm(viewModel: OmniAgentViewModel) {
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 4
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.Background)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Step Indicator
        StepIndicator(currentStep, totalSteps)
        
        Spacer(modifier = Modifier.height(32.dp))

        // Form Content with Animation
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                }.using(SizeTransform(clip = false))
            },
            label = "StepTransition"
        ) { step ->
            when (step) {
                1 -> PersonalInfoForm()
                2 -> WorkExperienceForm()
                3 -> EducationForm()
                4 -> SkillsForm()
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(32.dp))

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentStep > 1) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OmniColors.TextSecondary)
                ) {
                    Text("Back")
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }

            Button(
                onClick = { 
                    if (currentStep < totalSteps) currentStep++ 
                    else { /* Finalize & Save */ }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OmniColors.Primary)
            ) {
                Text(if (currentStep == totalSteps) "Finish Beast Resume" else "Next Step")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun StepIndicator(current: Int, total: Int) {
    Column {
        Text(
            text = "Step $current of $total",
            style = MaterialTheme.typography.labelLarge,
            color = OmniColors.Accent,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 1..total) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (i <= current) OmniColors.Accent else OmniColors.Surface,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun PersonalInfoForm() {
    Column {
        Text("Personal Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OmniColors.TextPrimary)
        Text("Let's start with who you are.", style = MaterialTheme.typography.bodyMedium, color = OmniColors.TextTertiary)
        Spacer(modifier = Modifier.height(24.dp))
        
        CustomTextField(label = "Full Name", icon = Icons.Default.Person)
        Spacer(modifier = Modifier.height(16.dp))
        CustomTextField(label = "Email Address", icon = Icons.Default.Email)
        Spacer(modifier = Modifier.height(16.dp))
        CustomTextField(label = "Phone Number", icon = Icons.Default.Phone)
    }
}

@Composable
fun WorkExperienceForm() {
    Column {
        Text("Work Experience", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OmniColors.TextPrimary)
        Text("The AI will help rewrite these bullet points later.", style = MaterialTheme.typography.bodyMedium, color = OmniColors.TextTertiary)
        Spacer(modifier = Modifier.height(24.dp))
        
        CustomTextField(label = "Job Title", icon = Icons.Default.Work)
        Spacer(modifier = Modifier.height(16.dp))
        CustomTextField(label = "Company Name", icon = Icons.Default.Business)
        Spacer(modifier = Modifier.height(16.dp))
        CustomTextField(label = "Key Achievements", icon = Icons.Default.List, isMultiLine = true)
    }
}

@Composable
fun EducationForm() { /* Similar Structure */ }
@Composable
fun SkillsForm() { /* Similar Structure */ }

@Composable
fun CustomTextField(label: String, icon: ImageVector, isMultiLine: Boolean = false) {
    var text by remember { mutableStateOf("") }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = OmniColors.Primary) },
        modifier = Modifier.fillMaxWidth().then(if (isMultiLine) Modifier.height(120.dp) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = OmniColors.Surface,
            unfocusedContainerColor = OmniColors.Surface,
            focusedTextColor = OmniColors.TextPrimary,
            unfocusedTextColor = OmniColors.TextPrimary
        )
    )
}
