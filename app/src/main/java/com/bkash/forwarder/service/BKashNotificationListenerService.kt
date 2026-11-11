package com.bkash.forwarder.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.bkash.forwarder.data.pref.AppPreferences
import com.bkash.forwarder.data.repository.ForwarderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BKashNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "BKashNotificationListener"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val prefs = AppPreferences.getInstance(applicationContext)
        if (!prefs.isForwardingEnabled) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim() ?: ""

        val fullBody = when {
            bigText.isNotBlank() -> bigText
            text.isNotBlank() -> text
            else -> return
        }

        val pkgName = sbn.packageName.lowercase()

        // Filter: Must be from bKash app OR any SMS messaging app OR contain bKash / 16247
        val isBkashApp = pkgName.contains("bkash")
        val isSmsApp = pkgName.contains("messaging") ||
                pkgName.contains("mms") ||
                pkgName.contains("sms") ||
                pkgName.contains("google.android.apps.messaging") ||
                pkgName.contains("samsung.android.messaging")
        val isBkashContent = title.contains("bkash", ignoreCase = true) ||
                title.contains("16247") ||
                fullBody.contains("bkash", ignoreCase = true) ||
                fullBody.contains("16247") ||
                fullBody.contains("TrxID", ignoreCase = true)

        if (!((isBkashApp || isSmsApp) && isBkashContent)) {
            return
        }

        Log.i(TAG, "Notification captured from $pkgName | Title: '$title' | Body length: ${fullBody.length}")

        val sender = if (title.isNotBlank()) title else "bKash"
        val repository = ForwarderRepository.getInstance(applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.processIncomingSms(sender, fullBody)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling captured notification: ${e.message}", e)
            }
        }
    }
}
