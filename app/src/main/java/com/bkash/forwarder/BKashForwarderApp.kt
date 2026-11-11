package com.bkash.forwarder

import android.app.Application
import android.util.Log
import com.bkash.forwarder.data.pref.AppPreferences
import com.bkash.forwarder.service.SmsForwarderService

class BKashForwarderApp : Application() {

    companion object {
        private const val TAG = "BKashForwarderApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "bKash Forwarder Application starting up...")

        val prefs = AppPreferences.getInstance(this)
        if (prefs.isForwardingEnabled) {
            SmsForwarderService.startService(this)
        }
    }
}
