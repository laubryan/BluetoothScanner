package com.bryanlau.bluetoothscanner.bluetoothhelper

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat.getSystemService
import android.Manifest.permission.*
import android.app.Activity
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat.checkSelfPermission
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat

/**
 *  BluetoothHelper
 *
 *  This class scans for Bluetooth devices and returns some basic information
 *  for each device found.
 *
 *  @property context the Android context
 *
 *  Provide the Android context at instantiation, and call initialize() before
 *  calling scanForDevices() to start the scan. Found devices are returned using
 *  the onDeviceFound callback parameter.
 *
 *  For SDK 30 and lower, the following permissions are required:
 *
 *    BLUETOOTH
 *    BLUETOOTH_ADMIN
 *    ACCESS_COARSE_LOCATION
 *
 *    In particular, the location permission needs to be interactively confirmed
 *    by the user.
 *
 *  For SDK 31 and higher, the following permissions are required:
 *
 *    BLUETOOTH_SCAN
 *
 *
 */
class BluetoothHelper(private val context: Context) {

    // Interfaces
    private lateinit var _btManager: BluetoothManager
    private lateinit var _btAdapter: BluetoothAdapter
    private var _initialized = false

    // Broadcast
    var _scanReceiver: BroadcastReceiver? = null

    init {

    }

    /**
     * Translate Bluetooth device description
     *
     * @param bluetoothClass the Bluetooth device class found during discovery
     */
    private fun getBluetoothDeviceDescription(bluetoothClass: BluetoothClass): String {
        return when (bluetoothClass.majorDeviceClass) {
            BluetoothClass.Device.Major.PHONE -> "Phone"
            BluetoothClass.Device.Major.COMPUTER -> "Computer"
            BluetoothClass.Device.Major.AUDIO_VIDEO -> "Audio/Video"
            BluetoothClass.Device.Major.PERIPHERAL -> "Peripheral"
            BluetoothClass.Device.Major.IMAGING -> "Imaging Device"
            BluetoothClass.Device.Major.HEALTH -> "Health Device"
            else -> "Unknown Type"
        }
    }

    /**
     * Get list of missing permissions required for BLuetooth
     *
     * @return list of required permissions that the app is missing
     */
    private fun getMissingPermissions() : List<String> {

        // Define required permissions
        val requiredPermissions = mutableListOf<String>()

        // API level 30 and below
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            requiredPermissions.addAll(arrayOf(BLUETOOTH, BLUETOOTH_ADMIN))
        }
        else {
            requiredPermissions.addAll(arrayOf( BLUETOOTH, BLUETOOTH_ADMIN, BLUETOOTH_SCAN))
        }

        // Determine which permissions are missing
        val missingPermissions = requiredPermissions.filter { permission ->
            checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }

        return missingPermissions
    }

    /**
     * Does app have sufficient permission for Bluetooth operations
     *
     * @return true if the app has sufficient permissions to execute, false otherwise
     */
    fun hasSufficientPermissions() : Boolean {
        if (getMissingPermissions().isEmpty()) return true
        return false
    }

    /**
     * Initialize the class
     *
     * @return true if initialization was successful, false otherwise
     */
    fun initialize() : Boolean {
        try {
            // Initialize Bluetooth Manager
            _btManager = getSystemService(context, BluetoothManager::class.java)!!

            // Retrieve default adapter
            _btAdapter = _btManager.adapter

            // Check whether Bluetooth is enabled
            if (!_btAdapter.isEnabled) {
                Log.e("BluetoothHelper", "Bluetooth is not enabled")
                return false
            }

            // Set initialization state
            _initialized = true
            return true
        }
        catch(e: Exception) {
            return false
        }
    }

    /**
     * Process found device and pass it on
     *
     * @param device the Bluetooth device found during discovery
     * @param onDeviceFound user callback to invoke after device is parsed
     */
    fun processFoundDevice(device: BluetoothDevice, onDeviceFound: (BluetoothDeviceInfo) -> Unit) {

        val deviceName = device.name ?: "UNKNOWN"
        val deviceClass = getBluetoothDeviceDescription(device.bluetoothClass)
        val deviceInfo = BluetoothDeviceInfo(
            deviceName,
            device.address,
            deviceClass
        )
        onDeviceFound(deviceInfo)
    }

    /**
     * Interactively request permissions for specific cases
     *
     * @param activity the Android activity to use for the interactive permission request
     */
    fun requestPermissions(activity: Activity) {

        val requestCode = 9999

        // Android 11/R/SDK 30 or less require ACCESS_COARSE_LOCATION
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            val permissions = arrayOf(ACCESS_COARSE_LOCATION)
            ActivityCompat.requestPermissions(activity, permissions, requestCode)
        }
    }

    /**
     * Finalize and clean up scan
     *
     * @param onScanComplete the user callback provided from scanForDevices()
     */
    private fun scanFinalize(onScanComplete: () -> Unit) {

        // Unregister the receiver
        _scanReceiver?.let {
            context.unregisterReceiver(it)
            _scanReceiver = null
        }

        // Call user completion function
        onScanComplete()
    }

    /**
     * Scan for devices
     *
     * @param onDeviceFound user callback to invoke when a device is found
     * @param onScanComplete user callback to invoke when the scan is finished
     */
    fun scanForDevices(onDeviceFound: (BluetoothDeviceInfo) -> Unit, onScanComplete: () -> Unit) {

        // Initialize receiver if required
        if (_scanReceiver == null) {

            // Initialize BroadcastReceiver
            _scanReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {

                        // Device found
                        BluetoothDevice.ACTION_FOUND -> {
                            Log.i("BluetoothHelper", "ACTION_FOUND")
                            val device: BluetoothDevice? =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    intent.getParcelableExtra(
                                        BluetoothDevice.EXTRA_DEVICE,
                                        BluetoothDevice::class.java
                                    )
                                }
                                else {
                                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                                }
                            device?.let { processFoundDevice(it, onDeviceFound) }
                        }

                        // Scan started
                        BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                            Log.i("BluetoothHelper", "ACTION_DISCOVERY_STARTED")
                        }

                        // Scan complete
                        BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                            Log.i("BluetoothHelper", "ACTION_DISCOVERY_FINISHED")
                            scanFinalize(onScanComplete)
                        }
                    }
                }

            }
        }

        // Register the receiver
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(_scanReceiver, filter)

        // Start discovery
        if (
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.R &&
            checkSelfPermission(context, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BluetoothHelper", "Missing ACCESS_COARSE_LOCATION permission for Android R and lower")
        }
        else {
            _btAdapter.startDiscovery()
        }
    }

}