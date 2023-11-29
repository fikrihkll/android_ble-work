package com.dagger.ble_background

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.BeaconTransmitter
import org.altbeacon.beacon.Region


class BluetoothBeacon {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var successCallback: ((message: String) -> Unit)? = null
    private var errorCallback: ((message: String) -> Unit)? = null
    private lateinit var beaconTransmitter: BeaconTransmitter
    var isStarted = false
    private lateinit var context: Context

    fun init(activity: Context) {
        context = activity
        val iBeaconLayout = BeaconParser()
            .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
        beaconTransmitter = BeaconTransmitter(activity, iBeaconLayout)
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_ADVERTISE")
    fun stopBroadcast() {
        Log.w("FOREGROUN-BLE", "STOPPEDstart")
        beaconTransmitter.stopAdvertising()
        Log.w("FOREGROUN-BLE", "STOPPEDend")
    }
    private fun beaconFromMap(map: Map<*, *>): Beacon? {
        val builder = Beacon.Builder()
        val proximityUUID = map["proximityUUID"]
        if (proximityUUID is String) {
            builder.setId1(proximityUUID as String?)
        }
        val major = map["major"]
        if (major is Int) {
            builder.setId2(major.toString())
        }
        val minor = map["minor"]
        if (minor is Int) {
            builder.setId3(minor.toString())
        }
        val txPower = map["txPower"]
        if (txPower is Int) {
            builder.setTxPower((txPower as Int?)!!)
        } else {
            builder.setTxPower(-59)
        }
        builder.setDataFields(listOf(0L))
        builder.setManufacturer(0x004c)
        return builder.build()
    }

    fun startMonitoring(owner: LifecycleOwner, observer: Observer<Collection<Beacon>>) {
        val iBeaconLayout = BeaconParser()
            .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")

        val beaconManager =  BeaconManager.getInstanceForApplication(context)
        val region = Region("all-beacons-region", null, null, null)

        beaconManager.beaconParsers.add(iBeaconLayout)
        beaconManager.getRegionViewModel(region).rangedBeacons.observe(owner, observer)
        beaconManager.startRangingBeacons(region)
    }

    fun startBackgroundMonitoring(observer: Observer<Collection<Beacon>>) {
        val iBeaconLayout = BeaconParser()
            .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")

        val beaconManager =  BeaconManager.getInstanceForApplication(context)
        val region = Region("all-beacons-region", null, null, null)

        beaconManager.beaconParsers.add(iBeaconLayout)
        beaconManager.getRegionViewModel(region).rangedBeacons.observeForever(observer)
        beaconManager.startRangingBeacons(region)
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_ADVERTISE")
    fun startBroadcast2(uuid: String, major: Int, minor: Int, successCallback: (message: String) -> Unit, errorCallback: (message: String) -> Unit) {
        this.successCallback = successCallback
        this.errorCallback = errorCallback

        val map = mapOf<String, Any>(
            "proximityUUID" to uuid,
            "major" to major,
            "minor" to minor,
            "txPower" to -59
        )
        val beacon: Beacon? = beaconFromMap(map)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val advertisingMode = map["advertisingMode"]
            if (advertisingMode is Int) {
                (advertisingMode as Int?)?.let { beaconTransmitter.advertiseMode = it }
            }
            val advertisingTxPowerLevel = map["advertisingTxPowerLevel"]
            if (advertisingTxPowerLevel is Int) {
                (advertisingTxPowerLevel as Int?)?.let {
                    beaconTransmitter.advertiseTxPowerLevel = it
                }
            }
            beaconTransmitter.startAdvertising(beacon, object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                    Log.d(
                        "BEACON-BROADCAST",
                        "Start broadcasting = $beacon"
                    )
                    isStarted = true
                    Log.w("BLE-BROADCASTER", "app -- INFO -- connectable: ${settingsInEffect.isConnectable}; txPowerLevelLow: ${settingsInEffect?.txPowerLevel == AdvertiseSettings.ADVERTISE_TX_POWER_HIGH};")
                    successCallback.invoke("Start broadcasting = $beacon")
                }

                override fun onStartFailure(errorCode: Int) {
                    var error = "FEATURE_UNSUPPORTED"
                    if (errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE) {
                        error = "DATA_TOO_LARGE"
                    } else if (errorCode == ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
                        error = "TOO_MANY_ADVERTISERS"
                    } else if (errorCode == ADVERTISE_FAILED_ALREADY_STARTED) {
                        error = "ALREADY_STARTED"
                    } else if (errorCode == ADVERTISE_FAILED_INTERNAL_ERROR) {
                        error = "INTERNAL_ERROR"
                    }
                    Log.e("BEACON-BROADCAST", error)
                }
            })
        } else {
            Log.e("BEACON-BROADCAST", "NOT SUPPORTED")
        }
    }

}
