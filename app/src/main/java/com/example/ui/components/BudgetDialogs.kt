package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.BudgetExpenseEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.FinGuardFormatters
import com.example.ui.theme.LocalFinGuardColors

@Composable
fun EditBudgetProfileDialog(
    currentIncomeInPaise: Long,
    currentBudgetInPaise: Long,
    onDismiss: () -> Unit,
    onSave: (incomeRaw: String, budgetRaw: String) -> Unit
) {
    var income by remember {
        mutableStateOf(
            if (currentIncomeInPaise > 0L) (currentIncomeInPaise / 100L).toString() else ""
        )
    }
    var budget by remember {
        mutableStateOf(
            if (currentBudgetInPaise > 0L) (currentBudgetInPaise / 100L).toString() else ""
        )
    }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("Monthly Income & Budget") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Enter personal or demo values stored locally. FinGuard never connects to external bank accounts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = income,
                    onValueChange = {
                        income = it
                        localError = null
                    },
                    label = { Text("Monthly Income (₹)") },
                    placeholder = { Text("e.g. 45000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_income_input")
                )
                OutlinedTextField(
                    value = budget,
                    onValueChange = {
                        budget = it
                        localError = null
                    },
                    label = { Text("Monthly Spending Budget (₹)") },
                    placeholder = { Text("e.g. 25000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input")
                )
                localError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalFinGuardColors.current.danger
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val inc = FinGuardFormatters.parseNonNegativePaiseInput(income)
                    val bud = FinGuardFormatters.parsePaiseInput(budget)
                    when {
                        inc == null -> localError = "Enter a valid monthly income (0 or higher)."
                        bud == null || bud <= 0L -> localError = "Enter a monthly budget limit greater than ₹0."
                        else -> onSave(income, budget)
                    }
                },
                modifier = Modifier.testTag("confirm_save_budget_profile")
            ) {
                Text("Save Budget")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditExpenseDialog(
    existingExpense: BudgetExpenseEntity?,
    onDismiss: () -> Unit,
    onSave: (
        expenseId: Long?,
        title: String,
        amountRaw: String,
        category: ExpenseCategory,
        note: String,
        daysAgoOffset: Int,
        preserveDateMillis: Long?
    ) -> Unit
) {
    var title by remember(existingExpense) { mutableStateOf(existingExpense?.title.orEmpty()) }
    var amount by remember(existingExpense) {
        mutableStateOf(
            if (existingExpense != null) {
                if (existingExpense.amountInPaise % 100L == 0L) {
                    (existingExpense.amountInPaise / 100L).toString()
                } else {
                    String.format(java.util.Locale.US, "%.2f", existingExpense.amountInPaise / 100.0)
                }
            } else {
                ""
            }
        )
    }
    var category by remember(existingExpense) {
        mutableStateOf(
            existingExpense?.let { ExpenseCategory.fromRaw(it.category) } ?: ExpenseCategory.FOOD
        )
    }
    var note by remember(existingExpense) { mutableStateOf(existingExpense?.note.orEmpty()) }
    // -1 means keep existing expenseDateMillis when editing unless user picks another day preset
    var daysAgoOffset by remember(existingExpense) {
        mutableIntStateOf(if (existingExpense != null) -1 else 0)
    }
    var localError by remember { mutableStateOf<String?>(null) }

    val dateOptions = listOf(
        0 to "Today",
        1 to "Yesterday",
        3 to "3d ago",
        7 to "7d ago",
        14 to "14d ago"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Text(if (existingExpense == null) "Add Daily Expense" else "Edit Expense")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        localError = null
                    },
                    label = { Text("Expense Title") },
                    placeholder = { Text("e.g. Fresh Groceries") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input")
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        localError = null
                    },
                    label = { Text("Amount (₹)") },
                    placeholder = { Text("e.g. 450") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ExpenseCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = cat.chartColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("expense_cat_${cat.name.lowercase()}")
                        )
                    }
                }

                Text(
                    text = "Date",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (existingExpense != null) {
                        FilterChip(
                            selected = daysAgoOffset == -1,
                            onClick = { daysAgoOffset = -1 },
                            label = { Text("Keep (${FinGuardFormatters.formatShortDate(existingExpense.expenseDateMillis)})") }
                        )
                    }
                    dateOptions.forEach { (offset, label) ->
                        FilterChip(
                            selected = daysAgoOffset == offset,
                            onClick = { daysAgoOffset = offset },
                            label = { Text(label) },
                            modifier = Modifier.testTag("expense_date_$offset")
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    placeholder = { Text("Weekly meal prep") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_note_input")
                )

                localError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalFinGuardColors.current.danger
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paise = FinGuardFormatters.parsePaiseInput(amount)
                    when {
                        title.trim().isEmpty() -> localError = "Enter an expense title."
                        paise == null || paise <= 0L -> localError = "Enter a valid amount greater than ₹0."
                        else -> onSave(
                            existingExpense?.id,
                            title,
                            amount,
                            category,
                            note,
                            daysAgoOffset,
                            existingExpense?.expenseDateMillis
                        )
                    }
                },
                modifier = Modifier.testTag("confirm_save_expense")
            ) {
                Text(if (existingExpense == null) "Add Expense" else "Update Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateSavingsGoalDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, targetRaw: String, initialSavedRaw: String, deadlineDays: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var initialSaved by remember { mutableStateOf("0") }
    var deadlineDays by remember { mutableIntStateOf(90) }
    var localError by remember { mutableStateOf<String?>(null) }

    val deadlinePresets = listOf(
        30 to "30 Days",
        60 to "60 Days",
        90 to "90 Days",
        180 to "6 Months",
        365 to "1 Year"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("Create Savings Goal") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        localError = null
                    },
                    label = { Text("Goal Name") },
                    placeholder = { Text("e.g. Emergency Reserve") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_title_input")
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = {
                        target = it
                        localError = null
                    },
                    label = { Text("Target Amount (₹)") },
                    placeholder = { Text("e.g. 20000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_target_input")
                )
                OutlinedTextField(
                    value = initialSaved,
                    onValueChange = {
                        initialSaved = it
                        localError = null
                    },
                    label = { Text("Already Saved (₹)") },
                    placeholder = { Text("0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_saved_input")
                )
                Text(
                    text = "Target Deadline",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    deadlinePresets.forEach { (days, label) ->
                        FilterChip(
                            selected = deadlineDays == days,
                            onClick = { deadlineDays = days },
                            label = { Text(label) },
                            modifier = Modifier.testTag("goal_deadline_$days")
                        )
                    }
                }
                localError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalFinGuardColors.current.danger
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetPaise = FinGuardFormatters.parsePaiseInput(target)
                    val savedPaise = FinGuardFormatters.parseNonNegativePaiseInput(initialSaved.ifBlank { "0" })
                    when {
                        title.trim().isEmpty() -> localError = "Enter a savings goal name."
                        targetPaise == null || targetPaise <= 0L -> localError = "Enter a target amount greater than ₹0."
                        savedPaise == null -> localError = "Enter a valid initial saved amount (0 or higher)."
                        else -> onCreate(title, target, initialSaved.ifBlank { "0" }, deadlineDays)
                    }
                },
                modifier = Modifier.testTag("confirm_create_goal")
            ) {
                Text("Create Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ContributeGoalDialog(
    goal: SavingsGoalEntity,
    onDismiss: () -> Unit,
    onContribute: (amountRaw: String) -> Unit
) {
    var amount by remember { mutableStateOf("1000") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("Add to '${goal.title}'") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Saved ${FinGuardFormatters.formatPaise(goal.savedAmountInPaise)} of ${FinGuardFormatters.formatPaise(goal.targetAmountInPaise)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("500", "1000", "2500").forEach { preset ->
                        FilterChip(
                            selected = amount == preset,
                            onClick = {
                                amount = preset
                                localError = null
                            },
                            label = { Text("+₹$preset") }
                        )
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        localError = null
                    },
                    label = { Text("Contribution Amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_contribute_amount_input")
                )
                localError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalFinGuardColors.current.danger
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paise = FinGuardFormatters.parsePaiseInput(amount)
                    if (paise == null || paise <= 0L) {
                        localError = "Enter an amount greater than ₹0."
                    } else {
                        onContribute(amount)
                    }
                },
                modifier = Modifier.testTag("confirm_contribute_goal")
            ) {
                Text("Add Savings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ResetBudgetDialog(
    onDismiss: () -> Unit,
    onClearToBlank: () -> Unit,
    onRestoreSample: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("Reset Budget Data") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Choose whether to clear all demonstration/personal entries to ₹0 or restore the pre-populated sample dataset (₹45,000 income, ₹25,000 budget, 8 expenses & 2 goals).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onRestoreSample,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_restore_sample_budget")
                ) {
                    Text("Restore Sample Demo Data")
                }
                Button(
                    onClick = onClearToBlank,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalFinGuardColors.current.danger
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_clear_budget")
                ) {
                    Text("Clear All (Start Fresh)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
