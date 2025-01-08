package com.bryanlau.bluetoothscanner

import android.R
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
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
import androidx.core.content.ContextCompat
import com.bryanlau.bluetoothscanner.bluetoothhelper.BluetoothDeviceInfo
import com.bryanlau.bluetoothscanner.bluetoothhelper.BluetoothHelper
import com.bryanlau.bluetoothscanner.ui.theme.BluetoothScannerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.Manifest.permission.*
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions

class MainActivity : ComponentActivity() {

    lateinit var _bluetooth : BluetoothHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize BluetoothHelper class
        _bluetooth = BluetoothHelper(this)

        // Request permissions
        val requestCode = 9999
        val permissions = arrayOf(ACCESS_FINE_LOCATION)
        ActivityCompat.requestPermissions(this, permissions, requestCode)

        enableEdgeToEdge()
        setContent {
            BluetoothScannerTheme {
                MainPage(_bluetooth)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 640)
@Composable
fun MainPagePreview() {
    BluetoothScannerTheme {
        val btHelper = BluetoothHelper(LocalContext.current)
        MainPage(btHelper, modifier = Modifier)
    }
}

/**
 * Main page composable
 */
@Composable
fun MainPage(btHelper: BluetoothHelper, modifier: Modifier = Modifier) {

    var isScanning by remember { mutableStateOf(false) }
    var discoveredDevices: List<BluetoothDeviceInfo> = emptyList()
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
                                discoveredDevices = getDeviceList(context, btHelper)
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
                for (device in discoveredDevices) {
                    DeviceEntry(device.name, device.address)
                }
            }
        }
    }
}

/**
 * Gets a list of nearby Bluetooth devices
 *
 * @param btHelper
 * @return a list of Bluetooth devices
 */
suspend fun getDeviceList(context: Context, btHelper: BluetoothHelper): List<BluetoothDeviceInfo> {

    // Check permissions
    if (!btHelper.hasSufficientPermissions()) {
        Log.e("BluetoothScanner", "App does not have required permissions!")
        return emptyList()
    }

    // Set up Bluetooth
    Log.i("BluetoothScanner", "Initializing Bluetooth")
    if(!btHelper.initialize()) {
        Log.e("BluetoothScanner", "Could not initialize Bluetooth")
    }
    Log.i("BluetoothScanner", "Bluetooth initialized")

    // Scan for devices
    btHelper.scanForDevices(::onDeviceFound, ::onScanComplete)

    return emptyList()
}

/**
 * Process device found in scan
 */
fun onDeviceFound(deviceInfo: BluetoothDeviceInfo) {
    Log.i("BluetoothScanner", "Found device ${deviceInfo.name} (${deviceInfo.address})")
}

/**
 * Device scan finished
 */
fun onScanComplete() {
    Log.i("BluetoothScanner", "Scan complete")
}

@Composable
fun DeviceEntry(name: String, address: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier
        .fillMaxWidth()
        .padding(5.dp)) {
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