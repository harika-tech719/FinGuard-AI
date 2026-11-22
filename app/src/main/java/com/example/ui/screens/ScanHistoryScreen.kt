package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ScanRecordEntity
import com.example.data.model.FinGuardFormatters
import com.example.data.model.RiskLevel
import com.example.data.model.ScanType
import com.example.ui.ScanHistoryScreenState
import com.example.ui.components.GlassFintechCard
import com.example.ui.components.RiskBadge
import com.example.ui.components.RiskGaugeArc
import com.example.ui.components.SimulatedBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFinGuardColors

@Composable
fun ScanHistoryScreen(
    state: ScanHistoryScreenState,
    onBack: () -> Unit,
    onFilterType: (ScanType?) -> Unit,
    onFilterRisk: (RiskLevel?) -> Unit,
    onSelectRecord: (ScanRecordEntity?) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onClearAll: () -> Unit,
    onLaunchScanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var showClearDialog by remember { mutableStateOf(false) }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("history_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Scan History",
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Text(
                                text = "${state.allScans.size} saved threat reports",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (state.allScans.isNotEmpty() || state.filterType != null || state.filterRisk != null) {
                        TextButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.testTag("history_clear_all_button")
                        ) {
                            Text("Clear All", color = ext.danger)
                        }
                    }
                }
            }

            // Type Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.filterType == null,
                            onClick = { onFilterType(null) },
                            label = { Text("All Types") }
                        )
                    }
                    items(ScanType.entries) { type ->
                        FilterChip(
                            selected = state.filterType == type,
                            onClick = { onFilterType(if (state.filterType == type) null else type) },
                            label = { Text(type.shortLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("history_filter_type_${type.name.lowercase()}")
                        )
                    }
                }
            }

            // Risk Level Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.filterRisk == null,
                            onClick = { onFilterRisk(null) },
                            label = { Text("All Risks") }
                        )
                    }
                    items(RiskLevel.entries) { level ->
                        FilterChip(
                            selected = state.filterRisk == level,
                            onClick = { onFilterRisk(if (state.filterRisk == level) null else level) },
                            label = { Text(level.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.testTag("history_filter_risk_${level.name.lowercase()}")
                        )
                    }
                }
            }

            if (state.allScans.isEmpty()) {
                item {
                    GlassFintechCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No matching scan records",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Every SMS, URL, and UPI scan you run is saved here locally with its risk score and timestamp.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.mutedText
                            )
                            Button(onClick = onLaunchScanner) {
                                Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open Fraud Shield")
                            }
                        }
                    }
                }
            } else {
                items(state.allScans, key = { it.id }) { record ->
                    ScanHistoryRowCard(
                        record = record,
                        onClick = { onSelectRecord(record) },
                        onDelete = { onDeleteRecord(record.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text("Clear Scan History?") },
            text = { Text("This deletes all saved fraud scan logs from your local Room database.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showClearDialog = false
                    },
                    modifier = Modifier.testTag("confirm_clear_history")
                ) {
                    Text("Clear Logs")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    state.selectedRecord?.let { record ->
        ScanRecordDetailSheet(
            record = record,
            onDismiss = { onSelectRecord(null) },
            onDelete = { onDeleteRecord(record.id) }
        )
    }
}

@Composable
private fun ScanHistoryRowCard(
    record: ScanRecordEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val risk = RiskLevel.fromRaw(record.riskLevel, record.riskScore)
    val ext = LocalFinGuardColors.current

    GlassFintechCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scan_history_item_${record.id}"),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = record.scanType,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                RiskBadge(riskLevel = risk)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val scoreColor = when (risk) {
                    RiskLevel.LOW -> ext.safe
                    RiskLevel.MEDIUM -> ext.caution
                    RiskLevel.HIGH -> ext.highOrange
                    RiskLevel.CRITICAL -> ext.danger
                }
                Text(
                    text = "${record.riskScore}/100",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = scoreColor
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete scan record",
                        tint = ext.mutedText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = record.headline,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = record.inputContent,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${FinGuardFormatters.formatTimestamp(record.createdAt)} • ${record.engineUsed.replace('_', ' ')}",
            style = MaterialTheme.typography.labelSmall,
            color = ext.mutedText
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanRecordDetailSheet(
    record: ScanRecordEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val risk = RiskLevel.fromRaw(record.riskLevel, record.riskScore)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimulatedBadge(text = "${record.scanType} SCAN REPORT")
                RiskBadge(riskLevel = risk)
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                RiskGaugeArc(score = record.riskScore, riskLevel = risk, gaugeSize = 124.dp)
            }

            Text(text = record.headline, style = MaterialTheme.typography.titleLarge)
            Text(
                text = FinGuardFormatters.formatTimestamp(record.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = LocalFinGuardColors.current.mutedText
            )

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = record.inputContent,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    modifier = Modifier.fillMaxWidth().padding(10.dp)
                )
            }

            Text(text = record.explanation, style = MaterialTheme.typography.bodyMedium)

            val reasons = record.indicatorsJoined
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toList()
            if (reasons.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(text = "Score Reasons & Signals", style = MaterialTheme.typography.titleSmall)
                reasons.forEach { reason ->
                    Text(
                        text = "• $reason",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(text = "Practical Recommendation", style = MaterialTheme.typography.titleSmall)
            Text(
                text = record.recommendation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = "Advisory Notice: AI and local heuristic analyses are advisory only and cannot guarantee whether a message or transaction is fraudulent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalFinGuardColors.current.mutedText,
                    modifier = Modifier.fillMaxWidth().padding(10.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDelete) {
                    Text("Delete Record", color = LocalFinGuardColors.current.danger)
                }
            }
        }
    }
}
