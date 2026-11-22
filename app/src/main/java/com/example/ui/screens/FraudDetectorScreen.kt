package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.DemoCatalog
import com.example.data.model.FraudAnalysisResult
import com.example.data.model.ScanType
import com.example.ui.FraudDetectorScreenState
import com.example.ui.FraudScanUiState
import com.example.ui.components.GlassFintechCard
import com.example.ui.components.RiskBadge
import com.example.ui.components.RiskGaugeArc
import com.example.ui.components.SafetyGuardBanner
import com.example.ui.components.SimulatedBadge
import com.example.ui.theme.LocalFinGuardColors

@Composable
fun FraudDetectorScreen(
    state: FraudDetectorScreenState,
    onSelectScanType: (ScanType) -> Unit,
    onUpdateInput: (String) -> Unit,
    onToggleGeminiAi: (Boolean) -> Unit,
    onLoadSample: (ScanType, String) -> Unit,
    onRunScan: (forceOffline: Boolean) -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalFinGuardColors.current
    val clipboard = LocalClipboardManager.current
    val samplesForMode = DemoCatalog.fraudSamples.filter { it.scanType == state.selectedScanType }
    val listState = rememberLazyListState()

    LaunchedEffect(state.scanState) {
        if (state.scanState is FraudScanUiState.Success || state.scanState is FraudScanUiState.Error) {
            listState.animateScrollToItem(6)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("fraud_lazy_list"),
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
                            text = "Fraud Shield AI",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "SMS, URL & UPI heuristic scanner (0–100)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.testTag("shield_open_history_button")
                    ) {
                        Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("History")
                    }
                }
            }

            // Scan Type Segmented Control
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ScanType.entries.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = state.selectedScanType == type,
                            onClick = { onSelectScanType(type) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ScanType.entries.size
                            ),
                            modifier = Modifier.testTag("scan_type_${type.name.lowercase()}")
                        ) {
                            Text(type.shortLabel)
                        }
                    }
                }
            }

            // AI vs Offline Engine Selector
            item {
                GlassFintechCard(modifier = Modifier.fillMaxWidth()) {
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
                            Icon(
                                imageVector = if (state.useGeminiAi) Icons.Filled.AutoAwesome else Icons.Filled.OfflineBolt,
                                contentDescription = null,
                                tint = if (state.useGeminiAi) ext.champagneGold else ext.emerald
                            )
                            Column {
                                Text(
                                    text = if (state.useGeminiAi) "Gemini AI + Heuristic Shield" else "Offline Local Rules Shield",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (state.useGeminiAi) {
                                        "AI-assisted contextual inspection via Firebase AI Logic"
                                    } else {
                                        "100% offline rule-based SMS, URL & UPI analysis"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ext.mutedText
                                )
                            }
                        }
                        Switch(
                            checked = state.useGeminiAi,
                            onCheckedChange = onToggleGeminiAi,
                            modifier = Modifier.testTag("shield_engine_switch")
                        )
                    }
                }
            }

            // Sample Test Prompts (Both Suspicious & Legitimate)
            item {
                Text(
                    text = "Sample Messages & Links (Suspicious & Legitimate)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(samplesForMode) { sample ->
                        FilterChip(
                            selected = state.inputText == sample.payload,
                            onClick = { onLoadSample(sample.scanType, sample.payload) },
                            label = {
                                Text(
                                    text = if (sample.isRisky) "⚠ ${sample.title}" else "✓ ${sample.title}"
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            // Input Field + Paste + Scan Actions
            item {
                val hint = when (state.selectedScanType) {
                    ScanType.SMS -> "Paste an SMS message to inspect urgency, fake rewards, OTP requests, impersonation, or links..."
                    ScanType.URL -> "Enter a URL to inspect domain spelling, shorteners, HTTP usage, and phishing indicators..."
                    ScanType.UPI -> "Enter a simulated UPI payment/collect request to check warning signs..."
                }
                GlassFintechCard(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = onUpdateInput,
                        label = { Text(state.selectedScanType.label) },
                        placeholder = { Text(hint) },
                        minLines = 3,
                        maxLines = 6,
                        trailingIcon = {
                            if (state.inputText.isNotEmpty()) {
                                IconButton(
                                    onClick = { onUpdateInput("") },
                                    modifier = Modifier.testTag("fraud_clear_button")
                                ) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear input")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("fraud_input_field")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipText = clipboard.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    onUpdateInput(clipText)
                                }
                            },
                            modifier = Modifier.height(50.dp).testTag("fraud_paste_button")
                        ) {
                            Icon(Icons.Filled.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste")
                        }
                        Button(
                            onClick = { onRunScan(false) },
                            enabled = state.scanState !is FraudScanUiState.Scanning,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("fraud_scan_button")
                        ) {
                            if (state.scanState is FraudScanUiState.Scanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing...")
                            } else {
                                Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyze Threat")
                            }
                        }
                    }
                }
            }

            item {
                SafetyGuardBanner(
                    text = "Privacy Shield: Never enter real OTPs, UPI PINs, bank credentials, or card numbers. Sensitive digits are auto-redacted."
                )
            }

            // Results Section
            item {
                AnimatedContent(
                    targetState = state.scanState,
                    label = "fraud_scan_result"
                ) { scanState ->
                    when (scanState) {
                        FraudScanUiState.Idle -> {
                            GlassFintechCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Ready to Scan",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Choose a sample above or paste any SMS, URL, or simulated UPI request for an explainable 0–100 risk score.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        FraudScanUiState.Scanning -> {
                            GlassFintechCard(accentBorder = true, modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
                                    Text(
                                        text = if (state.useGeminiAi) "Running Gemini AI Threat Inspection..." else "Running Offline Rule Analysis...",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            }
                        }

                        is FraudScanUiState.Error -> {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                color = ext.dangerBg,
                                border = BorderStroke(1.dp, ext.danger.copy(alpha = 0.65f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = ext.danger)
                                        Text(
                                            text = "Scan Notice",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    Text(
                                        text = scanState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (scanState.allowOfflineFallback) {
                                        Button(
                                            onClick = { onRunScan(true) },
                                            modifier = Modifier.testTag("run_offline_fallback_button")
                                        ) {
                                            Icon(Icons.Filled.OfflineBolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Run Offline Heuristic Scan")
                                        }
                                    }
                                }
                            }
                        }

                        is FraudScanUiState.Success -> {
                            FraudResultCard(
                                result = scanState.result,
                                onOpenHistory = onOpenHistory
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun FraudResultCard(
    result: FraudAnalysisResult,
    onOpenHistory: () -> Unit
) {
    val ext = LocalFinGuardColors.current
    GlassFintechCard(
        accentBorder = true,
        modifier = Modifier.fillMaxWidth().testTag("fraud_result_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimulatedBadge(
                text = result.engineUsed.label.uppercase(),
                tint = if (result.engineUsed.name.contains("GEMINI")) ext.champagneGold else ext.tropicalAqua
            )
            RiskBadge(riskLevel = result.riskLevel)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            RiskGaugeArc(
                score = result.riskScore,
                riskLevel = result.riskLevel
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = result.headline,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = result.explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        result.fallbackNotice?.let { fallback ->
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = ext.cautionBg,
                border = BorderStroke(1.dp, ext.caution.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.OfflineBolt,
                        contentDescription = null,
                        tint = ext.caution,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = fallback,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        result.privacyNotice?.let { notice ->
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = ext.cautionBg
            ) {
                Text(
                    text = notice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Why This Score Was Assigned",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            result.indicators.forEach { indicator ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Text(
                        text = indicator,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text(
                    text = "Practical Safety Recommendation",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = result.recommendation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = ext.mutedText,
                    modifier = Modifier.size(16.dp).padding(top = 1.dp)
                )
                Text(
                    text = "Advisory Notice: AI and local heuristic analyses are advisory only and cannot guarantee whether a message, link, or transaction is fraudulent. Never enter real OTPs, UPI PINs, bank passwords, or card details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.mutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onOpenHistory) {
                Text("Saved to Scan History • View All")
            }
        }
    }
}
