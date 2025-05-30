package com.diracsens.android.fallprevention.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.diracsens.android.fallprevention.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class BluetoothService : Service() {
    private val binder = LocalBinder()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var isScanning = false
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var lastDataTime = 0L
    private var dataCount = 0
    private var isForeground = false
    private var lastConnectedDevice: BluetoothDevice? = null
    private var lastConnectedAddress: String? = null

    // Add discovered devices list and callback
    private val discoveredDevices = mutableListOf<BluetoothDevice>()
    private var deviceDiscoveryCallback: ((List<BluetoothDevice>) -> Unit)? = null

    private val _heartRateFlow = MutableStateFlow<Int>(0)
    val heartRateFlow: StateFlow<Int> = _heartRateFlow

    companion object {
        private const val TAG = "BluetoothService"
        private const val SCAN_PERIOD: Long = 10000 // 10 seconds
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "BluetoothServiceChannel"
        private const val CHANNEL_NAME = "Bluetooth Service"

        // Connection state constants
        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2

        // UUIDs for the Arduino Feather device
        private val SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
        private val CHARACTERISTIC_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")

        // Data type constants
        const val DATA_TYPE_HEART_RATE = "heart_rate"
        const val DATA_TYPE_BLOOD_PRESSURE = "blood_pressure"
        const val DATA_TYPE_BREATHING_RATE = "breathing_rate"
        const val DATA_TYPE_BODY_BALANCE = "body_balance"
        const val DATA_TYPE_GAIT = "gait"
        const val DATA_TYPE_CHROMIUM = "chromium"
        const val DATA_TYPE_LEAD = "lead"
        const val DATA_TYPE_MERCURY = "mercury"
        const val DATA_TYPE_CADMIUM = "cadmium"
        const val DATA_TYPE_SILVER = "silver"
        const val DATA_TYPE_TEMPERATURE = "temperature"

        // Intent actions and extras
        const val ACTION_DATA_AVAILABLE = "com.diracsens.android.fallprevention.ACTION_DATA_AVAILABLE"
        const val ACTION_CONNECT = "com.diracsens.android.fallprevention.ACTION_CONNECT"
        const val ACTION_CONNECTION_STATE_CHANGED = "com.diracsens.android.fallprevention.ACTION_CONNECTION_STATE_CHANGED"
        const val EXTRA_DATA = "com.diracsens.android.fallprevention.EXTRA_DATA"
        const val EXTRA_DATA_TYPE = "com.diracsens.android.fallprevention.EXTRA_DATA_TYPE"
        const val EXTRA_DEVICE_ADDRESS = "com.diracsens.android.fallprevention.EXTRA_DEVICE_ADDRESS"
        const val EXTRA_HEART_RATE = "com.diracsens.android.fallprevention.EXTRA_HEART_RATE"
        const val EXTRA_SYSTOLIC = "com.diracsens.android.fallprevention.EXTRA_SYSTOLIC"
        const val EXTRA_DIASTOLIC = "com.diracsens.android.fallprevention.EXTRA_DIASTOLIC"
        const val EXTRA_BREATHING_RATE = "com.diracsens.android.fallprevention.EXTRA_BREATHING_RATE"
        const val EXTRA_CONNECTION_STATE = "com.diracsens.android.fallprevention.EXTRA_CONNECTION_STATE"
    }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothService = this@BluetoothService
        
        fun getDiscoveredDevices(): List<BluetoothDevice> = discoveredDevices.toList()
        
        fun setDeviceDiscoveryCallback(callback: (List<BluetoothDevice>) -> Unit) {
            deviceDiscoveryCallback = callback
    }

        fun getLastConnectedDevice(): BluetoothDevice? = lastConnectedDevice
        
        fun getLastConnectedAddress(): String? = lastConnectedAddress
    }

    override fun onCreate() {
        super.onCreate()
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service starting with intent: ${intent?.action}")
        
        when (intent?.action) {
                ACTION_CONNECT -> {
                val address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                if (address != null) {
                    val device = bluetoothAdapter?.getRemoteDevice(address)
                    if (device != null) {
                        connect(device)
                    }
                }
            }
        }

        // Start as foreground service if not already
        if (!isForeground) {
            startForeground()
                    }

        return Service.START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Bluetooth service for sensor data collection"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForeground() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DiracSens Sensor")
            .setContentText("Collecting sensor data")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        isForeground = true
        Log.d(TAG, "Service started in foreground")
    }

    private fun stopForeground() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        isForeground = false
        Log.d(TAG, "Service stopped from foreground")
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun startScanning() {
        if (isScanning) {
            Log.d(TAG, "Scan already in progress, ignoring start request")
            return
        }

        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner is null, cannot start scan")
            return
        }

        Log.d(TAG, "Starting Bluetooth LE scan")
        try {
            // Clear previous discovered devices
            discoveredDevices.clear()
            deviceDiscoveryCallback?.invoke(emptyList())
            
            isScanning = true
            bluetoothLeScanner?.startScan(scanCallback)
            Log.d(TAG, "Scan started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start scan", e)
            isScanning = false
        }
    }

    fun stopScanning() {
        if (!isScanning) {
            Log.d(TAG, "No scan in progress, ignoring stop request")
            return
        }

        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner is null, cannot stop scan")
            return
        }

        Log.d(TAG, "Stopping Bluetooth LE scan")
        try {
            isScanning = false
            bluetoothLeScanner?.stopScan(scanCallback)
            Log.d(TAG, "Scan stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop scan", e)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            val deviceName = device.name ?: "Unknown"
            val deviceAddress = device.address
            Log.d(TAG, "Found device: name=$deviceName, address=$deviceAddress, rssi=${result.rssi}")
            
            // Add device to discovered devices if not already present
            if (!discoveredDevices.any { it.address == device.address }) {
                discoveredDevices.add(device)
                deviceDiscoveryCallback?.invoke(discoveredDevices.toList())
            }
        }

        override fun onScanFailed(errorCode: Int) {
            val errorMessage = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Application registration failed"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                else -> "Unknown error"
            }
            Log.e(TAG, "Scan failed with error: $errorCode - $errorMessage")
            isScanning = false
            discoveredDevices.clear()
            deviceDiscoveryCallback?.invoke(emptyList())
        }
    }

    fun connect(device: BluetoothDevice) {
        Log.d(TAG, "Connecting to device: ${device.name ?: "Unknown"} (${device.address})")
        // Store the device for future reconnection
        lastConnectedDevice = device
        lastConnectedAddress = device.address
        // Broadcast connecting state
        broadcastConnectionState(STATE_CONNECTING)
        // Stop scanning if in progress
        stopScanning()
        // Connect to the device
        connectToDevice(device)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        Log.d(TAG, "Connecting to device: ${device.name ?: "Unknown"} (${device.address})")
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceName = gatt.device.name ?: "Unknown"
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to GATT server for device: $deviceName")
                    // Reset data tracking when connecting
                    lastDataTime = 0L
                    dataCount = 0
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from GATT server for device: $deviceName")
                    // Log final stats when disconnecting
                    if (dataCount > 0) {
                        Log.i(TAG, "Connection stats: Received $dataCount data points")
                    }
                    broadcastConnectionState(STATE_DISCONNECTED)
                    gatt.close()
                    bluetoothGatt = null
                }
            }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Connection state change failed with status: $status")
                broadcastConnectionState(STATE_DISCONNECTED)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val deviceName = gatt.device.name ?: "Unknown"
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered for device: $deviceName")
                
                // Log all available services and characteristics
                gatt.services.forEach { service ->
                    Log.d(TAG, "Service: ${service.uuid}")
                    service.characteristics.forEach { characteristic ->
                        Log.d(TAG, "  Characteristic: ${characteristic.uuid}")
                        Log.d(TAG, "    Properties: ${characteristic.properties}")
                        Log.d(TAG, "    Permissions: ${characteristic.permissions}")
                    }
                }

                val service = gatt.getService(SERVICE_UUID)
                if (service == null) {
                    Log.e(TAG, "Required service not found: $SERVICE_UUID")
                    return
                }
                val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID)
                if (characteristic == null) {
                    Log.e(TAG, "Required characteristic not found: $CHARACTERISTIC_UUID")
                    return
                }
                Log.d(TAG, "Found required service and characteristic, enabling notifications")
                
                // Enable notifications for the characteristic
                val success = gatt.setCharacteristicNotification(characteristic, true)
                Log.d(TAG, "setCharacteristicNotification result: $success")
                
                // Get the descriptor for notifications
                        val descriptor = characteristic.getDescriptor(
                            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                        )
                if (descriptor != null) {
                    // Enable notifications by writing to the descriptor
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    val writeSuccess = gatt.writeDescriptor(descriptor)
                    Log.d(TAG, "writeDescriptor result: $writeSuccess")
                } else {
                    Log.e(TAG, "Notification descriptor not found")
                    }
            } else {
                Log.e(TAG, "Service discovery failed with status: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastData = if (lastDataTime > 0) currentTime - lastDataTime else 0
            lastDataTime = currentTime
            dataCount++

            val data = characteristic.getValue()
            Log.d(TAG, "Received data #$dataCount (${timeSinceLastData}ms since last) from ${characteristic.uuid}: ${data.joinToString(", ") { String.format("%02X", it) }}")
            
            // Try to interpret the data in different ways
            if (data.isNotEmpty()) {
                // Log as hex
                Log.d(TAG, "Data as hex: ${data.joinToString(" ") { String.format("%02X", it) }}")
                
                // Log as ASCII if possible
                val ascii = data.map { it.toInt().toChar() }.joinToString("")
                if (ascii.all { it.isPrintable() }) {
                    Log.d(TAG, "Data as ASCII: $ascii")
                }
                
                // Log as integers
                Log.d(TAG, "Data as integers: ${data.map { it.toInt() }.joinToString(", ")}")
                
                // Try to interpret as heart rate if it matches our expected format
                if (data.size >= 2 && data[0].toInt() == 0x07) {
                    val heartRate = data.copyOfRange(1, data.size).toInt()
                    Log.d(TAG, "Interpreted as heart rate: $heartRate")
                    updateHeartRate(heartRate)
                } else {
                    // If it doesn't match our expected format, log that
                    Log.d(TAG, "Data doesn't match expected heart rate format (first byte should be 0x07)")
                }
            }
        }

        private fun Char.isPrintable(): Boolean {
            return this.code in 32..126
        }
    }

    private fun processHeartRateData(data: ByteArray) {
        Log.d(TAG, "Processing heart rate data: ${data.joinToString(", ") { String.format("%02X", it) }}")
        if (data.size < 2) {
            Log.d(TAG, "Data too short: ${data.size} bytes")
            return
                            }

        // Assuming first byte is sensor type (0x07 for heart rate)
        if (data[0].toInt() != 0x07) {
            Log.d(TAG, "Unexpected sensor type: ${String.format("%02X", data[0])}")
            return
        }

        // Convert remaining bytes to heart rate value
        val heartRate = data.copyOfRange(1, data.size).toInt()
        Log.d(TAG, "Extracted heart rate value: $heartRate")
        updateHeartRate(heartRate)
    }

    private fun updateHeartRate(heartRate: Int) {
        _heartRateFlow.value = heartRate
        broadcastUpdate(heartRate)
    }

    private fun broadcastUpdate(heartRate: Int) {
                        val intent = Intent(ACTION_DATA_AVAILABLE).apply {
                            putExtra(EXTRA_DATA_TYPE, DATA_TYPE_HEART_RATE)
            putExtra(EXTRA_DATA, heartRate.toFloat())
                        }
                        sendBroadcast(intent)
                    }

    fun autoReconnect() {
        Log.d(TAG, "Attempting to auto-reconnect")
        lastConnectedAddress?.let { address ->
            val device = bluetoothAdapter?.getRemoteDevice(address)
            if (device != null) {
                Log.d(TAG, "Found last connected device, attempting to reconnect")
                connect(device)
            } else {
                Log.d(TAG, "Last connected device not found")
                broadcastConnectionState(STATE_DISCONNECTED)
            }
        } ?: run {
            Log.d(TAG, "No last connected device found")
            broadcastConnectionState(STATE_DISCONNECTED)
        }
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting from device")
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _heartRateFlow.value = 0
        
        // Don't clear lastConnectedDevice/Address here to allow auto-reconnect
        
        // Broadcast disconnection state
        val intent = Intent(ACTION_CONNECTION_STATE_CHANGED).apply {
            putExtra(EXTRA_CONNECTION_STATE, STATE_DISCONNECTED)
                        }
                        sendBroadcast(intent)
                    }

    private fun broadcastConnectionState(state: Int) {
        val intent = Intent(ACTION_CONNECTION_STATE_CHANGED).apply {
            putExtra(EXTRA_CONNECTION_STATE, state)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service being destroyed")
        try {
            stopForeground()
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            _heartRateFlow.value = 0
        } catch (e: Exception) {
            Log.e(TAG, "Error during service cleanup", e)
        }
    }
}

// Extension function to convert ByteArray to Int
fun ByteArray.toInt(): Int {
    if (size != 2) return 0
    return ((this[0].toInt() and 0xFF) shl 8) or (this[1].toInt() and 0xFF)
}