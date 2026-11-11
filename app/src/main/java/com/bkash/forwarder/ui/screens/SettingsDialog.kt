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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
        recipientType: String,
        forwardMode: String,
        textMeBotApiKey: String,
        callMeBotApiKey: String,
        phoneId: String,
        accessToken: String,
        recipient: String,
        webhookUrl: String,
        customHeader: String,
        autoRetry: Boolean
    ) -> Unit
) {
    var recipientType by remember { mutableStateOf(preferences.recipientType) }
    var forwardMode by remember { mutableStateOf(preferences.forwardMode) }
    var apiMode by remember { mutableStateOf(preferences.apiMode) }
    var recipientNumber by remember { mutableStateOf(preferences.recipientWhatsAppNumber) }
    var textMeBotApiKey by remember { mutableStateOf(preferences.textMeBotApiKey) }
    var callMeBotApiKey by remember { mutableStateOf(preferences.callMeBotApiKey) }
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
                    text = "Configure auto-forwarding to your WhatsApp group",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Destination Target Selector: WhatsApp Group vs Individual Number
                Text(
                    text = "DESTINATION TARGET",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceSubtle)
                        .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                        .padding(3.dp)
                ) {
                    ModeTabItem(
                        label = "WhatsApp Group",
                        selected = recipientType == "GROUP",
                        onClick = {
                            recipientType = "GROUP"
                            if (apiMode == "CALLMEBOT") {
                                apiMode = "TEXTMEBOT" // CallMeBot doesn't support groups
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ModeTabItem(
                        label = "Individual Number",
                        selected = recipientType == "INDIVIDUAL",
                        onClick = { recipientType = "INDIVIDUAL" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Message Format Selector: Exact Copy-Paste (Raw) vs Formatted Receipt Card
                Text(
                    text = "FORWARDING FORMAT",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceSubtle)
                        .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                        .padding(3.dp)
                ) {
                    ModeTabItem(
                        label = "Raw Copy-Paste",
                        selected = forwardMode == "RAW",
                        onClick = { forwardMode = "RAW" },
                        modifier = Modifier.weight(1f)
                    )
                    ModeTabItem(
                        label = "Receipt Card",
                        selected = forwardMode == "FORMATTED",
                        onClick = { forwardMode = "FORMATTED" },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (forwardMode == "RAW") {
                        "Forwards the exact bKash message received on your phone without modifying text."
                    } else {
                        "Formats transaction into a structured card with emojis and line breakdown."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Target Identifier (Group ID or Phone Number)
                if (recipientType == "GROUP") {
                    OutlinedTextField(
                        value = recipientNumber,
                        onValueChange = { recipientNumber = it },
                        label = { Text("WhatsApp Group ID") },
                        placeholder = { Text("120363040377656518@g.us") },
                        supportingText = {
                            Text("Group JID (ends with @g.us) or numeric Group ID", color = TextMuted)
                        },
                        singleLine = true,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = recipientNumber,
                        onValueChange = { recipientNumber = it },
                        label = { Text("Your WhatsApp Number") },
                        placeholder = { Text("88017XXXXXXXX") },
                        supportingText = {
                            Text("International format with country code (e.g. 88017...)", color = TextMuted)
                        },
                        singleLine = true,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. API Provider Selector
                Text(
                    text = "DELIVERY PROVIDER",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceSubtle)
                        .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                        .padding(3.dp)
                ) {
                    ModeTabItem(
                        label = "TextMeBot",
                        selected = apiMode == "TEXTMEBOT",
                        onClick = { apiMode = "TEXTMEBOT" },
                        modifier = Modifier.weight(1f)
                    )
                    ModeTabItem(
                        label = "Gateway",
                        selected = apiMode == "WEBHOOK",
                        onClick = { apiMode = "WEBHOOK" },
                        modifier = Modifier.weight(1f)
                    )
                    if (recipientType == "INDIVIDUAL") {
                        ModeTabItem(
                            label = "CallMeBot",
                            selected = apiMode == "CALLMEBOT",
                            onClick = { apiMode = "CALLMEBOT" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ModeTabItem(
                        label = "Cloud API",
                        selected = apiMode == "CLOUD_API",
                        onClick = { apiMode = "CLOUD_API" },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Provider-specific credentials
                when (apiMode) {
                    "TEXTMEBOT" -> {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "⚡ TextMeBot (Group & 1-to-1):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "1. Add TextMeBot to your WhatsApp group (or activate bot on WhatsApp).\n2. Enter your API Key below.\n3. Make sure the Group ID above ends with @g.us.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = textMeBotApiKey,
                            onValueChange = { textMeBotApiKey = it },
                            label = { Text("TextMeBot API Key") },
                            placeholder = { Text("e.g. AB12CD34EF") },
                            singleLine = true,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    "WEBHOOK" -> {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🌐 WhatsApp Gateway / Webhook:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Supports UltraMsg, Green API, Evolution API, Baileys, or any HTTP webhook. Payload automatically passes 'to' / 'chatId' and 'message' / 'body'.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = webhookUrl,
                            onValueChange = { webhookUrl = it },
                            label = { Text("Gateway Endpoint URL") },
                            placeholder = { Text("https://api.ultramsg.com/... or https://your-server.com/send") },
                            supportingText = { Text("HTTP POST or GET with template variables", color = TextMuted) },
                            colors = textFieldColors,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    "CALLMEBOT" -> {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "⚡ 30-Second Setup (Individual Only):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "1. Send: 'I allow callmebot to send me messages' to +34 941 83 07 48 on WhatsApp.\n2. Paste the API key you receive below.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = callMeBotApiKey,
                            onValueChange = { callMeBotApiKey = it },
                            label = { Text("CallMeBot API Key") },
                            placeholder = { Text("e.g. 948201") },
                            singleLine = true,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    "CLOUD_API" -> {
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
                    }
                }

                if (forwardMode == "FORMATTED") {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customHeader,
                        onValueChange = { customHeader = it },
                        label = { Text("Alert Header") },
                        singleLine = true,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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
                            text = "Resend failed alerts when internet restores",
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
                        onSave(
                            apiMode,
                            recipientType,
                            forwardMode,
                            textMeBotApiKey,
                            callMeBotApiKey,
                            phoneId,
                            accessToken,
                            recipientNumber,
                            webhookUrl,
                            customHeader,
                            autoRetry
                        )
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
                        Text(
                            text = if (recipientType == "GROUP") "Send Test to WhatsApp Group" else "Send Test WhatsApp Message",
                            style = MaterialTheme.typography.bodyMedium
                        )
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
                            onSave(
                                apiMode,
                                recipientType,
                                forwardMode,
                                textMeBotApiKey,
                                callMeBotApiKey,
                                phoneId,
                                accessToken,
                                recipientNumber,
                                webhookUrl,
                                customHeader,
                                autoRetry
                            )
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

@Composable
private fun ModeTabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) AccentPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) AccentPrimaryText else TextSecondary,
            fontSize = 12.sp
        )
    }
}
