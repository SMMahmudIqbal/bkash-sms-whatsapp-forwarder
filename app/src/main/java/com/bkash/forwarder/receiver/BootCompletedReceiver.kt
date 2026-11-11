package com.bkash.forwarder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.bkash.forwarder.data.pref.AppPreferences
import com.bkash.forwarder.service.SmsForwarderService

class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Device boot/restart action received: $action")

        val preferences = AppPreferences.getInstance(context)
        if (preferences.isForwardingEnabled) {
            Log.i(TAG, "Forwarding is enabled. Automatically starting SmsForwarderService...")
            SmsForwarderService.startService(context)
        } else {
            Log.i(TAG, "Forwarding is disabled in settings. Skipping service auto-start.")
        }
    }
}
