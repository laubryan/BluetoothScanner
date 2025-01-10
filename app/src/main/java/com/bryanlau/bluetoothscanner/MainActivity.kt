package com.bryanlau.bluetoothscanner

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bryanlau.bluetoothscanner.bluetoothhelper.BluetoothDeviceInfo
import com.bryanlau.bluetoothscanner.bluetoothhelper.BluetoothHelper
import com.bryanlau.bluetoothscanner.ui.theme.BluetoothScannerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var _bluetooth : BluetoothHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize BluetoothHelper class
        _bluetooth = BluetoothHelper(this)

        // Request permissions
        _bluetooth.requestPermissions(this)

        enableEdgeToEdge()
        setContent {
            BluetoothScannerTheme {
                MainView(_bluetooth)
            }
        }
    }
}

@Composable
fun MainView(btHelper: BluetoothHelper) {
    val deviceList = remember { mutableStateListOf<BluetoothDeviceInfo>() }
    MainPage(btHelper, deviceList)
}

@Preview(showBackground = true, widthDp = 320, heightDp = 640)
@Composable
fun MainViewPreview() {
    BluetoothScannerTheme {
        val btHelper = BluetoothHelper(LocalContext.current)
        val deviceList = listOf(
            BluetoothDeviceInfo("Device 1", "00:11:22:33:AA:BB", "Phone"),
            BluetoothDeviceInfo("Device 2", "22:33:44:CC:DD:EE", "Computer"),
            BluetoothDeviceInfo("Device 3", "23:45:67:89:AB:CD", "Audio")
        )
        MainPage(btHelper, deviceList.toMutableStateList())
    }
}

/**
 * Main page composable
 */
@Composable
fun MainPage(
    btHelper: BluetoothHelper,
    deviceList: SnapshotStateList<BluetoothDeviceInfo>,
    modifier: Modifier = Modifier
) {

    var isScanning by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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
                            // Set initial state
                            isScanning = true

                            // Start discovery
                            coroutineScope.launch(Dispatchers.IO) {
                                getDeviceList(
                                    btHelper,
                                    onDeviceFound = { device -> deviceList.add(device) },
                                    onScanComplete = {
                                        isScanning = false
                                        Log.i("BluetoothScanner", "Scan complete")
                                    }
                                )
                            }
                        }
                        else {
                            // TODO: Stop scanning
                            isScanning = false
                        }
                    }) {
                        Text(text = if (isScanning) "Stop Scanning" else "Scan Now")
                    }
                }
            }
        }
    ) {
        // View Body
        padding ->
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Display list of devices
            DeviceList(deviceList)
        }
    }
}

@Composable
fun DeviceList(devices: SnapshotStateList<BluetoothDeviceInfo>) {
    LazyColumn {
        items(devices) { device ->
            DeviceEntry(device.name,  device.address, device.deviceClass)
        }
    }
}

/**
 * Gets a list of nearby Bluetooth devices
 *
 * @param btHelper
 * @return a list of Bluetooth devices
 */
private fun getDeviceList(btHelper: BluetoothHelper, onDeviceFound: (BluetoothDeviceInfo) -> Unit, onScanComplete: () -> Unit) {

    // Check permissions
    if (!btHelper.hasSufficientPermissions()) {
        Log.e("BluetoothScanner", "App does not have required permissions!")
        return
    }

    // Set up Bluetooth
    Log.i("BluetoothScanner", "Initializing Bluetooth")
    if(!btHelper.initialize()) {
        Log.e("BluetoothScanner", "Could not initialize Bluetooth")
    }
    Log.i("BluetoothScanner", "Bluetooth initialized")

    // Scan for devices
    btHelper.scanForDevices(onDeviceFound, onScanComplete)
}

@Composable
fun DeviceEntry(name: String, address: String, deviceClass: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier
        .fillMaxWidth()
        .padding(5.dp)) {
        Text( text = name, style = MaterialTheme.typography.headlineSmall )
        Text( text = address )
        Text( text = deviceClass )
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceEntryPreview() {
    DeviceEntry("Bluetooth Device 1", "00:11:22:AA:BB:CC", "Phone")
}