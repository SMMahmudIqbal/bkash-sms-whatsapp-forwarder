package com.bkash.forwarder.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bkash.forwarder.data.pref.AppPreferences
import com.bkash.forwarder.ui.theme.AccentBkash
import com.bkash.forwarder.ui.theme.AccentGreen

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "WhatsApp Integration Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Configure automated dispatch to your WhatsApp number",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Selector Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (apiMode == "CLOUD_API") AccentBkash else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { apiMode = "CLOUD_API" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Meta Cloud API",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (apiMode == "CLOUD_API") FontWeight.Bold else FontWeight.Normal,
                            color = if (apiMode == "CLOUD_API") Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (apiMode == "WEBHOOK") AccentBkash else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { apiMode = "WEBHOOK" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Custom Webhook",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (apiMode == "WEBHOOK") FontWeight.Bold else FontWeight.Normal,
                            color = if (apiMode == "WEBHOOK") Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Recipient WhatsApp Number (common to both)
                OutlinedTextField(
                    value = recipientNumber,
                    onValueChange = { recipientNumber = it },
                    label = { Text("WhatsApp Recipient Number") },
                    placeholder = { Text("e.g. 8801712345678 or 01712345678") },
                    supportingText = { Text("Include country code (e.g. 88 for Bangladesh)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (apiMode == "CLOUD_API") {
                    OutlinedTextField(
                        value = phoneId,
                        onValueChange = { phoneId = it },
                        label = { Text("Meta Phone Number ID") },
                        placeholder = { Text("e.g. 109283746592817") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = accessToken,
                        onValueChange = { accessToken = it },
                        label = { Text("Meta System User Access Token") },
                        placeholder = { Text("EAABw...") },
                        supportingText = { Text("Permanent access token from Meta Developer Portal") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = webhookUrl,
                        onValueChange = { webhookUrl = it },
                        label = { Text("Webhook / Gateway URL") },
                        placeholder = { Text("https://api.callmebot.com/whatsapp.php?phone=...&apikey=...") },
                        supportingText = { Text("Supports CallMeBot, Baileys, Evolution API, or custom HTTP endpoint") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customHeader,
                    onValueChange = { customHeader = it },
                    label = { Text("Custom Message Header") },
                    singleLine = true,
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
                            text = "Auto-retry on Reconnection",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Automatically resend failed alerts when internet restores",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = autoRetry,
                        onCheckedChange = { autoRetry = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentGreen)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Test Message Section
                Button(
                    onClick = {
                        // Temporarily apply values before testing
                        onSave(apiMode, phoneId, accessToken, recipientNumber, webhookUrl, customHeader, autoRetry)
                        onTestConnection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTesting && recipientNumber.isNotBlank()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Sending Test Message...")
                    } else {
                        Text("Send Test WhatsApp Message")
                    }
                }

                if (!testResult.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = testResult,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (testResult.startsWith("Success")) AccentGreen else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onSave(apiMode, phoneId, accessToken, recipientNumber, webhookUrl, customHeader, autoRetry)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBkash),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Settings")
                    }
                }
            }
        }
    }
}
