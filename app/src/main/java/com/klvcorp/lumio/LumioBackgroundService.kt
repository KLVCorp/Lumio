package com.klvcorp.lumio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import java.util.Locale

/** User-enabled connected-device foreground service. No promise of survival after force-stop/reboot. */
class LumioBackgroundService : Service() {
    companion object {
        const val ACTION_STOP = "com.klvcorp.lumio.STOP_BACKGROUND"
        private const val CHANNEL_ID = "lumio_connected_device"
        private const val NOTIFICATION_ID = 42
    }
    private val handler = Handler(Looper.getMainLooper())
    private val reconnect = object : Runnable {
        override fun run() {
            val controller = LumioRuntime.get(applicationContext)
            if (controller.backgroundEnabled && controller.devices.autoConnect) controller.reconnectIfEnabled()
            handler.postDelayed(this, 30_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Lumio automation", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            LumioRuntime.get(applicationContext).setBackgroundEnabled(false)
            stopSelf()
            return START_NOT_STICKY
        }
        val controller = LumioRuntime.get(applicationContext)
        if (!controller.backgroundEnabled || !controller.ble.hasPermissions()) {
            stopSelf(); return START_NOT_STICKY
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, LumioBackgroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val chosen = getSharedPreferences("lumio_preferences", MODE_PRIVATE).getString("language", "system") ?: "system"
        val russian = if (chosen == "system") Locale.getDefault().language == "ru" else chosen == "ru"
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Lumio")
            .setContentText(if (russian) "Bluetooth и автоматизация работают" else "Bluetooth and automations are active")
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, if (russian) "Остановить" else "Stop", stop).build())
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            } else startForeground(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
            stopSelf(); return START_NOT_STICKY
        }
        handler.removeCallbacks(reconnect)
        handler.post(reconnect)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(reconnect)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
