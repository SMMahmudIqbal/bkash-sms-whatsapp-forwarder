package com.bkash.forwarder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bkash.forwarder.data.pref.AppPreferences
import com.bkash.forwarder.ui.theme.AccentEmerald
import com.bkash.forwarder.ui.theme.AccentPrimary
import com.bkash.forwarder.ui.theme.AccentPrimaryText
import com.bkash.forwarder.ui.theme.AccentRose
import com.bkash.forwarder.ui.theme.BorderDark
import com.bkash.forwarder.ui.theme.PitchDark
import com.bkash.forwarder.ui.theme.SurfaceDark
import com.bkash.forwarder.ui.theme.SurfaceSubtle
import com.bkash.forwarder.ui.theme.TextMuted
import com.bkash.forwarder.ui.theme.TextPrimary
import com.bkash.forwarder.ui.theme.TextSecondary

@Composable
fun SettingsDialog(
    preferences: AppPreferences,
    isTesting: Boolean,
    testResult: String?,
    onTestConnection: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        apiMode: String,
        phoneId: String,
        accessToken: String,
        recipient: String,
        webhookUrl: String,
        customHeader: String,
        autoRetry: Boolean
    ) -> Unit
) {
    var apiMode by remember { mutableStateOf(preferences.apiMode) }
    var recipientNumber by remember { mutableStateOf(preferences.recipientWhatsAppNumber) }
    var phoneId by remember { mutableStateOf(preferences.cloudPhoneNumberId) }
    var accessToken by remember { mutableStateOf(preferences.cloudAccessToken) }
    var webhookUrl by remember { mutableStateOf(preferences.customWebhookUrl) }
    var customHeader by remember { mutableStateOf(preferences.customHeader) }
    var autoRetry by remember { mutableStateOf(preferences.isAutoRetryEnabled) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AccentPrimary,
        unfocusedBorderColor = BorderDark,
        focusedContainerColor = SurfaceSubtle,
        unfocusedContainerColor = SurfaceSubtle,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedLabelColor = TextPrimary,
        unfocusedLabelColor = TextMuted,
        cursorColor = AccentPrimary
    )

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
                .padding(vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "WhatsApp Integration",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Configure API delivery for incoming bKash alerts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Sleek Segmented Mode Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceSubtle)
                        .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (apiMode == "CLOUD_API") AccentPrimary else Color.Transparent)
                            .clickable { apiMode = "CLOUD_API" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Meta Cloud API",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (apiMode == "CLOUD_API") FontWeight.SemiBold else FontWeight.Normal,
                            color = if (apiMode == "CLOUD_API") AccentPrimaryText else TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (apiMode == "WEBHOOK") AccentPrimary else Color.Transparent)
                            .clickable { apiMode = "WEBHOOK" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Custom Webhook",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (apiMode == "WEBHOOK") FontWeight.SemiBold else FontWeight.Normal,
                            color = if (apiMode == "WEBHOOK") AccentPrimaryText else TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Recipient WhatsApp Number
                OutlinedTextField(
                    value = recipientNumber,
                    onValueChange = { recipientNumber = it },
                    label = { Text("Recipient WhatsApp Number") },
                    placeholder = { Text("88017XXXXXXXX") },
                    supportingText = { Text("Include country code (e.g. 880 for BD)", color = TextMuted) },
                    singleLine = true,
                    colors = textFieldColors,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (apiMode == "CLOUD_API") {
                    OutlinedTextField(
                        value = phoneId,
                        onValueChange = { phoneId = it },
                        label = { Text("Meta Phone Number ID") },
                        placeholder = { Text("109283746592817") },
                        singleLine = true,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = accessToken,
                        onValueChange = { accessToken = it },
                        label = { Text("System User Access Token") },
                        placeholder = { Text("EAABw...") },
                        supportingText = { Text("Permanent token from Meta Business Manager", color = TextMuted) },
                        colors = textFieldColors,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = webhookUrl,
                        onValueChange = { webhookUrl = it },
                        label = { Text("Gateway URL") },
                        placeholder = { Text("https://api.callmebot.com/whatsapp.php?phone=...&apikey=...") },
                        supportingText = { Text("CallMeBot, Baileys, Evolution API, or HTTP endpoint", color = TextMuted) },
                        colors = textFieldColors,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customHeader,
                    onValueChange = { customHeader = it },
                    label = { Text("Message Header") },
                    singleLine = true,
                    colors = textFieldColors,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Auto Retry Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-retry on Reconnect",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Automatically resend failed alerts when internet restores",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = autoRetry,
                        onCheckedChange = { autoRetry = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PitchDark,
                            checkedTrackColor = AccentPrimary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceSubtle,
                            uncheckedBorderColor = BorderDark
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Test Message Button
                OutlinedButton(
                    onClick = {
                        onSave(apiMode, phoneId, accessToken, recipientNumber, webhookUrl, customHeader, autoRetry)
                        onTestConnection()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isTesting && recipientNumber.isNotBlank(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            color = TextPrimary,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Sending Test...")
                    } else {
                        Text("Send Test WhatsApp Message", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (!testResult.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = testResult,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (testResult.startsWith("Success")) AccentEmerald else AccentRose,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save & Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onSave(apiMode, phoneId, accessToken, recipientNumber, webhookUrl, customHeader, autoRetry)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary,
                            contentColor = AccentPrimaryText
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Settings", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
