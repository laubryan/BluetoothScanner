package com.bryanlau.bluetoothscanner

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.registerReceiver
import com.bryanlau.bluetoothscanner.ui.theme.BluetoothScannerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BluetoothScannerTheme {
                MainPage()
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 640)
@Composable
fun MainPagePreview() {
    BluetoothScannerTheme {
        MainPage(modifier = Modifier)
    }
}

@Composable
fun MainPage(modifier: Modifier = Modifier) {

    var isScanning by remember { mutableStateOf(false) }
    var items: List<BluetoothDevice> = emptyList()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            BottomAppBar() {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = {
                        // Idle
                        if(!isScanning) {
                            // Set scanning flag
                            isScanning = true

                            // Get device list
                            coroutineScope.launch(Dispatchers.IO) {
                                items = getDeviceList(context)
                                isScanning = false
                            }
                        }
                        else {
                            // Scanning
                            isScanning = false
                        }
                    }) {
                        Text(text = if (isScanning) "Stop Scanning" else "Scan Now")
                    }
                }
            }
        }
    ) {
        padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Display items
            if(!isScanning) {
                for (device in items) {
                    val deviceName = getDeviceName(context, device)
                    val deviceHwAddress = device.address

                    DeviceEntry(deviceName, deviceHwAddress)
                }
            }
        }
    }
}

/**
 * Check for Bluetooth permissions
 */
fun checkPermissions(context: Context): List<String> {

    // Define required permissions
    val requiredPermissions = mutableListOf<String>()

    // API level 30 and below
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
        requiredPermissions.addAll(arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,))
    }
    else {
        requiredPermissions.addAll(arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_SCAN,))
    }

    // Determine which permissions are missing
    val missingPermissions = requiredPermissions.filter { permission ->
        ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
    }

    return missingPermissions
}

/**
 * Gets a list of nearby Bluetooth devices
 *
 * @param context
 * @return a list of Bluetooth devices
 */
suspend fun getDeviceList(context: Context): List<BluetoothDevice> {

    // Check permissions
    var missingPermissions = checkPermissions(context)

    // Request the missing permissions
    if (missingPermissions.isNotEmpty()) {
        Log.i("BluetoothScanner", "There are " + missingPermissions.size + " missing permissions to request")
        val requestCode = 99
        requestPermissions(context as Activity, missingPermissions.toTypedArray(), requestCode)
    }

    // Last check
    missingPermissions = checkPermissions(context)
    if (missingPermissions.isNotEmpty()) {
        Log.e("BluetoothScanner", "Could not obtain required permissions!")
        return emptyList()
    }

    // Set up Bluetooth
    Log.i("BluetoothScanner", "Initializing Bluetooth")
    val btManager: BluetoothManager? = getSystemService(context, BluetoothManager::class.java)
    val btAdapter: BluetoothAdapter? = btManager?.adapter

    if (btAdapter == null) {
        // No Bluetooth support
        Log.e("BluetoothScanner", "Device doesn't support Bluetooth!")
        return emptyList()
    }

    // Check if Bluetooth is enabled
    if(!btAdapter.isEnabled) {
        Log.e("BluetoothScanner", "Bluetooth is not enabled!")
        return emptyList()
    }
    Log.i("BluetoothScanner", "Bluetooth is enabled")

    // Create flow to emit discovered devices
    val deviceFlow = callbackFlow {

        // Define broadcast receiver for Bluetooth scan results
        val scanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {

                    // Discovery started
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                        Log.i("BluetoothScanner", "Discovery started")
                    }

                    // A device was found in the scan
                    BluetoothDevice.ACTION_FOUND -> {

                        // Get the device
                        Log.i("BluetoothScanner", "Bluetooth device found")
                        val device: BluetoothDevice? =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) // >= Api 33
                            } else {
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) // < Api 33
                            }

                        // Send the device to the flow
                        device?.let {
                            try {
                                trySend(it)
                            }
                            catch (e: Exception) {
                                Log.e("BluetoothScanner", "Flow was closed when trying to send device")
                            }
                        }
                    }

                    // Discovery complete
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        try {
                            Log.i("BluetoothScanner", "Closing flow")
                            close() // Close the flow
                        }
                        catch (e: Exception) {
                            Log.e("BluetoothScanner", "Error closing the flow")
                        }
                    }
                }
            } // onReceive
        } // BroadcastReceiver

        // Register broadcast receiver for discovery
        Log.i("BluetoothScanner", "Initializing broadcast receiver")
        val scanFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        }
        registerReceiver(context, scanReceiver, scanFilter, ContextCompat.RECEIVER_NOT_EXPORTED)

        // Stop if scanning currently
        if(btAdapter.isDiscovering)  btAdapter.cancelDiscovery()

        // Start discovery
        Log.i("BluetoothScanner", "Starting Bluetooth scan")
        if (!btAdapter.startDiscovery()) {
            // Couldn't start discovery, something went wrong
            Log.e("BluetoothScanner", "Couldn't start discovery!")
            close()
        }

        // Unregister the receiver
        awaitClose {
            Log.i("BluetoothScanner", "Unregistering broadcast receiver")
            context.unregisterReceiver(scanReceiver)
        }
    } // Flow

    // Collect the flow
    val discoveredDevices = mutableListOf<BluetoothDevice>()
    deviceFlow.collect { device ->
        Log.i("BluetoothScanner", "Collecting flow")
        discoveredDevices.add(device)
    }

    // Populate list
    return discoveredDevices
}

/**
 * Get bluetooth device name
 */
private fun getDeviceName(context: Context, device: BluetoothDevice) : String {

    // Get the device name if we have the necessary permissions
    val deviceName =
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            device.name ?: ""
        } else "UNKNOWN"

    return deviceName
}

@Composable
fun DeviceEntry(name: String, address: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(5.dp)) {
        Text( text = name, style = MaterialTheme.typography.headlineMedium )
        Text( text = address )
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceEntryPreview() {
    val context: Context = LocalContext.current
    DeviceEntry("Bluetooth Device 1", "00:11:22:AA:BB:CC")
}