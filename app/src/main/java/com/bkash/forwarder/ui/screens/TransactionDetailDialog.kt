package com.bkash.forwarder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bkash.forwarder.data.local.ForwardStatus
import com.bkash.forwarder.data.local.TransactionRecord
import com.bkash.forwarder.ui.theme.AccentBkash
import com.bkash.forwarder.ui.theme.StatusFailed
import com.bkash.forwarder.ui.theme.StatusPending
import com.bkash.forwarder.ui.theme.StatusSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailDialog(
    record: TransactionRecord,
    onDismiss: () -> Unit,
    onRetry: (String) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(record.timestamp))

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with TrxID and Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TrxID: ${record.trxId}",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    StatusBadge(status = record.status)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount & Type Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = record.type,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AccentBkash,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "৳ ${record.amount}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (record.counterpart.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Party: ${record.counterpart}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (record.balance.isNotBlank()) {
                            Text(
                                text = "New Balance: ৳ ${record.balance}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        if (record.reference.isNotBlank()) {
                            Text(
                                text = "Ref: ${record.reference}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                if (!record.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = StatusFailed.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Forwarding Error:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusFailed,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = record.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusFailed
                            )
                            if (record.retryCount > 0) {
                                Text(
                                    text = "Retry attempts: ${record.retryCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StatusFailed.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Raw SMS Card
                Text(
                    text = "Raw SMS Message",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = record.rawSms,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }

                    if (record.status != ForwardStatus.SENT) {
                        Button(
                            onClick = {
                                onRetry(record.trxId)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBkash),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Retry Now")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: ForwardStatus) {
    val (bg, fg, label) = when (status) {
        ForwardStatus.SENT -> Triple(StatusSuccess.copy(alpha = 0.2f), StatusSuccess, "SENT")
        ForwardStatus.FAILED -> Triple(StatusFailed.copy(alpha = 0.2f), StatusFailed, "FAILED")
        ForwardStatus.PENDING -> Triple(StatusPending.copy(alpha = 0.2f), StatusPending, "PENDING")
        ForwardStatus.IGNORED_DUPLICATE -> Triple(Color.Gray.copy(alpha = 0.2f), Color.Gray, "DUPLICATE")
    }

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
