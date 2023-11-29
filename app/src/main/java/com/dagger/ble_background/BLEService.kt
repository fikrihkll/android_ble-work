package com.dagger.ble_background

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random


class BLEService : Service() {



    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        Log.w("FOREGROUN-BLE", "FOREGROUND COME UP")
        Log.w("FOREGROUN-BLE", "BLE WILL BE INIT")
        GlobalScope.launch {
            startBroadcasting()
        }

        startForeground(Random(10000).nextInt(), createNotificationObject(context = applicationContext, "BLECHANNEL","BLE","BLE is spreading out", "Nice nice"))
    }

    private fun createNotificationObject(
        context: Context,
        notificationChannelId: String,
        notificationChannelName: String,
        title: String,
        body: String,
        pendingIntent: PendingIntent? = null
    ): Notification {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        notificationManager?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(
                    notificationChannelId,
                    notificationChannelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                )

                notificationChannel.description = context.getString(R.string.app_name)
                notificationChannel.enableLights(true)
                notificationChannel.lightColor =
                    ContextCompat.getColor(context, R.color.black)
                notificationChannel.vibrationPattern = longArrayOf(0, 0, 0, 0)
                notificationChannel.enableVibration(true)
                notificationChannel.importance = NotificationManager.IMPORTANCE_HIGH
                notificationManager.createNotificationChannel(notificationChannel)
            }
        }

        val notification = NotificationCompat.Builder(context, notificationChannelId)
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0))
            .setPriority(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) NotificationManager.IMPORTANCE_HIGH else Notification.PRIORITY_HIGH)
            .setDefaults(
                Notification.DEFAULT_SOUND or Notification.DEFAULT_LIGHTS
            )
            .setChannelId(notificationChannelId)
            .build()

        pendingIntent?.let {
            notification.contentIntent = it
        }

        return notification
    }

    private suspend fun startBroadcasting() {
        delay(60000)
        val beacon = BluetoothBeacon()
        beacon.init(applicationContext)
        Log.w("FOREGROUN-BLE", "PREPARING BLE 2")
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("FOREGROUN-BLE", "NOT GRANTED")
            return
        }
        Log.w("FOREGROUN-BLE", "WILL BE STARTED")
        beacon.startBroadcast2(
            "03b6b09c-07e1-4731-a99e-379ef03f789a",
            41564,
            24860,
            successCallback = {
                Log.w("FOREGROUN-BLE", "SUCCESS started spreading moneyyy....")
            },
            errorCallback = { message ->
                Log.w("FOREGROUN-BLE", "ERROR $message")
            }
        )
        Log.w("FOREGROUN-BLE", "STARTED SUCCESSFULLY")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}