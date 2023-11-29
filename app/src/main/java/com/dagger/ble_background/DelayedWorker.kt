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
import android.os.CountDownTimer
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.altbeacon.beacon.Beacon
import kotlin.random.Random

class DelayedWorker(private val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private lateinit var notificationManager: NotificationManager

    override suspend fun doWork(): Result {
        notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Random(10000).nextInt(), createNotificationObject(context = applicationContext, "BLECHANNEL","BLE","BLE WILL BE STARTED", "Nice nice"))
        Log.w("FOREGROUN-BLE", "WORKER STARTED")
        startBroadcasting()
        Log.w("FOREGROUN-BLE", "WORKER STOPedD")
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            1, createNotificationObject(context = applicationContext, "BLECHANNEL","BLE","BLE is spreading out", "Nice nice")
        )
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
        Log.w("FOREGROUN-BLE", "PREPARING BLE")
        val beacon = BluetoothBeacon()
        beacon.init(context)
        Log.w("FOREGROUN-BLE", "PREPARING BLE 2")

        CoroutineScope(Dispatchers.Main).launch {
            beacon.startBackgroundMonitoring(rangingObserver)
        }
        Log.w("FOREGROUN-BLE", "STARTED SUCCESSFULLY")

        delay(60000 * 2)
        notificationManager.notify(Random(10000).nextInt(), createNotificationObject(context = applicationContext, "BLECHANNEL","BLE","BLE HAS BEEN STPOPPED", "Nice nice"))
    }

    private fun pushNotif(title: String, message: String) {
        val notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Random(10000).nextInt(), createNotificationObject(context = applicationContext, "BLECHANNEL","BLE",title, message))
    }

    private fun log() {
        object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.w("FOREGROUN-BLE", "Spreading... ${millisUntilFinished/1000}")
            }

            override fun onFinish() {
                Log.w("FOREGROUN-BLE", "FINISHED")
            }

        }.start()
    }

    val rangingObserver = Observer<Collection<Beacon>> { beacons ->
        Log.d(TAG, "Ranged: ${beacons.count()} beacons")
        for (beacon: Beacon in beacons) {
            Log.d(TAG, "$beacon about ${beacon.distance} meters away")
            Log.d(TAG, "details: name ${beacon.bluetoothName} | id ${beacon.id1} ")
            pushNotif("Detected", "ayeeee...")
        }
    }

}