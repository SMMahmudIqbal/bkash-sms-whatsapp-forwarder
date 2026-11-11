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

    init {
        checkBatteryOptimization()
    }

    fun checkBatteryOptimization() {
        val powerManager = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnored = powerManager?.isIgnoringBatteryOptimizations(getApplication<Application>().packageName) ?: true
        _isBatteryOptimizationIgnored.value = isIgnored
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
        phoneId: String,
        accessToken: String,
        recipient: String,
        webhookUrl: String,
        customHeader: String,
        autoRetry: Boolean
    ) {
        preferences.apiMode = apiMode
        preferences.cloudPhoneNumberId = phoneId
        preferences.cloudAccessToken = accessToken
        preferences.recipientWhatsAppNumber = recipient
        preferences.customWebhookUrl = webhookUrl
        preferences.customHeader = customHeader
        preferences.isAutoRetryEnabled = autoRetry
    }
}
