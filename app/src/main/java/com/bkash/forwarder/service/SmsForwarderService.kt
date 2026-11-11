package com.bkash.forwarder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bkash.forwarder.data.pref.AppPreferences
import com.bkash.forwarder.receiver.SmsReceiver
import com.bkash.forwarder.ui.MainActivity

class SmsForwarderService : Service() {

    companion object {
        private const val TAG = "SmsForwarderService"
        const val CHANNEL_ID = "bkash_forwarder_persistent_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.bkash.forwarder.ACTION_START"
        const val ACTION_STOP = "com.bkash.forwarder.ACTION_STOP"

        fun startService(context: Context) {
            val intent = Intent(context, SmsForwarderService::class.java).apply {
                action = ACTION_START
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service: ${e.message}", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SmsForwarderService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var dynamicSmsReceiver: SmsReceiver? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Creating SmsForwarderService...")
        createNotificationChannel()

        // Register dynamic SMS receiver as backup listener
        try {
            dynamicSmsReceiver = SmsReceiver()
            val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION).apply {
                priority = 999
            }
            ContextCompat.registerReceiver(
                this,
                dynamicSmsReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            Log.d(TAG, "Dynamic SMS receiver registered.")
        } catch (e: Exception) {
            Log.w(TAG, "Could not register dynamic SMS receiver: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Log.i(TAG, "Received ACTION_STOP. Stopping foreground service...")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildPersistentNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        Log.i(TAG, "SmsForwarderService started in foreground.")
        return START_STICKY
    }

    private fun buildPersistentNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prefs = AppPreferences.getInstance(this)
        val targetNumber = prefs.recipientWhatsAppNumber.ifBlank { "Not configured" }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("bKash Auto Forwarder Active")
            .setContentText("Listening for bKash SMS -> WhatsApp ($targetNumber)")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "bKash Forwarder Service"
            val descriptionText = "Keeps bKash SMS Forwarder active in the background"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Destroying SmsForwarderService...")
        try {
            if (dynamicSmsReceiver != null) {
                unregisterReceiver(dynamicSmsReceiver)
                dynamicSmsReceiver = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering dynamic receiver: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
