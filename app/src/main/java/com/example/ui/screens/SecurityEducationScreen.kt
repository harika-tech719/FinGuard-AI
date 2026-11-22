package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DemoCatalog
import com.example.data.model.EducationCategory
import com.example.data.model.EducationLesson
import com.example.data.model.SecurityQuizQuestion
import com.example.ui.EducationScreenState
import com.example.ui.components.GlassFintechCard
import com.example.ui.components.SimulatedBadge
import com.example.ui.theme.LocalFinGuardColors

@Composable
fun SecurityEducationScreen(
    state: EducationScreenState,
    onSelectCategory: (EducationCategory?) -> Unit,
    onToggleLessonCompleted: (String) -> Unit,
    onSelectQuizAnswer: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalFinGuardColors.current
    val filteredLessons = remember(state.selectedCategory) {
        if (state.selectedCategory == null) {
            DemoCatalog.educationLessons
        } else {
            DemoCatalog.educationLessons.filter { it.category == state.selectedCategory }
        }
    }
    val totalLessons = DemoCatalog.educationLessons.size
    val completedCount = state.completedLessonIds.size

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("education_lazy_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Banner
            item {
                val allPlaybooksDone = totalLessons > 0 && completedCount == totalLessons
                val shape = MaterialTheme.shapes.extraLarge
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(shape),
                    shape = shape,
                    border = BorderStroke(
                        1.dp,
                        if (allPlaybooksDone) ext.champagneGold.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_education_banner),
                            contentDescription = "Security Academy Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            ext.obsidian.copy(alpha = 0.95f),
                                            ext.deepOceanBlue.copy(alpha = 0.72f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SimulatedBadge(
                                text = if (allPlaybooksDone) "SECURITY ACADEMY • MASTERED" else "SECURITY ACADEMY",
                                tint = if (allPlaybooksDone) ext.champagneGold else ext.tropicalAqua
                            )
                            Column {
                                Text(
                                    text = "Fraud Defense Playbook",
                                    style = MaterialTheme.typography.headlineLarge
                                )
                                Text(
                                    text = "Master OTP safety, UPI fraud prevention, and phishing defense",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "$completedCount of $totalLessons Playbooks Reviewed",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (allPlaybooksDone) ext.champagneGold else ext.auroraMint
                                )
                                LinearProgressIndicator(
                                    progress = { completedCount / totalLessons.toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.extraSmall),
                                    color = if (allPlaybooksDone) ext.champagneGold else ext.auroraMint,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            }
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.selectedCategory == null,
                            onClick = { onSelectCategory(null) },
                            label = { Text("All Topics") }
                        )
                    }
                    items(EducationCategory.entries) { category ->
                        FilterChip(
                            selected = state.selectedCategory == category,
                            onClick = {
                                onSelectCategory(if (state.selectedCategory == category) null else category)
                            },
                            label = { Text(category.title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("edu_cat_${category.name.lowercase()}")
                        )
                    }
                }
            }

            items(filteredLessons, key = { it.id }) { lesson ->
                LessonCard(
                    lesson = lesson,
                    isCompleted = lesson.id in state.completedLessonIds,
                    onToggleCompleted = { onToggleLessonCompleted(lesson.id) }
                )
            }

            // Interactive Quiz Section
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Quiz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Spot the Scam — Quick Check",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            items(DemoCatalog.quizQuestions, key = { it.id }) { question ->
                QuizQuestionCard(
                    question = question,
                    selectedOption = state.selectedQuizAnswers[question.id],
                    onSelectOption = { idx -> onSelectQuizAnswer(question.id, idx) }
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun LessonCard(
    lesson: EducationLesson,
    isCompleted: Boolean,
    onToggleCompleted: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val ext = LocalFinGuardColors.current

    GlassFintechCard(
        accentBorder = isCompleted,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lesson_card_${lesson.id}"),
        onClick = { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = lesson.category.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse lesson" else "Expand lesson"
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = lesson.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = ext.dangerBg,
                    border = BorderStroke(1.dp, ext.danger.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = null,
                                tint = ext.danger,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Scam Red Flag Example",
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.danger
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lesson.redFlagExample,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = ext.safeBg,
                    border = BorderStroke(1.dp, ext.safe.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = ext.safe,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Safe Practice",
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.safe
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lesson.safePractice,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Takeaway: ${lesson.keyTakeaway}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onToggleCompleted,
                        modifier = Modifier.testTag("toggle_lesson_${lesson.id}")
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isCompleted) ext.safe else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isCompleted) "Reviewed" else "Mark Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizQuestionCard(
    question: SecurityQuizQuestion,
    selectedOption: Int?,
    onSelectOption: (Int) -> Unit
) {
    val ext = LocalFinGuardColors.current
    GlassFintechCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = question.category.title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = question.question,
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            question.options.forEachIndexed { idx, option ->
                val isSelected = selectedOption == idx
                val isCorrect = idx == question.correctIndex
                val bgColor = when {
                    selectedOption == null -> MaterialTheme.colorScheme.surfaceContainerHigh
                    isSelected && isCorrect -> ext.safeBg
                    isSelected && !isCorrect -> ext.dangerBg
                    isCorrect -> ext.safeBg.copy(alpha = 0.45f)
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                }
                val borderColor = when {
                    selectedOption == null -> MaterialTheme.colorScheme.outlineVariant
                    isSelected && isCorrect -> ext.safe
                    isSelected && !isCorrect -> ext.danger
                    isCorrect -> ext.safe.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.outlineVariant
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onSelectOption(idx) }
                        .testTag("quiz_${question.id}_opt_$idx"),
                    shape = MaterialTheme.shapes.small,
                    color = bgColor,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        AnimatedVisibility(visible = selectedOption != null) {
            val correct = selectedOption == question.correctIndex
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                shape = MaterialTheme.shapes.small,
                color = if (correct) ext.safeBg else ext.cautionBg
            ) {
                Text(
                    text = (if (correct) "Correct: " else "Good check: ") + question.explanation,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}
