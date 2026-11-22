package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.WalletTransactionEntity
import com.example.data.model.DemoCatalog
import com.example.data.model.DemoProfile
import com.example.data.model.FinGuardFormatters
import com.example.data.model.SimulationOutcome
import com.example.ui.UpiSimUiState
import com.example.ui.components.GlassFintechCard
import com.example.ui.components.SimulatedBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFinGuardColors

@Composable
fun UpiSimulationScreen(
    state: UpiSimUiState,
    onSelectSender: (DemoProfile) -> Unit,
    onSelectReceiver: (DemoProfile) -> Unit,
    onToggleCustomReceiver: (Boolean) -> Unit,
    onUpdateCustomName: (String) -> Unit,
    onUpdateCustomVpa: (String) -> Unit,
    onUpdateAmount: (String) -> Unit,
    onUpdateNote: (String) -> Unit,
    onSelectOutcomeMode: (SimulationOutcome) -> Unit,
    onExecuteSimulation: () -> Unit,
    onDismissReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalFinGuardColors.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("upi_lazy_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "UPI Simulation Lab",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "Offline fake-money payment sandbox",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SimulatedBadge(text = "NO REAL UPI / NPCI")
                }
            }

            // Active Demo Balance Strip
            item {
                GlassFintechCard(accentBorder = true, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Simulated Balance • ${state.selectedSender.vpa}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = FinGuardFormatters.formatPaise(state.balanceInPaise),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.testTag("upi_sender_balance")
                            )
                        }
                        SimulatedBadge(text = "DEMO FUNDS", tint = ext.safe)
                    }
                }
            }

            // Fictional Sender Selector
            item {
                GlassFintechCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "1. Fictional Sender Profile",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DemoCatalog.senderProfiles.forEach { profile ->
                            val selected = state.selectedSender.id == profile.id
                            ProfileChoiceRow(
                                profile = profile,
                                selected = selected,
                                onClick = { onSelectSender(profile) }
                            )
                        }
                    }
                }
            }

            // Fictional Receiver Selector
            item {
                GlassFintechCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Fictional Receiver Profile",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Custom VPA",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Switch(
                                checked = state.useCustomReceiver,
                                onCheckedChange = onToggleCustomReceiver,
                                modifier = Modifier.testTag("upi_custom_vpa_switch")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.useCustomReceiver) {
                        OutlinedTextField(
                            value = state.customReceiverName,
                            onValueChange = onUpdateCustomName,
                            label = { Text("Fictional Receiver Name") },
                            placeholder = { Text("e.g., Demo Cafe") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.customReceiverVpa,
                            onValueChange = onUpdateCustomVpa,
                            label = { Text("Fictional VPA (name@demo)") },
                            placeholder = { Text("e.g., cafe@okdemo or reward.claim@fakebank") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("upi_custom_vpa_input")
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DemoCatalog.receiverProfiles.forEach { profile ->
                                val selected = state.selectedReceiver?.id == profile.id
                                ProfileChoiceRow(
                                    profile = profile,
                                    selected = selected,
                                    onClick = { onSelectReceiver(profile) }
                                )
                            }
                        }
                    }
                }
            }

            // Amount, Remark & Simulation Mode
            item {
                GlassFintechCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "3. Payment Details & Outcome Mode",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = state.amountInput,
                        onValueChange = onUpdateAmount,
                        label = { Text("Simulated Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("upi_amount_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.noteInput,
                        onValueChange = onUpdateNote,
                        label = { Text("Payment Remark") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("upi_note_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Simulation Outcome Behavior",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        modifier = Modifier.testTag("upi_outcome_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(SimulationOutcome.entries) { mode ->
                            FilterChip(
                                selected = state.outcomeMode == mode,
                                onClick = { onSelectOutcomeMode(mode) },
                                label = { Text(mode.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.testTag("upi_outcome_${mode.name.lowercase()}")
                            )
                        }
                    }
                    Text(
                        text = state.outcomeMode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.mutedText,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Real-time Fraud Pre-Check Warning
            item {
                AnimatedVisibility(visible = state.preCheckWarning != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = ext.highOrangeBg,
                        border = BorderStroke(1.dp, ext.highOrange.copy(alpha = 0.72f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = "Fraud guard warning",
                                tint = ext.highOrange
                            )
                            Text(
                                text = state.preCheckWarning.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // No-PIN Sandbox Guarantee + Error
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = ext.safe,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Zero real credentials: FinGuard never asks for UPI PINs, OTPs, or real bank accounts. Generates a local TX ID only.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            state.errorMessage?.let { err ->
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().testTag("upi_error_banner"),
                        shape = MaterialTheme.shapes.medium,
                        color = ext.dangerBg,
                        border = BorderStroke(1.dp, ext.danger.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = ext.danger)
                            Text(err, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onExecuteSimulation,
                    enabled = !state.isSimulating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("upi_simulate_submit_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (state.isSimulating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulating Sandbox Rail...")
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Simulated UPI Transfer")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    state.latestReceipt?.let { receipt ->
        SimulatedUpiReceiptDialog(
            receipt = receipt,
            onDismiss = onDismissReceipt
        )
    }
}

@Composable
private fun ProfileChoiceRow(
    profile: DemoProfile,
    selected: Boolean,
    onClick: () -> Unit
) {
    val ext = LocalFinGuardColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("profile_choice_${profile.id}"),
        shape = MaterialTheme.shapes.medium,
        color = when {
            selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            profile.isRiskySample -> ext.dangerBg.copy(alpha = 0.45f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = when {
                selected -> MaterialTheme.colorScheme.primary
                profile.isRiskySample -> ext.danger.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (profile.isRiskySample) ext.danger.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.initials,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (profile.isRiskySample) ext.danger else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = profile.vpa,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (profile.isRiskySample) ext.danger else MaterialTheme.colorScheme.tertiary
                )
            }
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = profile.roleTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.mutedText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SimulatedUpiReceiptDialog(
    receipt: WalletTransactionEntity,
    onDismiss: () -> Unit
) {
    val ext = LocalFinGuardColors.current
    val isFailed = receipt.status == "SIMULATED_FAILED"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SimulatedBadge(
                    text = "SIMULATED TRANSACTION",
                    tint = if (isFailed) ext.danger else ext.safe
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isFailed) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (isFailed) ext.danger else ext.safe,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = if (isFailed) "Simulated Transfer Failed" else "Simulated Transfer Approved",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = FinGuardFormatters.formatPaise(receipt.amountInPaise),
                    style = MaterialTheme.typography.displayMedium,
                    color = if (isFailed) ext.danger else ext.safe
                )
                if (!receipt.failureReason.isNullOrBlank()) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = ext.dangerBg
                    ) {
                        Text(
                            text = receipt.failureReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ReceiptLine("Local TX ID", receipt.txRef)
                ReceiptLine("From (Demo)", "${receipt.senderName}\n${receipt.senderVpa}")
                ReceiptLine("To (Demo)", "${receipt.counterpartyName}\n${receipt.counterpartyVpa}")
                ReceiptLine(
                    "Sender Balance",
                    "${FinGuardFormatters.formatPaise(receipt.balanceBeforeInPaise)} → ${FinGuardFormatters.formatPaise(receipt.balanceAfterInPaise)}"
                )
                if (receipt.receiverBalanceBeforeInPaise != null && receipt.receiverBalanceAfterInPaise != null) {
                    ReceiptLine(
                        "Receiver Balance",
                        "${FinGuardFormatters.formatPaise(receipt.receiverBalanceBeforeInPaise)} → ${FinGuardFormatters.formatPaise(receipt.receiverBalanceAfterInPaise)}"
                    )
                }
                ReceiptLine("Timestamp", FinGuardFormatters.formatTimestamp(receipt.createdAt))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "SIMULATED TRANSACTION ONLY. Not connected to UPI, NPCI, or any bank.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_upi_receipt")
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun ReceiptLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = LocalFinGuardColors.current.mutedText,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = JetBrainsMonoFontFamily,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.62f)
        )
    }
}
