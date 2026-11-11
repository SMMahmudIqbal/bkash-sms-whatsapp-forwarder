package com.bkash.forwarder.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
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
        telegramBotToken: String,
        telegramChatId: String,
        phoneId: String,
        accessToken: String,
        recipient: String,
        webhookUrl: String,
        customHeader: String,
        autoRetry: Boolean
    ) -> Unit
) {
    val context = LocalContext.current

    var recipientType by remember { mutableStateOf(preferences.recipientType) }
    var forwardMode by remember { mutableStateOf(preferences.forwardMode) }
    var apiMode by remember { mutableStateOf(preferences.apiMode) }
    var recipientNumber by remember { mutableStateOf(preferences.recipientWhatsAppNumber) }
    var textMeBotApiKey by remember { mutableStateOf(preferences.textMeBotApiKey) }
    var callMeBotApiKey by remember { mutableStateOf(preferences.callMeBotApiKey) }
    var telegramBotToken by remember { mutableStateOf(preferences.telegramBotToken) }
    var telegramChatId by remember { mutableStateOf(preferences.telegramChatId) }
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
                    text = "Forwarding Integration",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Configure auto-forwarding to WhatsApp or Telegram",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Destination Target: Group vs Individual
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
                        label = "Group",
                        selected = recipientType == "GROUP",
                        onClick = { recipientType = "GROUP" },
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

                // 2. Format Selector: Raw Copy-Paste vs Receipt Card
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

                // 3. Target Identifier
                if (recipientType == "GROUP") {
                    OutlinedTextField(
                        value = recipientNumber,
                        onValueChange = { recipientNumber = it },
                        label = { Text("Group ID") },
                        placeholder = { Text("120363040377656518@g.us") },
                        supportingText = {
                            Text("WhatsApp Group JID (ends with @g.us) or Telegram Chat ID", color = TextMuted)
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
                        placeholder = { Text("8801540086883") },
                        supportingText = {
                            Text("International format with country code (e.g. 88015...)", color = TextMuted)
                        },
                        singleLine = true,
                        colors = textFieldColors,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Delivery Provider Selector
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
                        label = "CallMeBot",
                        selected = apiMode == "CALLMEBOT",
                        onClick = { apiMode = "CALLMEBOT" },
                        modifier = Modifier.weight(1f)
                    )
                    ModeTabItem(
                        label = "Telegram",
                        selected = apiMode == "TELEGRAM",
                        onClick = { apiMode = "TELEGRAM" },
                        modifier = Modifier.weight(1f)
                    )
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
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Provider-specific credentials
                when (apiMode) {
                    "CALLMEBOT" -> {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "⚡ CallMeBot (100% Free - Personal WhatsApp):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Get your free API key in 5 seconds via WhatsApp:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        val uri = Uri.parse("https://api.whatsapp.com/send?phone=34941830748&text=I%20allow%20callmebot%20to%20send%20me%20messages")
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Fallback
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentEmerald,
                                        contentColor = PitchDark
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp)
                                ) {
                                    Text("1-Tap: Get Free Key on WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Send the pre-filled text in WhatsApp, then copy the API key you receive and paste it below.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
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

                    "TELEGRAM" -> {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "✈️ Telegram Group Bot (100% Free Forever):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "1. In Telegram, search @BotFather and send /newbot to create your bot.\n2. Copy the Bot Token and paste it below.\n3. Add your new bot to your Telegram Group.\n4. Enter the Group Chat ID (e.g. -100123456789) below.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = telegramBotToken,
                            onValueChange = { telegramBotToken = it },
                            label = { Text("Telegram Bot Token") },
                            placeholder = { Text("123456789:ABCdefGHIjklMNOpqrs") },
                            singleLine = true,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = telegramChatId,
                            onValueChange = { telegramChatId = it },
                            label = { Text("Telegram Chat / Group ID") },
                            placeholder = { Text("-1001234567890 or @your_channel") },
                            singleLine = true,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

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
                                    text = "⚠️ TextMeBot Notice (Requires Paid Plan):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentRose
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "TextMeBot requires purchasing an active key from textmebot.com. Without a paid plan, requests return 'Invalid Premium APIkey'.\n\n💡 Tip: For 100% free delivery, use CallMeBot (for WhatsApp) or Telegram (for Groups).",
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
                            label = { Text("TextMeBot Premium API Key") },
                            placeholder = { Text("Enter your paid API key") },
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
                            telegramBotToken,
                            telegramChatId,
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
                    enabled = !isTesting && (recipientNumber.isNotBlank() || telegramChatId.isNotBlank()),
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
                        val testLabel = when {
                            apiMode == "TELEGRAM" -> "Send Test to Telegram"
                            recipientType == "GROUP" -> "Send Test to WhatsApp Group"
                            else -> "Send Test WhatsApp Message"
                        }
                        Text(text = testLabel, style = MaterialTheme.typography.bodyMedium)
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
                                telegramBotToken,
                                telegramChatId,
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
