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
import com.omniagent.app.core.model.ResumeData

@Composable
fun ResumeBuilderForm(viewModel: OmniAgentViewModel, onBack: () -> Unit) {
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 6
    val scrollState = rememberScrollState()
    val resumeData by viewModel.careerResumeData.collectAsState()

    // Local state for form fields
    var fullName by remember { mutableStateOf(resumeData.fullName) }
    var email by remember { mutableStateOf(resumeData.email) }
    var phone by remember { mutableStateOf(resumeData.phone) }
    var jobTitle by remember { mutableStateOf(resumeData.jobTitle) }
    var company by remember { mutableStateOf(resumeData.company) }
    var experience by remember { mutableStateOf(resumeData.experienceDescription) }
    var education by remember { mutableStateOf(resumeData.education) }
    var skills by remember { mutableStateOf(resumeData.skills) }
    var templateId by remember { mutableStateOf(resumeData.templateId) }

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
                1 -> PersonalInfoForm(
                    fullName, { fullName = it },
                    email, { email = it },
                    phone, { phone = it }
                )
                2 -> WorkExperienceForm(
                    jobTitle, { jobTitle = it },
                    company, { company = it },
                    experience, { experience = it }
                )
                3 -> EducationForm(education, { education = it })
                4 -> SkillsForm(skills, { skills = it })
                5 -> TemplateSelectionForm(templateId, { templateId = it })
                6 -> ResumePreviewScreen(
                    ResumeData(fullName, email, phone, jobTitle, company, experience, education, skills, templateId)
                )
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
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OmniColors.TextSecondary)
                ) {
                    Text("Cancel")
                }
            }

            Button(
                onClick = { 
                    if (currentStep < totalSteps) {
                        currentStep++
                    } else {
                        val finalData = ResumeData(
                            fullName, email, phone, jobTitle, company, experience, education, skills, templateId
                        )
                        viewModel.updateResumeData(finalData)
                        viewModel.runCareerAudit(finalData.toMarkdown())
                        onBack() // Target HUB to see results
                    }
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
fun PersonalInfoForm(
    name: String, onNameChange: (String) -> Unit,
    email: String, onEmailChange: (String) -> Unit,
    phone: String, onPhoneChange: (String) -> Unit
) {
    Column {
        Text("Personal Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OmniColors.TextPrimary)
        Text("Let's start with who you are.", style = MaterialTheme.typography.bodyMedium, color = OmniColors.TextTertiary)
        Spacer(modifier = Modifier.height(24.dp))
        
        CustomTextField(label = "Full Name", icon = Icons.Default.Person, value = name, onValueChange = onNameChange)
        Spacer(modifier = Modifier.height(16.dp))
        CustomTextField(label = "Email Address", icon = Icons.Default.Email, value = email, onValueChange = onEmailChange)
        Spacer(modifier = Modifier.height(16.dp))
        CustomTextField(label = "Phone Number", icon = Icons.Default.Phone, value = phone, onValueChange = onPhoneChange)
    }
}

@Composable
fun WorkExperienceForm(
    title: String, onTitleChange: (String) -> Unit,
    company: String, onCompanyChange: (String) -> Unit,
    desc: String, onDescChange: (String) -> Unit
) {
    Column {
        Text("Work Experience", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OmniColors.TextPrimary)
        Text("The AI will help rewrite these bullet points later.", style = MaterialTheme.typography.bodyMedium, color = OmniColors.TextTertiary)
        Spacer(modifier = Modifier.height(24.dp))
        
        CustomTextField(label = "Job Title", icon = Icons.Default.Work, value = title, onValueChange = onTitleChange)
        Spacer(modifier = Modifier.height(16.dp))
        CustomTextField(label = "Company Name", icon = Icons.Default.Business, value = company, onValueChange = onCompanyChange)
        Spacer(modifier = Modifier.height(16.dp))
        CustomTextField(label = "Key Achievements", icon = Icons.Default.List, isMultiLine = true, value = desc, onValueChange = onDescChange)
    }
}

@Composable
fun EducationForm(edu: String, onEduChange: (String) -> Unit) {
    Column {
        Text("Education", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OmniColors.TextPrimary)
        Text("Where did you study?", style = MaterialTheme.typography.bodyMedium, color = OmniColors.TextTertiary)
        Spacer(modifier = Modifier.height(24.dp))
        
        CustomTextField(label = "Major/Degree/School", icon = Icons.Default.School, isMultiLine = true, value = edu, onValueChange = onEduChange)
    }
 }

@Composable
fun SkillsForm(skills: String, onSkillsChange: (String) -> Unit) {
    Column {
        Text("Skills", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = OmniColors.TextPrimary)
        Text("Comma separated list of tech & soft skills.", style = MaterialTheme.typography.bodyMedium, color = OmniColors.TextTertiary)
        Spacer(modifier = Modifier.height(24.dp))
        
        CustomTextField(label = "Skills (e.g. Python, Java, Leadership)", icon = Icons.Default.Extension, isMultiLine = true, value = skills, onValueChange = onSkillsChange)
    }
 }

@Composable
fun CustomTextField(label: String, icon: ImageVector, isMultiLine: Boolean = false, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
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
