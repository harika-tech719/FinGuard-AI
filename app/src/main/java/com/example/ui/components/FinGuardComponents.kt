package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.WalletTransactionEntity
import com.example.data.model.FinGuardFormatters
import com.example.data.model.RiskLevel
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFinGuardColors

@Composable
fun FinGuardBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val ext = LocalFinGuardColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ext.obsidian,
                        ext.midnight,
                        ext.obsidian
                    )
                )
            )
    ) {
        content()
    }
}

@Composable
fun GlassFintechCard(
    modifier: Modifier = Modifier,
    accentBorder: Boolean = false,
    goldAccent: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = LocalFinGuardColors.current
    val shape = MaterialTheme.shapes.large
    val borderBrush = when {
        goldAccent -> Brush.linearGradient(
            colors = listOf(
                ext.champagneGold.copy(alpha = 0.72f),
                ext.tropicalAqua.copy(alpha = 0.45f)
            )
        )
        accentBorder -> Brush.linearGradient(
            colors = listOf(
                ext.oceanBlue.copy(alpha = 0.85f),
                ext.tropicalAqua.copy(alpha = 0.75f)
            )
        )
        else -> Brush.linearGradient(
            colors = listOf(
                ext.cardBorder.copy(alpha = 0.75f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            )
        )
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = if (accentBorder || goldAccent) 6.dp else 2.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun SimulatedBadge(
    text: String = "SIMULATED SANDBOX",
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.tertiary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                maxLines = 1
            )
        }
    }
}

@Composable
fun RiskBadge(
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier
) {
    val ext = LocalFinGuardColors.current
    val (fg, bg) = when (riskLevel) {
        RiskLevel.LOW -> ext.safe to ext.safeBg
        RiskLevel.MEDIUM -> ext.caution to ext.cautionBg
        RiskLevel.HIGH -> ext.highOrange to ext.highOrangeBg
        RiskLevel.CRITICAL -> ext.danger to ext.dangerBg
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.6f))
    ) {
        Text(
            text = riskLevel.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun RiskGaugeArc(
    score: Int,
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier,
    gaugeSize: Dp = 148.dp
) {
    val ext = LocalFinGuardColors.current
    val targetColor = when (riskLevel) {
        RiskLevel.LOW -> ext.safe
        RiskLevel.MEDIUM -> ext.caution
        RiskLevel.HIGH -> ext.highOrange
        RiskLevel.CRITICAL -> ext.danger
    }
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        label = "risk_color"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = score.coerceIn(0, 100) / 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "risk_progress"
    )
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier.size(gaugeSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        animatedColor.copy(alpha = 0.7f),
                        animatedColor
                    )
                ),
                startAngle = 140f,
                sweepAngle = 260f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.displayMedium,
                color = animatedColor
            )
            Text(
                text = "RISK / 100",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SafetyGuardBanner(
    modifier: Modifier = Modifier,
    text: String = "Sandbox Shield: Never enter real bank credentials, UPI PINs, OTPs, or card numbers."
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Security notice",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TransactionRowCard(
    tx: WalletTransactionEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalFinGuardColors.current
    val isCredit = tx.type == "CREDIT"
    val isFailed = tx.status == "SIMULATED_FAILED"

    val icon: ImageVector
    val iconTint: Color
    val iconBg: Color
    val amountPrefix: String
    val amountColor: Color

    when {
        isFailed -> {
            icon = Icons.Filled.ErrorOutline
            iconTint = ext.danger
            iconBg = ext.dangerBg
            amountPrefix = ""
            amountColor = ext.danger
        }
        isCredit -> {
            icon = Icons.AutoMirrored.Filled.CallReceived
            iconTint = ext.safe
            iconBg = ext.safeBg
            amountPrefix = "+"
            amountColor = ext.safe
        }
        else -> {
            icon = Icons.AutoMirrored.Filled.CallMade
            iconTint = MaterialTheme.colorScheme.tertiary
            iconBg = MaterialTheme.colorScheme.primaryContainer
            amountPrefix = "-"
            amountColor = MaterialTheme.colorScheme.onSurface
        }
    }

    GlassFintechCard(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("transaction_card_${tx.id}"),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tx.title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${tx.txRef} • ${FinGuardFormatters.formatShortTimestamp(tx.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = ext.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                SimulatedBadge(
                    text = if (isFailed) "SIMULATED FAILED" else "SIMULATED RECEIPT",
                    tint = if (isFailed) ext.danger else MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${FinGuardFormatters.formatPaise(tx.amountInPaise)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Text(
                    text = if (isFailed) "Declined" else "Tap receipt",
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.mutedText
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailBottomSheet(
    tx: WalletTransactionEntity,
    onDismiss: () -> Unit
) {
    val ext = LocalFinGuardColors.current
    val isFailed = tx.status == "SIMULATED_FAILED"
    val isCredit = tx.type == "CREDIT"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isFailed) Icons.Filled.WarningAmber else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (isFailed) ext.danger else ext.safe,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = tx.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    SimulatedBadge(
                        text = "SIMULATED TRANSACTION RECEIPT",
                        tint = if (isFailed) ext.danger else MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Text(
                text = (if (isCredit) "+" else "-") + FinGuardFormatters.formatPaise(tx.amountInPaise),
                style = MaterialTheme.typography.displayMedium,
                color = when {
                    isFailed -> ext.danger
                    isCredit -> ext.safe
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            if (!tx.failureReason.isNullOrBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = ext.dangerBg,
                    border = BorderStroke(1.dp, ext.danger.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = ext.danger
                        )
                        Text(
                            text = tx.failureReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            DetailRow("Local TX ID", tx.txRef, monospace = true)
            DetailRow("Status", if (isFailed) "Simulated Failure" else "Simulated Success")
            DetailRow("Sender", "${tx.senderName} (${tx.senderVpa})")
            DetailRow("Receiver", "${tx.counterpartyName} (${tx.counterpartyVpa})")
            if (!isCredit) {
                DetailRow(
                    "Sender Balance",
                    "${FinGuardFormatters.formatPaise(tx.balanceBeforeInPaise)} → ${FinGuardFormatters.formatPaise(tx.balanceAfterInPaise)}"
                )
            }
            if (tx.receiverBalanceBeforeInPaise != null && tx.receiverBalanceAfterInPaise != null) {
                DetailRow(
                    "Receiver Balance",
                    "${FinGuardFormatters.formatPaise(tx.receiverBalanceBeforeInPaise)} → ${FinGuardFormatters.formatPaise(tx.receiverBalanceAfterInPaise)}"
                )
            }
            DetailRow("Timestamp", FinGuardFormatters.formatTimestamp(tx.createdAt))
            if (tx.note.isNotBlank()) {
                DetailRow("Memo", tx.note)
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "SIMULATED TRANSACTION ONLY. Fictional Room ledger entry; no real money or bank rail was used.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SimulatedWalletReceiptDialog(
    receipt: WalletTransactionEntity,
    onDismiss: () -> Unit
) {
    val ext = LocalFinGuardColors.current
    val isCredit = receipt.type == "CREDIT"
    val isFailed = receipt.status == "SIMULATED_FAILED"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SimulatedBadge(
                    text = "SIMULATED TRANSACTION RECEIPT",
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
                        text = when {
                            isFailed -> "Simulated Transfer Failed"
                            isCredit -> "Demo Funds Added"
                            else -> "Demo Transfer Completed"
                        },
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
                    text = (if (isCredit) "+" else "-") + FinGuardFormatters.formatPaise(receipt.amountInPaise),
                    style = MaterialTheme.typography.displayMedium,
                    color = if (isFailed) ext.danger else ext.safe
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DetailRow("Local TX ID", receipt.txRef, monospace = true)
                DetailRow("From", "${receipt.senderName} (${receipt.senderVpa})")
                DetailRow("To", "${receipt.counterpartyName} (${receipt.counterpartyVpa})")
                if (!isCredit) {
                    DetailRow(
                        "Sender Balance",
                        "${FinGuardFormatters.formatPaise(receipt.balanceBeforeInPaise)} → ${FinGuardFormatters.formatPaise(receipt.balanceAfterInPaise)}"
                    )
                }
                if (receipt.receiverBalanceBeforeInPaise != null && receipt.receiverBalanceAfterInPaise != null) {
                    DetailRow(
                        "Receiver Balance",
                        "${FinGuardFormatters.formatPaise(receipt.receiverBalanceBeforeInPaise)} → ${FinGuardFormatters.formatPaise(receipt.receiverBalanceAfterInPaise)}"
                    )
                }
                DetailRow("Timestamp", FinGuardFormatters.formatTimestamp(receipt.createdAt))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "SIMULATED SANDBOX RECEIPT: Saved to Room database. No real funds were transferred.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_wallet_receipt")
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    monospace: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = LocalFinGuardColors.current.mutedText,
            modifier = Modifier.weight(0.36f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (monospace) JetBrainsMonoFontFamily else null,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.64f)
        )
    }
}
