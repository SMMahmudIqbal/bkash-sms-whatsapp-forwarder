package com.bkash.forwarder.network

import android.util.Log
import com.bkash.forwarder.data.pref.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class WhatsAppApiService(private val preferences: AppPreferences) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "WhatsAppApiService"
        private const val GRAPH_API_BASE = "https://graph.facebook.com/v21.0"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Sends formatted text via configured WhatsApp integration.
     */
    suspend fun sendMessage(
        recipient: String = preferences.getFormattedRecipient(),
        messageText: String,
        trxId: String = ""
    ): SendResult = withContext(Dispatchers.IO) {
        if (recipient.isBlank()) {
            val error = if (preferences.recipientType == "GROUP") {
                "WhatsApp Group ID is not configured. Enter your Group ID in Settings."
            } else {
                "Recipient WhatsApp number is not configured. Enter your number in Settings."
            }
            return@withContext SendResult.Failure(error)
        }

        when (preferences.apiMode) {
            "TEXTMEBOT" -> sendViaTextMeBot(recipient, messageText)
            "CALLMEBOT" -> sendViaCallMeBot(recipient, messageText)
            "WEBHOOK" -> sendViaWebhook(recipient, messageText, trxId)
            else -> sendViaCloudApi(recipient, messageText)
        }
    }

    /**
     * Sends message to WhatsApp Group or Individual via TextMeBot API (Direct Group support).
     */
    private fun sendViaTextMeBot(recipient: String, messageText: String): SendResult {
        val apiKey = preferences.textMeBotApiKey
        if (apiKey.isBlank()) {
            return SendResult.Failure("TextMeBot API Key is missing. Enter your key in Settings.")
        }

        return try {
            val encodedRecipient = URLEncoder.encode(recipient, "UTF-8")
            val encodedText = URLEncoder.encode(messageText, "UTF-8")
            val url = "https://api.textmebot.com/send.php?recipient=$encodedRecipient&apikey=$apiKey&text=$encodedText"

            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (response.isSuccessful && !responseString.contains("ERROR", ignoreCase = true)) {
                Log.i(TAG, "TextMeBot message delivered successfully to $recipient.")
                SendResult.Success(messageId = null, responseBody = responseString)
            } else {
                SendResult.Failure("TextMeBot Error: $responseString", statusCode = response.code)
            }
        } catch (e: IOException) {
            SendResult.Failure("Network error: ${e.localizedMessage}", isNetworkError = true)
        } catch (e: Exception) {
            SendResult.Failure("TextMeBot error: ${e.localizedMessage}")
        }
    }

    /**
     * Sends message using CallMeBot WhatsApp API (Fast, Free, 30-Second Setup for personal 1-to-1).
     */
    private fun sendViaCallMeBot(recipient: String, messageText: String): SendResult {
        val apiKey = preferences.callMeBotApiKey
        if (apiKey.isBlank()) {
            return SendResult.Failure("CallMeBot API Key is missing. Enter your key in Settings.")
        }

        val cleanPhone = preferences.getFormattedRecipientNumber()
        if (cleanPhone.isBlank()) {
            return SendResult.Failure("Recipient phone number is missing.")
        }

        return try {
            val encodedText = URLEncoder.encode(messageText, "UTF-8")
            val url = "https://api.callmebot.com/whatsapp.php?phone=$cleanPhone&text=$encodedText&apikey=$apiKey"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (response.isSuccessful && !responseString.contains("ERROR", ignoreCase = true)) {
                Log.i(TAG, "CallMeBot message delivered successfully.")
                SendResult.Success(messageId = null, responseBody = responseString)
            } else {
                SendResult.Failure("CallMeBot Error: $responseString", statusCode = response.code)
            }
        } catch (e: IOException) {
            SendResult.Failure("Network error: ${e.localizedMessage}", isNetworkError = true)
        } catch (e: Exception) {
            SendResult.Failure("CallMeBot error: ${e.localizedMessage}")
        }
    }

    /**
     * Sends message using official Meta WhatsApp Business Cloud API.
     */
    private fun sendViaCloudApi(recipient: String, messageText: String): SendResult {
        val phoneId = preferences.cloudPhoneNumberId
        val accessToken = preferences.cloudAccessToken

        if (phoneId.isBlank() || accessToken.isBlank()) {
            return SendResult.Failure("WhatsApp Cloud API Phone ID or Access Token is missing.")
        }

        val endpoint = "$GRAPH_API_BASE/$phoneId/messages"

        val payload = JSONObject().apply {
            put("messaging_product", "whatsapp")
            put("recipient_type", "individual")
            put("to", recipient)
            put("type", "text")
            put("text", JSONObject().apply {
                put("preview_url", false)
                put("body", messageText)
            })
        }

        val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        return try {
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""
            val code = response.code

            Log.d(TAG, "Cloud API Response [$code]: $responseString")

            if (response.isSuccessful) {
                var messageId: String? = null
                try {
                    val json = JSONObject(responseString)
                    val messages = json.optJSONArray("messages")
                    if (messages != null && messages.length() > 0) {
                        messageId = messages.getJSONObject(0).optString("id")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing message id: ${e.message}")
                }
                SendResult.Success(messageId = messageId, responseBody = responseString)
            } else {
                var errorMessage = "HTTP $code"
                try {
                    val json = JSONObject(responseString)
                    val errorObj = json.optJSONObject("error")
                    if (errorObj != null) {
                        val msg = errorObj.optString("message")
                        val errCode = errorObj.optInt("code")
                        errorMessage = "Meta API Error ($errCode): $msg"
                    }
                } catch (e: Exception) {
                    errorMessage = "HTTP $code: $responseString"
                }
                SendResult.Failure(errorMessage = errorMessage, statusCode = code, isNetworkError = false)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network I/O error: ${e.message}", e)
            SendResult.Failure(
                errorMessage = "Network error: ${e.localizedMessage ?: "Connection failed"}",
                isNetworkError = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            SendResult.Failure(
                errorMessage = "Unexpected error: ${e.localizedMessage ?: "Unknown"}",
                isNetworkError = false
            )
        }
    }

    /**
     * Sends message using Custom Webhook or third-party WhatsApp gateway (UltraMsg, Green API, Evolution, Baileys, etc.).
     */
    private fun sendViaWebhook(recipient: String, messageText: String, trxId: String): SendResult {
        val webhookUrl = preferences.customWebhookUrl
        if (webhookUrl.isBlank()) {
            return SendResult.Failure("Custom Webhook / Gateway URL is missing. Enter it in Settings.")
        }

        return try {
            val request: Request = when {
                // Template URL with placeholders like {recipient}, {group}, {to}, {text}, {message}, {msg}
                webhookUrl.contains("{recipient}") || webhookUrl.contains("{group}") ||
                        webhookUrl.contains("{to}") || webhookUrl.contains("{text}") ||
                        webhookUrl.contains("{message}") || webhookUrl.contains("{msg}") -> {
                    val encodedText = URLEncoder.encode(messageText, "UTF-8")
                    val encodedRecipient = URLEncoder.encode(recipient, "UTF-8")
                    val resolvedUrl = webhookUrl
                        .replace("{recipient}", encodedRecipient)
                        .replace("{group}", encodedRecipient)
                        .replace("{to}", encodedRecipient)
                        .replace("{text}", encodedText)
                        .replace("{message}", encodedText)
                        .replace("{msg}", encodedText)
                        .replace("{trxId}", trxId)
                    Request.Builder().url(resolvedUrl).get().build()
                }

                // CallMeBot query param format: https://api.callmebot.com/whatsapp.php?phone=...&text=...&apikey=...
                webhookUrl.contains("callmebot.com", ignoreCase = true) -> {
                    val encodedText = URLEncoder.encode(messageText, "UTF-8")
                    val urlWithParams = if (webhookUrl.contains("?")) {
                        "$webhookUrl&text=$encodedText"
                    } else {
                        "$webhookUrl?phone=$recipient&text=$encodedText"
                    }
                    Request.Builder().url(urlWithParams).get().build()
                }

                // Standard WhatsApp Gateway JSON POST (UltraMsg, Green API, Evolution, Baileys, etc.)
                else -> {
                    val payload = JSONObject().apply {
                        put("to", recipient)
                        put("chatId", recipient)
                        put("recipient", recipient)
                        put("group", recipient)
                        put("message", messageText)
                        put("text", messageText)
                        put("body", messageText)
                        put("raw", messageText)
                        put("trx_id", trxId)
                        put("timestamp", System.currentTimeMillis())
                    }
                    val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
                    Request.Builder().url(webhookUrl).post(body).build()
                }
            }

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                SendResult.Success(messageId = null, responseBody = responseString)
            } else {
                SendResult.Failure(
                    errorMessage = "Gateway returned HTTP ${response.code}: $responseString",
                    statusCode = response.code
                )
            }
        } catch (e: IOException) {
            SendResult.Failure(
                errorMessage = "Gateway network error: ${e.localizedMessage}",
                isNetworkError = true
            )
        } catch (e: Exception) {
            SendResult.Failure(
                errorMessage = "Gateway failed: ${e.localizedMessage}",
                isNetworkError = false
            )
        }
    }
}
