package com.bkash.forwarder.ui

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bkash.forwarder.data.local.TransactionRecord
import com.bkash.forwarder.data.pref.AppPreferences
import com.bkash.forwarder.data.repository.ForwarderRepository
import com.bkash.forwarder.network.SendResult
import com.bkash.forwarder.service.SmsForwarderService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ForwarderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ForwarderRepository.getInstance(application)
    val preferences = repository.preferences

    val records: StateFlow<List<TransactionRecord>> = repository.recordsFlow
    val stats: StateFlow<Triple<Int, Int, Int>> = repository.statsFlow

    private val _isForwardingEnabled = MutableStateFlow(preferences.isForwardingEnabled)
    val isForwardingEnabled: StateFlow<Boolean> = _isForwardingEnabled.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()

    private val _isBatteryOptimizationIgnored = MutableStateFlow(true)
    val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored.asStateFlow()

    private val _isNotificationAccessEnabled = MutableStateFlow(false)
    val isNotificationAccessEnabled: StateFlow<Boolean> = _isNotificationAccessEnabled.asStateFlow()

    private val _hasSmsPermission = MutableStateFlow(false)
    val hasSmsPermission: StateFlow<Boolean> = _hasSmsPermission.asStateFlow()

    init {
        checkSystemStates()
    }

    fun checkSystemStates() {
        val app = getApplication<Application>()
        val powerManager = app.getSystemService(Context.POWER_SERVICE) as? PowerManager
        _isBatteryOptimizationIgnored.value = powerManager?.isIgnoringBatteryOptimizations(app.packageName) ?: true

        val flat = android.provider.Settings.Secure.getString(app.contentResolver, "enabled_notification_listeners")
        _isNotificationAccessEnabled.value = flat != null && flat.contains(app.packageName)

        val receive = androidx.core.content.ContextCompat.checkSelfPermission(
            app, android.Manifest.permission.RECEIVE_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        _hasSmsPermission.value = receive
    }

    fun checkBatteryOptimization() {
        checkSystemStates()
    }

    fun setForwardingEnabled(enabled: Boolean) {
        preferences.isForwardingEnabled = enabled
        _isForwardingEnabled.value = enabled
        if (enabled) {
            SmsForwarderService.startService(getApplication())
        } else {
            SmsForwarderService.stopService(getApplication())
        }
    }

    fun sendTestMessage() {
        viewModelScope.launch {
            _isTesting.value = true
            _testResult.value = null
            when (val result = repository.sendTestMessage()) {
                is SendResult.Success -> {
                    _testResult.value = "Success: Test message sent to WhatsApp!"
                }
                is SendResult.Failure -> {
                    _testResult.value = "Failed: ${result.errorMessage}"
                }
            }
            _isTesting.value = false
        }
    }

    fun retryTransaction(trxId: String) {
        viewModelScope.launch {
            repository.retryTransaction(trxId)
        }
    }

    fun retryAll() {
        viewModelScope.launch {
            repository.retryAllPendingAndFailed()
        }
    }

    fun clearAllHistory() {
        repository.clearAllHistory()
    }

    fun clearTestResult() {
        _testResult.value = null
    }

    fun saveSettings(
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
    ) {
        preferences.apiMode = apiMode
        preferences.recipientType = recipientType
        preferences.forwardMode = forwardMode
        preferences.textMeBotApiKey = textMeBotApiKey
        preferences.callMeBotApiKey = callMeBotApiKey
        preferences.telegramBotToken = telegramBotToken
        preferences.telegramChatId = telegramChatId
        preferences.cloudPhoneNumberId = phoneId
        preferences.cloudAccessToken = accessToken
        preferences.recipientWhatsAppNumber = recipient
        preferences.customWebhookUrl = webhookUrl
        preferences.customHeader = customHeader
        preferences.isAutoRetryEnabled = autoRetry
    }
}
