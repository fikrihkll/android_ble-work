package com.dagger.ble_background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlin.random.Random

class GeofenceEventBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent!!)
        if (geofencingEvent?.hasError() == true) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e("GEOFENCE-BLE", errorMessage)
            return
        }

        Log.e("GEOFENCE-BLE", "amunt ${(geofencingEvent?.triggeringGeofences?.size) ?: -1}")
        val geofenceTransition = geofencingEvent?.geofenceTransition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            Log.e("GEOFENCE-BLE", "amunt ${(triggeringGeofences?.size) ?: -1}")

//            notificationManager.sendGeofenceEnteredNotification(context)
        } else {
            Log.e("GEOFENCE-BLE", "Invalid type transition $geofenceTransition")
        }
        Log.w("GEOF-FK", "EVENT TRIGGERED")
        context?.let {
            Log.w("GEOF-FK", "CONTEXT NOT NULL and intent ${intent}")
            Log.w("GEOF-FK", "INTENT bool -> ${intent == null}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Log.w("GEOF-FK", "INTENT data -> ${intent}")
            }
            startWorker(it)
        }
    }

    private fun startWorker(context: Context) {
        Log.w("GEOF-FK", "WORKER WILL BE TRIGGERED")
        val delayedWorker = OneTimeWorkRequestBuilder<DelayedWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("BLE_WORKER")
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            "ble",
            ExistingWorkPolicy.REPLACE,
            delayedWorker
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
}