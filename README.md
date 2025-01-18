#  BluetoothScanner

![BluetoothScanner app screenshot](https://github.com/laubryan/BluetoothScanner/blob/ca7ad2bf2db1deee024279388a51d88c4602ed98/screenshots/bluetooth-scanner-screenshot.jpg)

### Description
A native Android app that scans for nearby Bluetooth devices. For each device that's discovered, it displays the device's friendly name 
(if available, some devices don't provide it), hardware address and device type.

This came about because of a different project I'm working on that needs Bluetooth connectivity, so I wrote this app first to understand the API and workflow.
I'll be customizing it for that project, but in the meantime it's a useful example of how the basic Bluetooth API works in Android, and perhaps more generally how Jetpack Compose works.

### Tech

The app is native Android, written in Kotlin and using Jetpack Compose for the UI. 
The Bluetooth scanning class finds devices asynchronously and updates the UI list state as they come in, with the option of cancelling the scan before it's finished.

### Improvements

Some improvements that could be made:

- Show the signal strength for each device (i.e. the RSSI)
- List paired devices first since those would be quicker to determine
- List protocols used by the devices
