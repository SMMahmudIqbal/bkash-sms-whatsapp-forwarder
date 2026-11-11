package com.bkash.forwarder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bkash.forwarder.data.local.ForwardStatus
import com.bkash.forwarder.data.local.TransactionRecord
import com.bkash.forwarder.ui.ForwarderViewModel
import com.bkash.forwarder.ui.theme.AccentAmber
import com.bkash.forwarder.ui.theme.AccentEmerald
import com.bkash.forwarder.ui.theme.AccentPrimary
import com.bkash.forwarder.ui.theme.AccentPrimaryText
import com.bkash.forwarder.ui.theme.AccentRose
import com.bkash.forwarder.ui.theme.BorderDark
import com.bkash.forwarder.ui.theme.PitchDark
import com.bkash.forwarder.ui.theme.SurfaceDark
import com.bkash.forwarder.ui.theme.SurfaceSubtle
import com.bkash.forwarder.ui.theme.TextFaint
import com.bkash.forwarder.ui.theme.TextMuted
import com.bkash.forwarder.ui.theme.TextPrimary
import com.bkash.forwarder.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: ForwarderViewModel,
    onRequestBatteryOptimization: () -> Unit,
    onRequestSmsPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    hasSmsPermissions: Boolean
) {
    val isEnabled by viewModel.isForwardingEnabled.collectAsState()
    val records by viewModel.records.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()
    val testResult by viewModel.testResult.collectAsState()
    val isBatteryOptimized by viewModel.isBatteryOptimizationIgnored.collectAsState()
    val isNotificationAccessEnabled by viewModel.isNotificationAccessEnabled.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showRestrictedGuideDialog by remember { mutableStateOf(false) }
    var selectedRecord by remember { mutableStateOf<TransactionRecord?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(testResult) {
        testResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearTestResult()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = PitchDark
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Sleek Top Header Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "bKash Forwarder",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isEnabled) AccentEmerald else TextMuted)
                            )
                            Text(
                                text = if (isEnabled) "Active & Listening" else "Forwarding Paused",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isEnabled) AccentEmerald else TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDark)
                            .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                            .clickable { showSettingsDialog = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
            }

            // 2. Minimalist Detection Channels Status Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Detection Channels",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            val isReady = hasSmsPermissions || isNotificationAccessEnabled
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isReady) AccentEmerald.copy(alpha = 0.12f) else AccentAmber.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isReady) "READY" else "ACTION REQUIRED",
                                    color = if (isReady) AccentEmerald else AccentAmber,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Channel 1: Direct SMS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Direct SMS Interception",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (hasSmsPermissions) "Active & monitoring" else "Requires SMS permission",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            if (hasSmsPermissions) {
                                StatusPill(label = "Active", isGood = true)
                            } else {
                                MinimalActionChip(
                                    label = "How to Allow",
                                    onClick = { showRestrictedGuideDialog = true }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = BorderDark, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Channel 2: Notification Listener
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notification Interception",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isNotificationAccessEnabled) "Active & capturing notifications" else "Captures bKash alerts from notifications",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            if (isNotificationAccessEnabled) {
                                StatusPill(label = "Active", isGood = true)
                            } else {
                                MinimalActionChip(
                                    label = "Turn On",
                                    onClick = onOpenNotificationSettings
                                )
                            }
                        }
                    }
                }
            }

            // 3. Battery Whitelist (Clean Minimalist Banner)
            if (!isBatteryOptimized) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Background Reliability",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Exempt app from battery saver to avoid sleep delay",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            MinimalActionChip(
                                label = "Whitelist",
                                onClick = onRequestBatteryOptimization
                            )
                        }
                    }
                }
            }

            // 4. Master Forwarding Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto Forwarding",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isEnabled) {
                                    val target = viewModel.preferences.recipientWhatsAppNumber
                                    val isGroup = viewModel.preferences.recipientType == "GROUP"
                                    val targetLabel = if (isGroup) "Group" else "Number"
                                    val modeLabel = if (viewModel.preferences.forwardMode == "RAW") "Raw Copy-Paste" else "Card"
                                    if (target.isNotBlank()) "Target: $targetLabel ($target) • $modeLabel" else "Target: Not configured in settings"
                                } else {
                                    "Forwarding is paused"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { viewModel.setForwardingEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PitchDark,
                                checkedTrackColor = AccentPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceSubtle,
                                uncheckedBorderColor = BorderDark
                            )
                        )
                    }
                }
            }

            // 5. Clean Metric Counters (Minimalist Monospaced / Pure White numbers)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CleanMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "RECEIVED",
                        count = stats.first.toString()
                    )
                    CleanMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "FORWARDED",
                        count = stats.second.toString()
                    )
                    CleanMetricCard(
                        modifier = Modifier.weight(1f),
                        label = "PENDING / FAIL",
                        count = stats.third.toString(),
                        isAlert = stats.third > 0
                    )
                }
            }

            // 6. Primary Action: Crisp Monochrome Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.sendTestMessage() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary,
                            contentColor = AccentPrimaryText
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        enabled = !isTesting
                    ) {
                        Text(
                            text = if (isTesting) "Sending Test..." else "Send Test WhatsApp",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (stats.third > 0) {
                        OutlinedButton(
                            onClick = { viewModel.retryAll() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(46.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text(
                                text = "Retry (${stats.third})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (records.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showClearConfirm = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(46.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) {
                            Text("Clear", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // 7. Recent Forwarding History
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${records.size} logged",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // 8. Transaction Cards or Empty State
            if (records.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Waiting for bKash alerts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Incoming bKash transaction SMS or notifications will appear here and auto-forward to WhatsApp.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                items(records, key = { it.trxId }) { record ->
                    MinimalistTransactionItem(
                        record = record,
                        onClick = { selectedRecord = record }
                    )
                }
            }

            // 9. Mandatory Discrete Bottom Attribution (Subtle / Low-Contrast / Unobtrusive)
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Developed by S. M. Mahmud Iqbal",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextFaint,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            preferences = viewModel.preferences,
            isTesting = isTesting,
            testResult = testResult,
            onTestConnection = { viewModel.sendTestMessage() },
            onDismiss = { showSettingsDialog = false },
            onSave = { mode, recipientType, forwardMode, textMeBotKey, callMeBotKey, phoneId, token, recipient, webhook, header, retry ->
                viewModel.saveSettings(
                    mode,
                    recipientType,
                    forwardMode,
                    textMeBotKey,
                    callMeBotKey,
                    phoneId,
                    token,
                    recipient,
                    webhook,
                    header,
                    retry
                )
            }
        )
    }

    // Detail Dialog
    selectedRecord?.let { record ->
        TransactionDetailDialog(
            record = record,
            onDismiss = { selectedRecord = null },
            onRetry = { viewModel.retryTransaction(it) }
        )
    }

    // Clear History Dialog
    if (showClearConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = SurfaceDark,
            title = { Text("Clear History?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text = { Text("This removes all logged transactions and reset counters.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel", color = TextPrimary)
                }
            }
        )
    }

    // Android 13/14/15 Restricted Settings Unlock Guide Dialog
    if (showRestrictedGuideDialog) {
        RestrictedSettingsGuideDialog(
            onOpenAppSettings = onOpenAppSettings,
            onOpenNotificationSettings = onOpenNotificationSettings,
            onDismiss = { showRestrictedGuideDialog = false }
        )
    }
}

@Composable
fun CleanMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    count: String,
    isAlert: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = modifier.border(1.dp, BorderDark, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isAlert) AccentAmber else TextPrimary
            )
        }
    }
}

@Composable
fun MinimalActionChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceSubtle)
            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            fontSize = 12.sp
        )
    }
}

@Composable
fun StatusPill(label: String, isGood: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceSubtle)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isGood) AccentEmerald else AccentAmber)
        )
        Text(
            text = label,
            color = if (isGood) AccentEmerald else AccentAmber,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp
        )
    }
}

@Composable
fun MinimalistTransactionItem(
    record: TransactionRecord,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(record.timestamp))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = record.trxId,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(text = "•", color = TextMuted, fontSize = 10.sp)
                    Text(
                        text = record.type,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "৳ ${record.amount}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                if (record.counterpart.isNotBlank()) {
                    Text(
                        text = "${record.counterpart} • $dateStr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                } else {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            MinimalStatusBadge(status = record.status)
        }
    }
}

@Composable
fun MinimalStatusBadge(status: ForwardStatus) {
    val (dotColor, label) = when (status) {
        ForwardStatus.SENT -> Pair(AccentEmerald, "Sent")
        ForwardStatus.FAILED -> Pair(AccentRose, "Failed")
        ForwardStatus.PENDING -> Pair(AccentAmber, "Pending")
        ForwardStatus.IGNORED_DUPLICATE -> Pair(TextMuted, "Duplicate")
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceSubtle)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            color = TextPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp
        )
    }
}
