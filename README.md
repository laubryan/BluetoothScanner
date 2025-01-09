#  BluetoothScanner

### Description
A native Android app that scans for nearby Bluetooth devices. For each device that's discovered, it displays the device's friendly name 
(if available, some devices don't provide it), hardware address and device type.

This came about because of a different project I'm working on that needs Bluetooth connectivity, so I wrote this app first to understand the API and workflow.

### Tech

The app is native Android, written in Kotlin and using Jetpack Compose for the UI. 
The Bluetooth scanning class uses a BroadcastReceiver to find devices asynchronously and updates the UI list state as they come in.

### Improvements

Some improvements that could be made:

- Show the signal strength for each device (i.e. the RSSI)
- Support Bluetooth Low Energy (BLE)
- List paired devices first since those would be quicker to determine
- List protocols used by the devices
