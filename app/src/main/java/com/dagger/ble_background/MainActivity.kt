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
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.dagger.ble_background.databinding.ActivityMainBinding
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import org.altbeacon.beacon.Beacon
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

const val TAG = "spn.bug-hunter"

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityMainBinding
    private val beacon = BluetoothBeacon()
    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    @RequiresApi(Build.VERSION_CODES.S)
    private val REQUIRED_PERMISSIONS_31 = mutableListOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val REQUIRED_PERMISSIONS_33 = mutableListOf(
        Manifest.permission.POST_NOTIFICATIONS
    )

    val rangingObserver = Observer<Collection<Beacon>> { beacons ->
        Log.d(TAG, "Ranged: ${beacons.count()} beacons")
        for (beacon: Beacon in beacons) {
            Log.d(TAG, "$beacon about ${beacon.distance} meters away")
            Log.d(TAG, "details: name ${beacon.bluetoothName} | id ${beacon.id1} ")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerOnClick()
        beacon.init(this)
        if (!arePermissionsGranted()) {
            requestPermission()
        }
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.startBroadcastButton -> {
                startWorker(this)
            }
        }
    }

    private fun startWorker(context: Context) {
        Log.w("GEOF-FK", "WORKER WILL BE TRIGGERED")
        val delayedWorker = OneTimeWorkRequestBuilder<DelayedWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .addTag("BLE_WORKER")
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            "ble",
            ExistingWorkPolicy.REPLACE,
            delayedWorker
        )
    }

    private fun startMonitoring() {
        beacon.startMonitoring(this, rangingObserver)
    }

    private fun getPerimssionList(): Array<String> {
        val permissionList = mutableListOf<String>()
        permissionList.addAll(REQUIRED_PERMISSIONS)
        if (Build.VERSION.SDK_INT >= 31) {
            permissionList.addAll(REQUIRED_PERMISSIONS_31)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            permissionList.addAll(REQUIRED_PERMISSIONS_33)
        }
        return permissionList.toTypedArray()
    }

    private fun pushNotif(title: String, message: String) {
        val notificationManager = getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Random(10000).nextInt(), createNotificationObject(context = applicationContext, "BLECHANNEL","BLE",title, message))
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

    private fun requestPermission(){
        val requestMultiplePermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (!arePermissionsGranted())
                Toast.makeText(
                    this,
                    "Izin bluetooth dibutuhkan",
                    Toast.LENGTH_SHORT
                ).show()

        }

        requestMultiplePermissions.launch(getPerimssionList())
    }

    private fun arePermissionsGranted(): Boolean {
        return getPerimssionList().all {
            ContextCompat.checkSelfPermission(
                this, it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun executeBeacon() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermission()
            return
        }
        if (beacon.isStarted) {
            beacon.stopBroadcast()
        } else {
            startBeacon()
        }
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_ADVERTISE")
    private fun startBeacon() {
        beacon.startBroadcast2(
            "03b6b09c-07e1-4731-a99e-379ef03f789a",
            41564,
            24860,
            successCallback = {
                              binding.statusBleTextView.text = it
            },
            errorCallback = { message ->
                binding.statusBleTextView.text = "Error ${message}"
            }
        )
    }

    private fun registerOnClick() {
        binding.startBroadcastButton.setOnClickListener(this)
    }
}