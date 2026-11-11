package com.bkash.forwarder.data.pref

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "bkash_forwarder_prefs"

        private const val KEY_FORWARDING_ENABLED = "key_forwarding_enabled"
        private const val KEY_API_MODE = "key_api_mode" // "TEXTMEBOT", "WEBHOOK", "CALLMEBOT", "CLOUD_API"
        private const val KEY_RECIPIENT_TYPE = "key_recipient_type" // "GROUP" or "INDIVIDUAL"
        private const val KEY_FORWARD_MODE = "key_forward_mode" // "RAW" or "FORMATTED"
        private const val KEY_TEXTMEBOT_API_KEY = "key_textmebot_api_key"
        private const val KEY_CALLMEBOT_API_KEY = "key_callmebot_api_key"
        private const val KEY_TELEGRAM_BOT_TOKEN = "key_telegram_bot_token"
        private const val KEY_TELEGRAM_CHAT_ID = "key_telegram_chat_id"
        private const val KEY_PHONE_NUMBER_ID = "key_phone_number_id"
        private const val KEY_ACCESS_TOKEN = "key_access_token"
        private const val KEY_RECIPIENT_NUMBER = "key_recipient_number"
        private const val KEY_WEBHOOK_URL = "key_webhook_url"
        private const val KEY_CUSTOM_HEADER = "key_custom_header"
        private const val KEY_SECURITY_PIN = "key_security_pin"
        private const val KEY_PIN_LOCK_ENABLED = "key_pin_lock_enabled"
        private const val KEY_AUTO_RETRY_ENABLED = "key_auto_retry_enabled"

        @Volatile
        private var instance: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
        }
    }

    var isForwardingEnabled: Boolean
        get() = prefs.getBoolean(KEY_FORWARDING_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_FORWARDING_ENABLED, value).apply()

    var apiMode: String
        get() = prefs.getString(KEY_API_MODE, "TEXTMEBOT") ?: "TEXTMEBOT"
        set(value) = prefs.edit().putString(KEY_API_MODE, value).apply()

    var recipientType: String
        get() = prefs.getString(KEY_RECIPIENT_TYPE, "GROUP") ?: "GROUP"
        set(value) = prefs.edit().putString(KEY_RECIPIENT_TYPE, value).apply()

    var forwardMode: String
        get() = prefs.getString(KEY_FORWARD_MODE, "RAW") ?: "RAW"
        set(value) = prefs.edit().putString(KEY_FORWARD_MODE, value).apply()

    var textMeBotApiKey: String
        get() = prefs.getString(KEY_TEXTMEBOT_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TEXTMEBOT_API_KEY, value.trim()).apply()

    var callMeBotApiKey: String
        get() = prefs.getString(KEY_CALLMEBOT_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CALLMEBOT_API_KEY, value.trim()).apply()

    var telegramBotToken: String
        get() = prefs.getString(KEY_TELEGRAM_BOT_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TELEGRAM_BOT_TOKEN, value.trim()).apply()

    var telegramChatId: String
        get() = prefs.getString(KEY_TELEGRAM_CHAT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TELEGRAM_CHAT_ID, value.trim()).apply()

    var cloudPhoneNumberId: String
        get() = prefs.getString(KEY_PHONE_NUMBER_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PHONE_NUMBER_ID, value.trim()).apply()

    var cloudAccessToken: String
        get() = prefs.getString(KEY_ACCESS_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value.trim()).apply()

    var recipientWhatsAppNumber: String
        get() = prefs.getString(KEY_RECIPIENT_NUMBER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_RECIPIENT_NUMBER, value.trim()).apply()

    var customWebhookUrl: String
        get() = prefs.getString(KEY_WEBHOOK_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WEBHOOK_URL, value.trim()).apply()

    var customHeader: String
        get() = prefs.getString(KEY_CUSTOM_HEADER, "🔔 *bKash Transaction Alert*") ?: "🔔 *bKash Transaction Alert*"
        set(value) = prefs.edit().putString(KEY_CUSTOM_HEADER, value.trim()).apply()

    var isPinLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_PIN_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PIN_LOCK_ENABLED, value).apply()

    var securityPin: String
        get() = prefs.getString(KEY_SECURITY_PIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SECURITY_PIN, value.trim()).apply()

    var isAutoRetryEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RETRY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RETRY_ENABLED, value).apply()

    /**
     * Resolves the target recipient based on whether it is a WhatsApp Group or individual phone number.
     * For Groups: ensures the ID ends with @g.us (e.g., 120363040377656518@g.us).
     * For Individuals: formats with country code (e.g., 88017XXXXXXXX).
     */
    fun getFormattedRecipient(): String {
        val trimmed = recipientWhatsAppNumber.trim()
        if (trimmed.isBlank()) return ""

        if (recipientType == "GROUP" || trimmed.contains("@g.us", ignoreCase = true)) {
            return if (trimmed.endsWith("@g.us", ignoreCase = true)) {
                trimmed
            } else {
                "${trimmed.replace(Regex("[^0-9]"), "")}@g.us"
            }
        }

        return getFormattedRecipientNumber()
    }

    /**
     * Cleans up phone number to international E.164 standard without plus symbol (e.g., 88017XXXXXXXX)
     */
    fun getFormattedRecipientNumber(): String {
        val raw = recipientWhatsAppNumber.replace(Regex("[^0-9]"), "")
        if (raw.isBlank()) return ""
        // If user entered 017XXXXXXXX (11 digits), prepend Bangladesh country code 88
        return if (raw.startsWith("01") && raw.length == 11) {
            "88$raw"
        } else {
            raw
        }
    }

    fun isConfigured(): Boolean {
        return when (apiMode) {
            "TEXTMEBOT" -> textMeBotApiKey.isNotBlank() && recipientWhatsAppNumber.isNotBlank()
            "CALLMEBOT" -> callMeBotApiKey.isNotBlank() && recipientWhatsAppNumber.isNotBlank()
            "TELEGRAM" -> telegramBotToken.isNotBlank() && (telegramChatId.isNotBlank() || recipientWhatsAppNumber.isNotBlank())
            "CLOUD_API" -> cloudPhoneNumberId.isNotBlank() && cloudAccessToken.isNotBlank() && recipientWhatsAppNumber.isNotBlank()
            "WEBHOOK" -> customWebhookUrl.isNotBlank() && recipientWhatsAppNumber.isNotBlank()
            else -> false
        }
    }
}
