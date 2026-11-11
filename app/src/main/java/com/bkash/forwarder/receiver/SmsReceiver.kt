package com.bkash.forwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Telephony
import android.util.Log
import com.bkash.forwarder.data.repository.ForwarderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
        private const val WAKE_LOCK_TAG = "bKashForwarder:SmsProcessingWakeLock"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION &&
            action != "android.provider.Telephony.SMS_RECEIVED"
        ) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            return
        }

        val sender = messages.first().displayOriginatingAddress ?: ""
        val bodyBuilder = StringBuilder()
        for (sms in messages) {
            bodyBuilder.append(sms.displayMessageBody ?: "")
        }
        val fullMessage = bodyBuilder.toString()

        Log.d(TAG, "Incoming SMS received from: $sender")

        // Use goAsync to process safely in coroutine without blocking broadcast thread
        val pendingResult = goAsync()

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        )?.apply {
            acquire(25_000) // 25 seconds safety timeout
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = ForwarderRepository.getInstance(context)
                repository.processIncomingSms(sender, fullMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling incoming SMS: ${e.message}", e)
            } finally {
                try {
                    if (wakeLock != null && wakeLock.isHeld) {
                        wakeLock.release()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error releasing wake lock: ${e.message}")
                }
                pendingResult.finish()
            }
        }
    }
}
