package com.bryanlau.bluetoothscanner.bluetoothhelper

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat.getSystemService
import android.Manifest.permission.*
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat.checkSelfPermission
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat.requestPermissions

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
     * Get list of missing permissions required for BLuetooth
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
     */
    fun hasSufficientPermissions() : Boolean {
        if (getMissingPermissions().isEmpty()) return true
        return false
    }

    /**
     * Initialize the class
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
     */
    fun processFoundDevice(device: BluetoothDevice, onDeviceFound: (BluetoothDeviceInfo) -> Unit) {

        val deviceInfo = BluetoothDeviceInfo(device.name, device.address)
        onDeviceFound(deviceInfo)
    }

    /**
     * Scan for devices
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
                            context?.unregisterReceiver(_scanReceiver)
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
        if (checkSelfPermission(context, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            _btAdapter.startDiscovery()
        }
        else {
            Log.e("BluetoothHelper", "Missing ACCESS_FINE_LOCATION permission")
        }
    }

    /**
     * Finalize and clean up scan
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
}