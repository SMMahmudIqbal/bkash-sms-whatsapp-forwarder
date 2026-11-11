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
        private const val KEY_API_MODE = "key_api_mode" // "CALLMEBOT", "CLOUD_API", or "WEBHOOK"
        private const val KEY_CALLMEBOT_API_KEY = "key_callmebot_api_key"
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
        get() = prefs.getString(KEY_API_MODE, "CALLMEBOT") ?: "CALLMEBOT"
        set(value) = prefs.edit().putString(KEY_API_MODE, value).apply()

    var callMeBotApiKey: String
        get() = prefs.getString(KEY_CALLMEBOT_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CALLMEBOT_API_KEY, value.trim()).apply()

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
        return if (apiMode == "CLOUD_API") {
            cloudPhoneNumberId.isNotBlank() && cloudAccessToken.isNotBlank() && recipientWhatsAppNumber.isNotBlank()
        } else {
            customWebhookUrl.isNotBlank() && recipientWhatsAppNumber.isNotBlank()
        }
    }
}
