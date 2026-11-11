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
        recipientNumber: String = preferences.getFormattedRecipientNumber(),
        messageText: String,
        trxId: String = ""
    ): SendResult = withContext(Dispatchers.IO) {
        if (recipientNumber.isBlank()) {
            return@withContext SendResult.Failure("Recipient WhatsApp number is not configured.")
        }

        if (preferences.apiMode == "WEBHOOK") {
            sendViaWebhook(recipientNumber, messageText, trxId)
        } else {
            sendViaCloudApi(recipientNumber, messageText)
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
     * Sends message using Custom Webhook or third-party WhatsApp gateway.
     */
    private fun sendViaWebhook(recipient: String, messageText: String, trxId: String): SendResult {
        val webhookUrl = preferences.customWebhookUrl
        if (webhookUrl.isBlank()) {
            return SendResult.Failure("Custom Webhook URL is missing.")
        }

        return try {
            val request: Request = if (webhookUrl.contains("callmebot.com", ignoreCase = true)) {
                // CallMeBot query param format: https://api.callmebot.com/whatsapp.php?phone=...&text=...&apikey=...
                val encodedText = URLEncoder.encode(messageText, "UTF-8")
                val urlWithParams = if (webhookUrl.contains("?")) {
                    "$webhookUrl&text=$encodedText"
                } else {
                    "$webhookUrl?phone=$recipient&text=$encodedText"
                }
                Request.Builder().url(urlWithParams).get().build()
            } else {
                val payload = JSONObject().apply {
                    put("to", recipient)
                    put("message", messageText)
                    put("trx_id", trxId)
                    put("timestamp", System.currentTimeMillis())
                }
                val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
                Request.Builder().url(webhookUrl).post(body).build()
            }

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                SendResult.Success(messageId = null, responseBody = responseString)
            } else {
                SendResult.Failure(
                    errorMessage = "Webhook returned HTTP ${response.code}: $responseString",
                    statusCode = response.code
                )
            }
        } catch (e: IOException) {
            SendResult.Failure(
                errorMessage = "Webhook network error: ${e.localizedMessage}",
                isNetworkError = true
            )
        } catch (e: Exception) {
            SendResult.Failure(
                errorMessage = "Webhook failed: ${e.localizedMessage}",
                isNetworkError = false
            )
        }
    }
}
