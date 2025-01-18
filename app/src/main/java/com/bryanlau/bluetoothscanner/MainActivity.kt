package com.bryanlau.bluetoothscanner

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardElevation
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Label
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bryanlau.bluetoothscanner.bluetoothhelper.BluetoothDeviceInfo
import com.bryanlau.bluetoothscanner.bluetoothhelper.BluetoothHelper
import com.bryanlau.bluetoothscanner.ui.theme.BluetoothScannerTheme
import kotlinx.coroutines.CoroutineScope
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
            BluetoothDeviceInfo("Device 3", "23:45:67:89:AB:CD", "Audio/Video"),
            BluetoothDeviceInfo("Device 4", "24:6F:13:57:AB:7E", "Unknown Type")
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
    var scanBLE by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            BottomAppBar() {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        onClick = { scanBLE = !scanBLE },
                        label = { Text("BLE") },
                        selected = scanBLE,
                        leadingIcon = if (scanBLE) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = "Done icon",
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        }
                        else null,
                        enabled = !isScanning
                    )
                    Button(onClick = {
                        // Idle
                        if(!isScanning) {
                            // Set initial state
                            isScanning = true
                            deviceList.clear()

                            // Start discovery
                            startDiscovery(coroutineScope, btHelper, scanBLE, deviceList, onScanComplete = { isScanning = false })
                        }
                        else {
                            // Cancel scan
                            Log.i("BluetoothScanner", "Cancel scan")
                            btHelper.cancelScan(scanBLE, onScanComplete = { isScanning = false })
                        }
                    }) {
                        Text(text = if (isScanning) "Stop Scanning" else "Scan Now")
                    }
                }
            }
        }
    ) {
        // View Body
        paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.LightGray)
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Display list of devices
            DeviceList(deviceList)
        }
    }
}

/**
 * Scan for normal Bluetooth devices
 *
 * @param btHelper Bluetooth helper class
 * @param deviceList list of discovered devices
 * @param onScanComplete scan complete callback
 */
private fun startDiscovery(
    coroutineScope: CoroutineScope,
    btHelper: BluetoothHelper,
    scanForBLE: Boolean,
    deviceList: SnapshotStateList<BluetoothDeviceInfo>,
    onScanComplete: () -> Unit
) {
    coroutineScope.launch(Dispatchers.IO) {
        getDeviceList(
            btHelper,
            scanForBLE,
            onDeviceFound = { device ->
                // Don't add duplicates
                if (!deviceList.contains(device)) {
                    deviceList.add(device)
                }
            },
            onScanComplete = {
                onScanComplete()
                Log.i("BluetoothScanner", "Scan complete")
            }
        )
    }
}

@Composable
fun DeviceList(devices: SnapshotStateList<BluetoothDeviceInfo>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .fillMaxSize()
    ) {
        items(devices) { device ->
            DeviceEntry(device.name, device.address, device.deviceClass)
        }
    }
}

/**
 * Gets a list of nearby Bluetooth devices
 *
 * @param btHelper
 * @return a list of Bluetooth devices
 */
private fun getDeviceList(btHelper: BluetoothHelper, scanForBLE: Boolean, onDeviceFound: (BluetoothDeviceInfo) -> Unit, onScanComplete: () -> Unit) {

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
    btHelper.scanForDevices(scanForBLE, onDeviceFound, onScanComplete)
}

@Composable
fun DeviceEntry(name: String, address: String, deviceClass: String, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = Modifier.shadow(elevation = 3.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(text = name, style = MaterialTheme.typography.headlineSmall)
            Text(text = address)
            Text(text = deviceClass)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceEntryPreview() {
    DeviceEntry("Bluetooth Device 1", "00:11:22:AA:BB:CC", "Phone")
}