package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ScanRecordEntity
import com.example.data.local.WalletTransactionEntity
import com.example.data.model.FinGuardFormatters
import com.example.data.model.RiskLevel
import com.example.ui.HomeSummaryUiState
import com.example.ui.components.GlassFintechCard
import com.example.ui.components.RiskBadge
import com.example.ui.components.SafetyGuardBanner
import com.example.ui.components.SimulatedBadge
import com.example.ui.components.TransactionDetailBottomSheet
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFinGuardColors

@Composable
fun HomeDashboardScreen(
    state: HomeSummaryUiState,
    onNavigateToWallet: () -> Unit,
    onNavigateToUpiSim: () -> Unit,
    onNavigateToShield: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToLearn: () -> Unit,
    onNavigateToBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    var selectedTx by remember { mutableStateOf<WalletTransactionEntity?>(null) }
    LaunchedEffect(Unit) { visible = true }

    val ext = LocalFinGuardColors.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("home_lazy_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically { -18 }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(ext.oceanBlue, ext.tropicalAqua)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = "FinGuard AI Crest",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "FIN GUARD AI",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Welcome, ${state.ownerName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        SimulatedBadge(text = "DEMO SANDBOX", tint = ext.auroraMint)
                    }
                }
            }

            // Hero Ocean-Blue-to-Aqua Wallet Balance Card
            item {
                val shape = MaterialTheme.shapes.extraLarge
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape),
                    shape = shape,
                    shadowElevation = 8.dp,
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                ext.tropicalAqua.copy(alpha = 0.72f),
                                ext.oceanBlue.copy(alpha = 0.65f)
                            )
                        )
                    )
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_shield_hero),
                            contentDescription = "FinGuard AI Security Shield Hero",
                            contentScale = ContentScale.Crop,
                            alpha = 0.35f,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            ext.deepOceanBlue.copy(alpha = 0.96f),
                                            ext.oceanBlue.copy(alpha = 0.88f),
                                            ext.tropicalAqua.copy(alpha = 0.42f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SimulatedBadge(
                                    text = "SIMULATED BALANCE",
                                    tint = ext.auroraMint
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = state.vpa,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column {
                                Text(
                                    text = "Fictional Demo Wallet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = FinGuardFormatters.formatPaise(state.balanceInPaise),
                                    style = MaterialTheme.typography.displayLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.testTag("home_wallet_balance")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onNavigateToWallet,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("home_open_wallet_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ext.obsidian.copy(alpha = 0.86f),
                                        contentColor = ext.auroraMint
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AddCircleOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Manage Wallet")
                                }
                                OutlinedButton(
                                    onClick = onNavigateToUpiSim,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("home_upi_sim_button"),
                                    border = BorderStroke(
                                        1.dp,
                                        ext.auroraMint.copy(alpha = 0.72f)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("UPI Sim", color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // Security Status & Counters Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        title = "Shield Status",
                        value = "${state.securityScore}%",
                        subtitle = "Active Guard",
                        accent = if (state.securityScore >= 95) ext.champagneGold else ext.auroraMint,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Demo TXs",
                        value = "${state.transactionCount}",
                        subtitle = "Saved Local",
                        accent = ext.tropicalAqua,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Scans Run",
                        value = "${state.totalScans}",
                        subtitle = "${state.threatsFlagged} Flagged",
                        accent = if (state.threatsFlagged > 0) ext.highOrange else ext.lightCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Smart Budget Pulse Preview Card (5-Tier Dynamic Color System)
            item {
                val tier = state.budgetStatusTier
                val tierColor = tier.accentColor
                GlassFintechCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_open_budget_button"),
                    accentBorder = true,
                    onClick = onNavigateToBudget
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(tierColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PieChart,
                                    contentDescription = "Smart Budget Planner",
                                    tint = tierColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Smart Budget Planner",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = when {
                                        state.monthlyBudgetInPaise <= 0L -> "Tap to set monthly income & budget"
                                        state.budgetOverspentInPaise > 0L ->
                                            "Overspent by ${FinGuardFormatters.formatPaise(state.budgetOverspentInPaise)} • ${state.savingsGoalCount} goals"
                                        else ->
                                            "Remaining: ${FinGuardFormatters.formatPaise(state.remainingBudgetInPaise)} • ${state.savingsGoalCount} goals"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        SimulatedBadge(
                            text = state.budgetAlertLevel.badgeText,
                            tint = tierColor
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (state.budgetUsedPercent.coerceIn(0, 100)) / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = tierColor,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${tier.label} • Spent ${FinGuardFormatters.formatPaise(state.totalMonthlyExpensesInPaise)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${state.budgetUsedPercent}% of ${FinGuardFormatters.formatPaise(state.monthlyBudgetInPaise)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = tierColor
                        )
                    }
                }
            }

            // Financial Insights Section (Emerald + Champagne Gold highlights)
            item {
                GlassFintechCard(
                    modifier = Modifier.fillMaxWidth(),
                    goldAccent = state.savingsRatePercent >= 20,
                    onClick = onNavigateToBudget
                ) {
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
                                imageVector = Icons.Filled.AutoGraph,
                                contentDescription = "Financial Insights",
                                tint = ext.luxuryMint,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Financial Insights",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (state.savingsRatePercent >= 20) {
                            SimulatedBadge(
                                text = "${state.savingsRatePercent}% SAVED",
                                tint = ext.champagneGold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = ext.tropicalAqua,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Net Monthly Savings",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.mutedText
                            )
                            Text(
                                text = FinGuardFormatters.formatPaise(state.netSavingsInPaise),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (state.netSavingsInPaise >= 0L) ext.luxuryMint else ext.danger
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Top Outflow",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.mutedText
                            )
                            Text(
                                text = state.topBudgetCategoryLabel ?: "Not Logged",
                                style = MaterialTheme.typography.titleSmall,
                                color = ext.champagneGold
                            )
                        }
                    }
                }
            }

            // Quick Access Grid
            item {
                Text(
                    text = "Quick Access",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickAccessTile(
                            title = "Fraud Shield",
                            subtitle = "SMS, URL & UPI scan",
                            icon = Icons.Filled.Security,
                            accent = ext.tropicalAqua,
                            testTag = "quick_fraud_shield",
                            onClick = onNavigateToShield,
                            modifier = Modifier.weight(1f)
                        )
                        QuickAccessTile(
                            title = "UPI Simulator",
                            subtitle = "Fake-money test lab",
                            icon = Icons.Filled.SwapHoriz,
                            accent = ext.lightCyan,
                            testTag = "quick_upi_sim",
                            onClick = onNavigateToUpiSim,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickAccessTile(
                            title = "Budget Planner",
                            subtitle = "${state.budgetUsedPercent}% used • Goals & AI",
                            icon = Icons.Filled.Savings,
                            accent = ext.luxuryMint,
                            testTag = "quick_budget_planner",
                            onClick = onNavigateToBudget,
                            modifier = Modifier.weight(1f)
                        )
                        QuickAccessTile(
                            title = "Scan History",
                            subtitle = "${state.totalScans} saved reports",
                            icon = Icons.Filled.History,
                            accent = ext.champagneGold,
                            testTag = "quick_scan_history",
                            onClick = onNavigateToHistory,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickAccessTile(
                            title = "Security Academy",
                            subtitle = "OTP, UPI & phishing",
                            icon = Icons.Filled.MenuBook,
                            accent = ext.auroraMint,
                            testTag = "quick_security_academy",
                            onClick = onNavigateToLearn,
                            modifier = Modifier.weight(1f)
                        )
                        QuickAccessTile(
                            title = "Demo Wallet",
                            subtitle = "Add & transfer funds",
                            icon = Icons.Filled.AccountBalanceWallet,
                            accent = ext.oceanBlue,
                            testTag = "quick_demo_wallet",
                            onClick = onNavigateToWallet,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                SafetyGuardBanner()
            }

            // Recent Simulated Activity
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Simulated Activity",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(
                        onClick = onNavigateToWallet,
                        modifier = Modifier.testTag("home_view_all_tx")
                    ) {
                        Text("Open Wallet")
                    }
                }
            }

            if (state.recentTransactions.isEmpty()) {
                item {
                    GlassFintechCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Initializing your ₹10,000.00 fictional demo wallet...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            } else {
                items(state.recentTransactions, key = { "home_tx_${it.id}" }) { tx ->
                    TransactionRowCard(tx = tx, onClick = { selectedTx = tx })
                }
            }

            // Recent Scan Alerts
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Fraud Scans",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.testTag("home_view_all_scans")
                    ) {
                        Text("Scan History")
                    }
                }
            }

            if (state.recentScans.isEmpty()) {
                item {
                    GlassFintechCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNavigateToShield
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No fraud scans yet",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Tap to test SMS scams, phishing URLs, or UPI collect requests.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(state.recentScans, key = { "home_scan_${it.id}" }) { scan ->
                    HomeScanPreviewCard(scan = scan, onClick = onNavigateToHistory)
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    selectedTx?.let { tx ->
        TransactionDetailBottomSheet(tx = tx, onDismiss = { selectedTx = null })
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    GlassFintechCard(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            color = accent
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = LocalFinGuardColors.current.mutedText,
            maxLines = 1
        )
    }
}

@Composable
private fun QuickAccessTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassFintechCard(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = LocalFinGuardColors.current.mutedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeScanPreviewCard(
    scan: ScanRecordEntity,
    onClick: () -> Unit
) {
    val risk = RiskLevel.fromRaw(scan.riskLevel, scan.riskScore)
    GlassFintechCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${scan.scanType} • ${scan.headline}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = scan.inputContent,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalFinGuardColors.current.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            RiskBadge(riskLevel = risk)
        }
    }
}
