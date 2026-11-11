package com.bkash.forwarder.data.repository

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bkash.forwarder.data.local.ForwardStatus
import com.bkash.forwarder.data.local.TransactionDatabaseHelper
import com.bkash.forwarder.data.local.TransactionRecord
import com.bkash.forwarder.data.pref.AppPreferences
import com.bkash.forwarder.network.SendResult
import com.bkash.forwarder.network.WhatsAppApiService
import com.bkash.forwarder.parser.BKashSmsParser
import com.bkash.forwarder.parser.ParseResult
import com.bkash.forwarder.worker.ForwardRetryWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ForwarderRepository(
    private val context: Context,
    private val dbHelper: TransactionDatabaseHelper = TransactionDatabaseHelper.getInstance(context),
    val preferences: AppPreferences = AppPreferences.getInstance(context),
    private val apiService: WhatsAppApiService = WhatsAppApiService(preferences)
) {

    companion object {
        private const val TAG = "ForwarderRepository"
        const val RETRY_WORK_NAME = "bkash_sms_forward_retry_work"

        @Volatile
        private var instance: ForwarderRepository? = null

        fun getInstance(context: Context): ForwarderRepository {
            return instance ?: synchronized(this) {
                instance ?: ForwarderRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val _recordsFlow = MutableStateFlow<List<TransactionRecord>>(emptyList())
    val recordsFlow: StateFlow<List<TransactionRecord>> = _recordsFlow.asStateFlow()

    private val _statsFlow = MutableStateFlow(Triple(0, 0, 0)) // (total, sent, failed/pending)
    val statsFlow: StateFlow<Triple<Int, Int, Int>> = _statsFlow.asStateFlow()

    private val repoScope = CoroutineScope(Dispatchers.IO)

    init {
        refreshData()
    }

    fun refreshData() {
        repoScope.launch {
            val records = dbHelper.getAllRecords(limit = 100)
            val stats = dbHelper.getCounts()
            _recordsFlow.value = records
            _statsFlow.value = stats
        }
    }

    /**
     * Processes incoming SMS directly from BroadcastReceiver or Service.
     * Returns true if a valid bKash transaction was processed; false if ignored or blocked.
     */
    suspend fun processIncomingSms(sender: String?, messageBody: String?): Boolean = withContext(Dispatchers.IO) {
        if (!preferences.isForwardingEnabled) {
            Log.d(TAG, "Forwarding switch is OFF. Ignoring incoming message.")
            return@withContext false
        }

        when (val parseResult = BKashSmsParser.parse(sender, messageBody)) {
            is ParseResult.NonBkashSender -> {
                // Non-bKash personal or commercial message: ignore completely!
                return@withContext false
            }
            is ParseResult.SecurityBlocked -> {
                Log.w(TAG, "SECURITY ALERT: Dropped bKash message because: ${parseResult.reason}")
                return@withContext false
            }
            is ParseResult.NotATransaction -> {
                Log.d(TAG, "Non-transaction bKash message ignored: ${parseResult.reason}")
                return@withContext false
            }
            is ParseResult.Success -> {
                val tx = parseResult.transaction
                Log.i(TAG, "Detected bKash Transaction: TrxID=${tx.trxId}, Type=${tx.type}, Amount=${tx.amount}")

                // Duplicate prevention check
                if (dbHelper.isTrxDuplicate(tx.trxId)) {
                    Log.w(TAG, "Duplicate Transaction ID detected (${tx.trxId}). Skipping forward.")
                    return@withContext false
                }

                // Forwarding format: RAW (Exact Copy-Paste) vs FORMATTED (Receipt Card)
                val messageToSend = if (preferences.forwardMode == "RAW") {
                    tx.rawSms
                } else {
                    tx.toWhatsAppMessage(preferences.customHeader)
                }

                val newRecord = TransactionRecord(
                    trxId = tx.trxId,
                    type = tx.type,
                    amount = tx.amount,
                    counterpart = tx.counterpart,
                    balance = tx.balance,
                    fee = tx.fee,
                    reference = tx.reference,
                    rawSms = tx.rawSms,
                    forwardedText = messageToSend,
                    status = ForwardStatus.PENDING,
                    retryCount = 0,
                    errorMessage = null,
                    timestamp = tx.timestamp,
                    updatedAt = System.currentTimeMillis()
                )

                // Save initial record to local database
                dbHelper.insertTransaction(newRecord)
                refreshData()

                // Dispatch to WhatsApp Group or Number
                val sendResult = apiService.sendMessage(
                    messageText = messageToSend,
                    trxId = tx.trxId
                )

                when (sendResult) {
                    is SendResult.Success -> {
                        Log.i(TAG, "Successfully forwarded TrxID=${tx.trxId} to WhatsApp!")
                        dbHelper.updateStatus(tx.trxId, ForwardStatus.SENT, null)
                    }
                    is SendResult.Failure -> {
                        Log.e(TAG, "Failed to forward TrxID=${tx.trxId}: ${sendResult.errorMessage}")
                        dbHelper.updateStatus(
                            tx.trxId,
                            ForwardStatus.FAILED,
                            sendResult.errorMessage,
                            incrementRetry = true
                        )
                        // If auto-retry enabled, schedule WorkManager retry with network constraint
                        if (preferences.isAutoRetryEnabled) {
                            scheduleRetryWorker()
                        }
                    }
                }

                refreshData()
                return@withContext true
            }
        }
    }

    /**
     * Manually retries sending a specific failed or pending transaction.
     */
    suspend fun retryTransaction(trxId: String): SendResult = withContext(Dispatchers.IO) {
        val record = dbHelper.getRecordByTrxId(trxId)
            ?: return@withContext SendResult.Failure("Record not found for TrxID: $trxId")

        dbHelper.updateStatus(trxId, ForwardStatus.PENDING, "Retrying...")
        refreshData()

        val result = apiService.sendMessage(
            messageText = record.forwardedText,
            trxId = record.trxId
        )

        when (result) {
            is SendResult.Success -> {
                dbHelper.updateStatus(trxId, ForwardStatus.SENT, null)
            }
            is SendResult.Failure -> {
                dbHelper.updateStatus(
                    trxId,
                    ForwardStatus.FAILED,
                    result.errorMessage,
                    incrementRetry = true
                )
            }
        }
        refreshData()
        return@withContext result
    }

    /**
     * Retries all pending and failed transactions.
     */
    suspend fun retryAllPendingAndFailed(): Int = withContext(Dispatchers.IO) {
        val pendingList = dbHelper.getPendingAndFailedRecords()
        var successCount = 0
        for (record in pendingList) {
            val result = apiService.sendMessage(
                messageText = record.forwardedText,
                trxId = record.trxId
            )
            if (result is SendResult.Success) {
                dbHelper.updateStatus(record.trxId, ForwardStatus.SENT, null)
                successCount++
            } else if (result is SendResult.Failure) {
                dbHelper.updateStatus(
                    record.trxId,
                    ForwardStatus.FAILED,
                    result.errorMessage,
                    incrementRetry = true
                )
            }
        }
        refreshData()
        return@withContext successCount
    }

    /**
     * Sends a test message to the configured WhatsApp group or number to verify API credentials.
     */
    suspend fun sendTestMessage(): SendResult = withContext(Dispatchers.IO) {
        val targetLabel = if (preferences.recipientType == "GROUP") "WhatsApp Group" else "WhatsApp Chat"
        val modeLabel = if (preferences.forwardMode == "RAW") "Exact Copy-Paste (Raw SMS)" else "Formatted Receipt Card"

        val testBody = if (preferences.forwardMode == "RAW") {
            "[bKash Forwarder Test] Connection verified! Incoming bKash messages will be copy-pasted directly into this $targetLabel."
        } else {
            buildString {
                appendLine("✅ *bKash SMS Forwarder - Test Connection*")
                appendLine("━━━━━━━━━━━━━━━━━━━")
                appendLine("Destination: $targetLabel (${preferences.getFormattedRecipient()})")
                appendLine("Forward Mode: $modeLabel")
                appendLine("Your WhatsApp connection is active and working properly!")
                appendLine("Incoming bKash transactions will be forwarded here automatically.")
                appendLine("━━━━━━━━━━━━━━━━━━━")
                append("_⚡ Developed by S. M. Mahmud Iqbal_")
            }
        }
        apiService.sendMessage(messageText = testBody, trxId = "TEST-CONN")
    }

    fun clearAllHistory() {
        repoScope.launch {
            dbHelper.clearAll()
            refreshData()
        }
    }

    /**
     * Schedules a background retry using Android WorkManager with Network constraint.
     */
    fun scheduleRetryWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val retryRequest = OneTimeWorkRequestBuilder<ForwardRetryWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            RETRY_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            retryRequest
        )
    }
}
