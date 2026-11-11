package com.bkash.forwarder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bkash.forwarder.ui.theme.AccentPrimary
import com.bkash.forwarder.ui.theme.AccentPrimaryText
import com.bkash.forwarder.ui.theme.BorderDark
import com.bkash.forwarder.ui.theme.SurfaceDark
import com.bkash.forwarder.ui.theme.SurfaceSubtle
import com.bkash.forwarder.ui.theme.TextMuted
import com.bkash.forwarder.ui.theme.TextPrimary
import com.bkash.forwarder.ui.theme.TextSecondary

@Composable
fun RestrictedSettingsGuideDialog(
    onOpenAppSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfaceDark,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(1.dp, BorderDark, RoundedCornerShape(18.dp))
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "SMS Permission Unlock",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Android 13 / 14 / 15 / 16 Restricted Settings",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Android automatically locks SMS permission for sideloaded APKs. You can unlock it in 10 seconds following these steps:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                CleanStepItem(
                    step = "1",
                    title = "Tap 'Open App Settings' below",
                    description = "Opens the phone's App Info page for bKash Forwarder."
                )

                CleanStepItem(
                    step = "2",
                    title = "Tap 3 vertical dots (⋮) top-right",
                    description = "In the top right corner of the system page, tap ⋮ (or 'More')."
                )

                CleanStepItem(
                    step = "3",
                    title = "Tap 'Allow restricted settings'",
                    description = "Confirm with your fingerprint or screen lock PIN."
                )

                CleanStepItem(
                    step = "4",
                    title = "Tap 'Permissions' ➔ 'SMS' ➔ 'Allow'",
                    description = "SMS access is now unlocked! Return to the app."
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Primary Action Button (Sleek solid white)
                Button(
                    onClick = {
                        onOpenAppSettings()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary,
                        contentColor = AccentPrimaryText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Open App Settings",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Alternative: Notification Interception
                OutlinedButton(
                    onClick = {
                        onOpenNotificationSettings()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Text(
                        text = "Or Use Notification Access",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun CleanStepItem(
    step: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(SurfaceSubtle)
                .border(1.dp, BorderDark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                color = TextPrimary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                fontSize = 13.sp
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
