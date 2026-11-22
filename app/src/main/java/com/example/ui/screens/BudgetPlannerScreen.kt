package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BudgetExpenseEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.model.AnalysisEngine
import com.example.data.model.BudgetCalculator
import com.example.data.model.BudgetReportPeriod
import com.example.data.model.ExpenseCategory
import com.example.data.model.FinGuardFormatters
import com.example.ui.BudgetPlannerUiState
import com.example.ui.components.AddEditExpenseDialog
import com.example.ui.components.BudgetAlertBanner
import com.example.ui.components.CircularBudgetProgressIndicator
import com.example.ui.components.ContributeGoalDialog
import com.example.ui.components.CreateSavingsGoalDialog
import com.example.ui.components.EditBudgetProfileDialog
import com.example.ui.components.ExpenseDonutChart
import com.example.ui.components.GlassFintechCard
import com.example.ui.components.ResetBudgetDialog
import com.example.ui.components.SafetyGuardBanner
import com.example.ui.components.SimulatedBadge
import com.example.ui.components.SpendingTrendBarChart
import com.example.ui.components.WeeklyMonthlyComparisonStrip
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFinGuardColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetPlannerScreen(
    state: BudgetPlannerUiState,
    onBack: () -> Unit,
    onSaveBudgetSettings: (incomeRaw: String, budgetRaw: String) -> Unit,
    onSaveExpense: (
        expenseId: Long?,
        title: String,
        amountRaw: String,
        category: ExpenseCategory,
        note: String,
        daysAgoOffset: Int,
        preserveDateMillis: Long?
    ) -> Unit,
    onDeleteExpense: (Long) -> Unit,
    onSelectCategoryFilter: (ExpenseCategory?) -> Unit,
    onSelectReportPeriod: (BudgetReportPeriod) -> Unit,
    onCreateSavingsGoal: (title: String, targetRaw: String, initialSavedRaw: String, deadlineDays: Int) -> Unit,
    onContributeGoal: (goalId: Long, amountRaw: String) -> Unit,
    onDeleteSavingsGoal: (Long) -> Unit,
    onToggleGemini: (Boolean) -> Unit,
    onGenerateAdvice: (forceOffline: Boolean) -> Unit,
    onResetToBlank: () -> Unit,
    onRestoreSampleData: () -> Unit,
    onClearMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val ext = LocalFinGuardColors.current
    val metrics = state.metrics
    val activeReport = when (state.selectedReportPeriod) {
        BudgetReportPeriod.WEEKLY -> metrics.weeklyReport
        BudgetReportPeriod.MONTHLY -> metrics.monthlyReport
    }

    var showProfileDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<BudgetExpenseEntity?>(null) }
    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var contributingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("budget_lazy_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("budget_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smart Budget Planner",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Personal & demo expense intelligence (offline Room ledger)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SimulatedBadge(
                        text = if (state.profile?.isSampleData == true) "DEMO SAMPLE" else "LOCAL DATA",
                        tint = ext.neonCyan
                    )
                }
            }

            // Interactive Circular Budget Progress + Key Metrics Dashboard
            item {
                GlassFintechCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = true
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Budget Utilization Gauge",
                                style = MaterialTheme.typography.titleMedium
                            )
                            SimulatedBadge(
                                text = metrics.statusTier.shortBadge,
                                tint = metrics.statusTier.accentColor
                            )
                        }

                        CircularBudgetProgressIndicator(
                            statusTier = metrics.statusTier,
                            usedPercent = metrics.budgetUsedPercent,
                            spentInPaise = metrics.totalExpensesInPaise,
                            budgetInPaise = metrics.monthlyBudgetInPaise,
                            overspentInPaise = metrics.overspentInPaise
                        )
                    }
                }
            }

            // Budget Snapshot: Income, Expenses, Remaining, Net Savings
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BudgetMetricTile(
                            label = "Monthly Income",
                            value = FinGuardFormatters.formatPaise(metrics.monthlyIncomeInPaise),
                            sublabel = if (state.profile?.isSampleData == true) "Sample Profile" else "User Entered",
                            accent = ext.tropicalAqua,
                            testTag = "budget_income_value",
                            modifier = Modifier.weight(1f)
                        )
                        BudgetMetricTile(
                            label = "Total Expenses (30D)",
                            value = FinGuardFormatters.formatPaise(metrics.totalExpensesInPaise),
                            sublabel = "${metrics.monthlyReport.transactionCount} entries",
                            accent = ext.lightCyan,
                            testTag = "budget_expenses_value",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val remainingColor = if (metrics.remainingBudgetInPaise >= 0L) ext.auroraMint else ext.danger
                        BudgetMetricTile(
                            label = if (metrics.overspentInPaise > 0L && metrics.monthlyBudgetInPaise > 0L) {
                                "Overspent Amount"
                            } else {
                                "Remaining Budget"
                            },
                            value = FinGuardFormatters.formatPaise(metrics.remainingBudgetInPaise),
                            sublabel = "Budget: ${FinGuardFormatters.formatPaise(metrics.monthlyBudgetInPaise)}",
                            accent = remainingColor,
                            testTag = "budget_remaining_value",
                            modifier = Modifier.weight(1f)
                        )
                        val savingsColor = if (metrics.netSavingsInPaise >= 0L) ext.luxuryMint else ext.danger
                        BudgetMetricTile(
                            label = "Total Savings",
                            value = FinGuardFormatters.formatPaise(metrics.netSavingsInPaise),
                            sublabel = "${metrics.savingsRatePercent}% savings rate",
                            accent = savingsColor,
                            testTag = "budget_savings_value",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Budget Threshold Alert Banner (5-Tier Dynamic Status + 75%, 90%, 100% Alerts)
            item {
                BudgetAlertBanner(
                    alertLevel = metrics.alertLevel,
                    statusTier = metrics.statusTier,
                    usedPercent = metrics.budgetUsedPercent,
                    spentInPaise = metrics.totalExpensesInPaise,
                    budgetInPaise = metrics.monthlyBudgetInPaise,
                    remainingInPaise = metrics.remainingBudgetInPaise,
                    overspentInPaise = metrics.overspentInPaise
                )
            }

            // Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            editingExpense = null
                            showExpenseDialog = true
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .minimumInteractiveComponentSize()
                            .testTag("budget_add_expense_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Expense")
                    }
                    OutlinedButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("budget_edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Budget")
                    }
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("budget_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RestartAlt,
                            contentDescription = "Reset Budget Data",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Feedback / Error Toast Card
            if (state.feedbackMessage != null || state.errorMessage != null) {
                item {
                    val isError = state.errorMessage != null
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (isError) ext.dangerBg else ext.safeBg,
                        border = BorderStroke(1.dp, if (isError) ext.danger else ext.safe)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.errorMessage ?: state.feedbackMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onClearMessages) { Text("Dismiss") }
                        }
                    }
                }
            }

            // Interactive Category Donut & Breakdown
            item {
                GlassFintechCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Category Spending Analysis",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Tap a slice or chip to filter expenses below",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (state.selectedCategoryFilter != null) {
                            TextButton(
                                onClick = { onSelectCategoryFilter(null) },
                                modifier = Modifier.testTag("budget_clear_cat_filter")
                            ) {
                                Text("Show All")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    ExpenseDonutChart(
                        breakdown = metrics.categoryBreakdown,
                        totalExpensesInPaise = metrics.totalExpensesInPaise,
                        selectedCategory = state.selectedCategoryFilter,
                        onSelectCategory = { cat -> onSelectCategoryFilter(cat) }
                    )
                }
            }

            // Weekly & Monthly Financial Reports
            item {
                GlassFintechCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Insights,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Financial Reports",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        BudgetReportPeriod.entries.forEachIndexed { index, period ->
                            val tag = if (period == BudgetReportPeriod.WEEKLY) {
                                "report_period_weekly"
                            } else {
                                "report_period_monthly"
                            }
                            SegmentedButton(
                                selected = state.selectedReportPeriod == period,
                                onClick = { onSelectReportPeriod(period) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = BudgetReportPeriod.entries.size
                                ),
                                modifier = Modifier.testTag(tag)
                            ) {
                                Text(period.label)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = activeReport.periodLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FinGuardFormatters.formatPaise(activeReport.totalSpentInPaise),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("report_total_spent")
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Daily Average",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FinGuardFormatters.formatPaise(activeReport.dailyAverageInPaise),
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = ext.neonCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    val targetPerBar = when (state.selectedReportPeriod) {
                        BudgetReportPeriod.WEEKLY -> (metrics.monthlyBudgetInPaise / 30L).coerceAtLeast(0L)
                        BudgetReportPeriod.MONTHLY -> (metrics.monthlyBudgetInPaise / 4L).coerceAtLeast(0L)
                    }
                    SpendingTrendBarChart(
                        bars = activeReport.barPoints,
                        targetLineInPaise = targetPerBar
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    WeeklyMonthlyComparisonStrip(
                        weeklyReport = metrics.weeklyReport,
                        monthlyReport = metrics.monthlyReport
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Entries: ${activeReport.transactionCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Top: ${activeReport.topCategory?.label ?: "None"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.champagneGold
                        )
                    }
                }
            }

            // Savings Goals Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Savings,
                            contentDescription = null,
                            tint = ext.safe
                        )
                        Text(
                            text = "Savings Goals (${state.goals.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    OutlinedButton(
                        onClick = { showCreateGoalDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("budget_add_goal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Goal")
                    }
                }
            }

            if (state.goals.isEmpty()) {
                item {
                    GlassFintechCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No savings goals yet. Tap 'New Goal' to set a target amount and deadline.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(state.goals, key = { "goal_${it.id}" }) { goal ->
                    SavingsGoalCard(
                        goal = goal,
                        onContribute = { contributingGoal = goal },
                        onDelete = { onDeleteSavingsGoal(goal.id) }
                    )
                }
            }

            // Gemini AI / Offline Budget Coach
            item {
                GlassFintechCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = true
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Column {
                                Text(
                                    text = "Smart Budget Coach",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (state.useGeminiCoach) {
                                        "Gemini AI + Offline Rule Fallback"
                                    } else {
                                        "Offline Rule-Based Coach"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = state.useGeminiCoach,
                            onCheckedChange = onToggleGemini,
                            modifier = Modifier.testTag("budget_ai_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onGenerateAdvice(false) },
                        enabled = !state.isLoadingAdvice,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("budget_get_advice_button")
                    ) {
                        if (state.isLoadingAdvice) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing Income & Expenses...")
                        } else {
                            Text(
                                if (state.useGeminiCoach) {
                                    "Generate Personalized Budget Advice"
                                } else {
                                    "Generate Offline Budget Advice"
                                }
                            )
                        }
                    }

                    state.advice?.let { advice ->
                        Spacer(modifier = Modifier.height(12.dp))
                        if (!advice.fallbackNotice.isNullOrBlank()) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = ext.cautionBg,
                                border = BorderStroke(1.dp, ext.caution.copy(alpha = 0.5f)),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = advice.fallbackNotice,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SimulatedBadge(
                                text = if (advice.engineUsed == AnalysisEngine.GEMINI_AI) {
                                    "GEMINI AI COACH"
                                } else {
                                    "OFFLINE COACH"
                                },
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Focus: ${advice.topFocusArea}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.neonCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = advice.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        advice.suggestions.forEach { tip ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("•", color = MaterialTheme.colorScheme.tertiary)
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Advisory Notice: Suggestions are informational only and based solely on locally entered figures. Never share real banking credentials, PINs, or OTPs.",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.mutedText
                        )
                    }
                }
            }

            // Daily Expenses Ledger Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.selectedCategoryFilter != null) {
                            "${state.selectedCategoryFilter.label} Expenses (${state.filteredExpenses.size})"
                        } else {
                            "Daily Expenses (${state.filteredExpenses.size})"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (state.selectedCategoryFilter != null) {
                        TextButton(onClick = { onSelectCategoryFilter(null) }) {
                            Text("Clear Filter")
                        }
                    }
                }
            }

            if (state.filteredExpenses.isEmpty()) {
                item {
                    GlassFintechCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No expenses recorded yet. Tap 'Add Expense' above to log daily spending.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(state.filteredExpenses, key = { "exp_${it.id}" }) { expense ->
                    BudgetExpenseRowCard(
                        expense = expense,
                        onEdit = {
                            editingExpense = expense
                            showExpenseDialog = true
                        },
                        onDelete = { onDeleteExpense(expense.id) }
                    )
                }
            }

            item {
                SafetyGuardBanner(
                    text = "Local Ledger Guarantee: User-entered & demonstration budget data only. Never connects to real bank accounts or asks for UPI PINs, OTPs, or passwords."
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showProfileDialog) {
        EditBudgetProfileDialog(
            currentIncomeInPaise = metrics.monthlyIncomeInPaise,
            currentBudgetInPaise = metrics.monthlyBudgetInPaise,
            onDismiss = { showProfileDialog = false },
            onSave = { inc, bud ->
                onSaveBudgetSettings(inc, bud)
                showProfileDialog = false
            }
        )
    }

    if (showExpenseDialog) {
        AddEditExpenseDialog(
            existingExpense = editingExpense,
            onDismiss = {
                showExpenseDialog = false
                editingExpense = null
            },
            onSave = { id, title, amt, cat, note, offset, preserve ->
                onSaveExpense(id, title, amt, cat, note, offset, preserve)
                showExpenseDialog = false
                editingExpense = null
            }
        )
    }

    if (showCreateGoalDialog) {
        CreateSavingsGoalDialog(
            onDismiss = { showCreateGoalDialog = false },
            onCreate = { title, target, saved, days ->
                onCreateSavingsGoal(title, target, saved, days)
                showCreateGoalDialog = false
            }
        )
    }

    contributingGoal?.let { goal ->
        ContributeGoalDialog(
            goal = goal,
            onDismiss = { contributingGoal = null },
            onContribute = { amount ->
                onContributeGoal(goal.id, amount)
                contributingGoal = null
            }
        )
    }

    if (showResetDialog) {
        ResetBudgetDialog(
            onDismiss = { showResetDialog = false },
            onClearToBlank = {
                onResetToBlank()
                showResetDialog = false
            },
            onRestoreSample = {
                onRestoreSampleData()
                showResetDialog = false
            }
        )
    }
}

@Composable
private fun BudgetMetricTile(
    label: String,
    value: String,
    sublabel: String,
    accent: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    GlassFintechCard(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(testTag)
        )
        Text(
            text = sublabel,
            style = MaterialTheme.typography.bodySmall,
            color = LocalFinGuardColors.current.mutedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SavingsGoalCard(
    goal: SavingsGoalEntity,
    onContribute: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = LocalFinGuardColors.current
    val progress = BudgetCalculator.goalProgressFraction(goal)
    val daysLeft = BudgetCalculator.goalDaysRemaining(goal)
    val isCompleted = goal.savedAmountInPaise >= goal.targetAmountInPaise

    GlassFintechCard(
        goldAccent = isCompleted,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_card_${goal.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Flag,
                    contentDescription = null,
                    tint = if (isCompleted) ext.champagneGold else ext.emerald,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isCompleted) "Milestone Achieved!" else "$daysLeft days remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCompleted) ext.champagneGold else ext.mutedText
                    )
                }
            }
            if (isCompleted) {
                SimulatedBadge(
                    text = "ACHIEVED",
                    tint = ext.champagneGold
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("goal_delete_${goal.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete goal ${goal.title}",
                    tint = ext.mutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (isCompleted) ext.champagneGold else ext.emerald,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${FinGuardFormatters.formatPaise(goal.savedAmountInPaise)} / ${FinGuardFormatters.formatPaise(goal.targetAmountInPaise)}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = JetBrainsMonoFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onContribute,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) ext.goldBg else ext.safeBg,
                    contentColor = if (isCompleted) ext.softGold else ext.luxuryMint
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("goal_contribute_${goal.id}")
            ) {
                Text("+ Add Savings", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun BudgetExpenseRowCard(
    expense: BudgetExpenseEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = LocalFinGuardColors.current
    val category = ExpenseCategory.fromRaw(expense.category)

    GlassFintechCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_card_${expense.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(category.chartColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = category.label,
                    tint = category.chartColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(category.label)
                        append(" • ")
                        append(FinGuardFormatters.formatShortDate(expense.expenseDateMillis))
                        if (expense.note.isNotBlank()) {
                            append(" • ")
                            append(expense.note)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = FinGuardFormatters.formatPaise(expense.amountInPaise),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMonoFontFamily,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("edit_expense_${expense.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit ${expense.title}",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_expense_${expense.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete ${expense.title}",
                    tint = ext.mutedText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
