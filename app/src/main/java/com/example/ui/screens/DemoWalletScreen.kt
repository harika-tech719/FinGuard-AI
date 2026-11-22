package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.WalletAccountEntity
import com.example.data.local.WalletTransactionEntity
import com.example.data.model.FinGuardFormatters
import com.example.ui.WalletFilter
import com.example.ui.WalletUiState
import com.example.ui.components.GlassFintechCard
import com.example.ui.components.SimulatedBadge
import com.example.ui.components.SimulatedWalletReceiptDialog
import com.example.ui.components.TransactionDetailBottomSheet
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.LocalFinGuardColors
import java.util.UUID

@Composable
fun DemoWalletScreen(
    state: WalletUiState,
    onSelectAccount: (String) -> Unit,
    onAddDemoMoney: (targetVpa: String, amount: String, note: String, requestKey: String) -> Unit,
    onTransferBetweenAccounts: (sender: WalletAccountEntity, receiver: WalletAccountEntity, amount: String, note: String, requestKey: String) -> Unit,
    onSelectFilter: (WalletFilter) -> Unit,
    onSelectTransaction: (WalletTransactionEntity?) -> Unit,
    onDismissReceipt: () -> Unit,
    onResetWallet: () -> Unit,
    onClearMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    var addDialogRequestKey by remember { mutableStateOf<String?>(null) }
    var transferDialogRequestKey by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    val ext = LocalFinGuardColors.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Demo Wallet",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "Multi-account offline fictional ledger",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SimulatedBadge(text = "SIMULATED BALANCE")
                }
            }

            // Active Wallet Vault Card
            item {
                val shape = MaterialTheme.shapes.extraLarge
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shape,
                    shadowElevation = 4.dp,
                    border = BorderStroke(
                        1.2.dp,
                        Brush.linearGradient(
                            listOf(
                                ext.tropicalAqua.copy(alpha = 0.85f),
                                ext.oceanBlue.copy(alpha = 0.65f),
                                ext.auroraMint.copy(alpha = 0.45f)
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        ext.deepOceanBlue,
                                        ext.oceanBlue.copy(alpha = 0.62f),
                                        ext.tropicalAqua.copy(alpha = 0.24f)
                                    )
                                )
                            )
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = state.ownerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${state.vpa} • ${state.selectedAccount?.bankLabel ?: "FinGuard Sandbox"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.auroraMint,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = ext.auroraMint,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Simulated Fictional Balance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FinGuardFormatters.formatPaise(state.balanceInPaise),
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("wallet_balance_text")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onClearMessages()
                                    addDialogRequestKey = UUID.randomUUID().toString()
                                },
                                enabled = !state.isProcessing,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("wallet_add_money_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Demo")
                            }
                            Button(
                                onClick = {
                                    onClearMessages()
                                    transferDialogRequestKey = UUID.randomUUID().toString()
                                },
                                enabled = !state.isProcessing && state.accounts.size >= 2,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("wallet_transfer_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Transfer")
                            }
                            OutlinedButton(
                                onClick = {
                                    onClearMessages()
                                    showResetDialog = true
                                },
                                enabled = !state.isProcessing,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("wallet_reset_button"),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Icon(
                                    Icons.Filled.RestartAlt,
                                    contentDescription = "Reset Demo Wallet",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Fictional Accounts Directory Strip (live Room balances!)
            if (state.accounts.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Fictional Sandbox Accounts (Tap to Inspect)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.accounts, key = { it.vpa }) { acct ->
                                val isSelected = acct.vpa == state.selectedAccount?.vpa
                                Surface(
                                    modifier = Modifier
                                        .width(196.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable { onSelectAccount(acct.vpa) }
                                        .testTag("account_card_${acct.vpa}"),
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.surfaceContainer,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = acct.initials,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = acct.ownerName,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = FinGuardFormatters.formatPaise(acct.balanceInPaise),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                        Text(
                                            text = "${acct.vpa} • SIMULATED",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ext.mutedText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Feedback / Error banners
            if (state.feedbackMessage != null || state.errorMessage != null) {
                item {
                    val isError = state.errorMessage != null
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = if (isError) ext.dangerBg else ext.safeBg,
                        border = BorderStroke(
                            1.dp,
                            (if (isError) ext.danger else ext.safe).copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isError) ext.danger else ext.safe
                                )
                                Text(
                                    text = state.errorMessage ?: state.feedbackMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TextButton(onClick = onClearMessages) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            // Filter Chips
            item {
                Text(
                    text = "Simulated Ledger History",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(WalletFilter.entries) { filter ->
                        FilterChip(
                            selected = state.selectedFilter == filter,
                            onClick = { onSelectFilter(filter) },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.tertiary
                            ),
                            modifier = Modifier.testTag("wallet_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            if (state.transactions.isEmpty()) {
                item {
                    GlassFintechCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = ext.mutedText,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "No transactions in this filter",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Add fictional demo money or transfer between accounts to record receipts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.mutedText
                            )
                        }
                    }
                }
            } else {
                items(state.transactions, key = { it.id }) { tx ->
                    TransactionRowCard(
                        tx = tx,
                        onClick = { onSelectTransaction(tx) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    addDialogRequestKey?.let { reqKey ->
        AddDemoMoneyDialog(
            targetAccountName = state.ownerName,
            targetVpa = state.vpa,
            onDismiss = { addDialogRequestKey = null },
            onConfirm = { amount, note ->
                onAddDemoMoney(state.vpa, amount, note, reqKey)
                addDialogRequestKey = null
            }
        )
    }

    transferDialogRequestKey?.let { reqKey ->
        val defaultSender = state.selectedAccount ?: state.accounts.first()
        DemoTransferDialog(
            accounts = state.accounts,
            initialSender = defaultSender,
            onDismiss = { transferDialogRequestKey = null },
            onConfirm = { sender, receiver, amount, note ->
                onTransferBetweenAccounts(sender, receiver, amount, note, reqKey)
                transferDialogRequestKey = null
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text("Reset All Demo Accounts?") },
            text = {
                Text(
                    "This restores Aarav's primary demo wallet to ₹10,000.00, resets all other fictional accounts to their initial balances, and resets simulated transaction history."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetWallet()
                        showResetDialog = false
                    },
                    modifier = Modifier.testTag("confirm_reset_wallet")
                ) {
                    Text("Reset Demo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    state.latestReceipt?.let { receipt ->
        SimulatedWalletReceiptDialog(
            receipt = receipt,
            onDismiss = onDismissReceipt
        )
    }

    state.selectedTransaction?.let { tx ->
        TransactionDetailBottomSheet(
            tx = tx,
            onDismiss = { onSelectTransaction(null) }
        )
    }
}

@Composable
private fun AddDemoMoneyDialog(
    targetAccountName: String,
    targetVpa: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var amount by remember { mutableStateOf("1000") }
    var note by remember { mutableStateOf("Practice wallet top-up") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("Add Fictional Demo Funds") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SimulatedBadge(text = "SIMULATED CREDIT ONLY")
                Text(
                    text = "Target: $targetAccountName ($targetVpa)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("500", "1000", "5000").forEach { preset ->
                        FilterChip(
                            selected = amount == preset,
                            onClick = {
                                amount = preset
                                validationError = null
                            },
                            label = { Text("+₹$preset") }
                        )
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        validationError = null
                    },
                    label = { Text("Demo Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_money_amount_input")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Memo (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                validationError?.let { err ->
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
                        validationError = "Enter a positive amount greater than ₹0."
                    } else if (paise > 10_000_000L) {
                        validationError = "Maximum single demo top-up is ₹1,00,000."
                    } else {
                        onConfirm(amount, note)
                    }
                },
                modifier = Modifier.testTag("confirm_add_money")
            ) {
                Text("Add Demo ₹")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DemoTransferDialog(
    accounts: List<WalletAccountEntity>,
    initialSender: WalletAccountEntity,
    onDismiss: () -> Unit,
    onConfirm: (WalletAccountEntity, WalletAccountEntity, String, String) -> Unit
) {
    var selectedSender by remember { mutableStateOf(initialSender) }
    val receiverCandidates = accounts.filter { it.vpa != selectedSender.vpa }
    var selectedReceiver by remember {
        mutableStateOf(receiverCandidates.firstOrNull() ?: accounts.last())
    }
    var amount by remember { mutableStateOf("250") }
    var note by remember { mutableStateOf("Demo peer transfer") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val parsedPaise = FinGuardFormatters.parsePaiseInput(amount)
    val ext = LocalFinGuardColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text("Transfer Between Fictional Accounts") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SimulatedBadge(text = "SIMULATED ACCOUNT TRANSFER")

                Text(
                    text = "From (Sender)",
                    style = MaterialTheme.typography.labelLarge
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accounts, key = { "sender_${it.vpa}" }) { acct ->
                        val isSelected = selectedSender.vpa == acct.vpa
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedSender = acct
                                if (selectedReceiver.vpa == acct.vpa) {
                                    selectedReceiver = accounts.first { it.vpa != acct.vpa }
                                }
                                validationError = null
                            },
                            label = {
                                Text("${acct.ownerName.substringBefore(" (")} (${FinGuardFormatters.formatPaise(acct.balanceInPaise)})")
                            }
                        )
                    }
                }

                Text(
                    text = "To (Receiver)",
                    style = MaterialTheme.typography.labelLarge
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    receiverCandidates.forEach { profile ->
                        val isSelected = selectedReceiver.vpa == profile.vpa
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    selectedReceiver = profile
                                    validationError = null
                                },
                            shape = MaterialTheme.shapes.small,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = profile.initials,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column {
                                        Text(profile.ownerName, style = MaterialTheme.typography.titleSmall)
                                        Text(profile.vpa, style = MaterialTheme.typography.labelSmall, color = ext.mutedText)
                                    }
                                }
                                Text(
                                    text = FinGuardFormatters.formatPaise(profile.balanceInPaise),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        validationError = null
                    },
                    label = { Text("Transfer Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input")
                )

                if (parsedPaise != null && parsedPaise > 0L && parsedPaise <= selectedSender.balanceInPaise) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            Text(
                                text = "Simulated Balance Preview",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Sender: ${FinGuardFormatters.formatPaise(selectedSender.balanceInPaise)} → ${FinGuardFormatters.formatPaise(selectedSender.balanceInPaise - parsedPaise)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Receiver: ${FinGuardFormatters.formatPaise(selectedReceiver.balanceInPaise)} → ${FinGuardFormatters.formatPaise(selectedReceiver.balanceInPaise + parsedPaise)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.safe
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Memo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                validationError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.danger
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paise = FinGuardFormatters.parsePaiseInput(amount)
                    if (paise == null || paise <= 0L) {
                        validationError = "Enter a positive amount greater than ₹0."
                    } else if (paise > selectedSender.balanceInPaise) {
                        validationError = "Overdraft prevented: exceeds ${selectedSender.ownerName}'s available demo balance."
                    } else if (selectedSender.vpa == selectedReceiver.vpa) {
                        validationError = "Sender and receiver must be different."
                    } else {
                        onConfirm(selectedSender, selectedReceiver, amount, note)
                    }
                },
                modifier = Modifier.testTag("confirm_demo_transfer")
            ) {
                Text("Send Demo ₹")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
