package com.diracsens.android.fallprevention.services

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.diracsens.android.fallprevention.models.BloodPressureReading
import com.diracsens.android.fallprevention.models.BreathingRateReading
import com.diracsens.android.fallprevention.models.HeartRateReading
import com.diracsens.android.fallprevention.repositories.HealthDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class BluetoothService : Service() {
    companion object {
        private const val TAG = "BluetoothService"

        // Actions
        const val ACTION_CONNECT = "com.diracsens.action.CONNECT"
        const val ACTION_DISCONNECT = "com.diracsens.action.DISCONNECT"
        const val ACTION_DATA_AVAILABLE = "com.diracsens.action.DATA_AVAILABLE"

        // Data types
        const val DATA_TYPE_BLOOD_PRESSURE = "blood_pressure"
        const val DATA_TYPE_HEART_RATE = "heart_rate"
        const val DATA_TYPE_BREATHING_RATE = "breathing_rate"
        const val DATA_TYPE_BODY_BALANCE = "body_balance"
        const val DATA_TYPE_GAIT = "gait"

        // Extras
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        const val EXTRA_DATA_TYPE = "data_type"
        const val EXTRA_SYSTOLIC = "systolic"
        const val EXTRA_DIASTOLIC = "diastolic"
        const val EXTRA_HEART_RATE = "heart_rate"
        const val EXTRA_BREATHING_RATE = "breathing_rate"

        // Arduino Feather service and characteristic UUIDs
        // These should match the UUIDs used in your Arduino code
        private val ARDUINO_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        private val ARDUINO_RX_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E") // TX from Arduino perspective
        private val ARDUINO_TX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E") // RX from Arduino perspective
    }

    private val binder = LocalBinder()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private val connectedDevices = mutableMapOf<String, BluetoothGatt>()
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothService = this@BluetoothService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_CONNECT -> {
                    val address = it.getStringExtra(EXTRA_DEVICE_ADDRESS)
                    address?.let { addr ->
                        connectToDevice(addr)
                    }
                }
                ACTION_DISCONNECT -> {
                    val address = it.getStringExtra(EXTRA_DEVICE_ADDRESS)
                    address?.let { addr ->
                        disconnectDevice(addr)
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun connectToDevice(address: String) {
        val device = bluetoothAdapter?.getRemoteDevice(address)
        device?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                bluetoothGatt = device.connectGatt(this, false, gattCallback)
            }
        }
    }

    private fun disconnectDevice(address: String) {
        connectedDevices[address]?.let { gatt ->
            gatt.disconnect()
            connectedDevices.remove(address)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to GATT server: $deviceAddress")
                    connectedDevices[deviceAddress] = gatt
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from GATT server: $deviceAddress")
                    connectedDevices.remove(deviceAddress)
                    gatt.close()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Find the Arduino service
                val service = gatt.getService(ARDUINO_SERVICE_UUID)
                service?.let {
                    // Enable notifications for the TX characteristic (data from Arduino)
                    val txChar = service.getCharacteristic(ARDUINO_TX_CHAR_UUID)
                    txChar?.let { characteristic ->
                        gatt.setCharacteristicNotification(characteristic, true)

                        // Enable remote notifications
                        val descriptor = characteristic.getDescriptor(
                            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                        )
                        descriptor?.let {
                            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(it)
                        }
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (ARDUINO_TX_CHAR_UUID == characteristic.uuid) {
                // Process data from Arduino
                val data = characteristic.value
                processArduinoData(data)
            }
        }
    }

    private fun processArduinoData(data: ByteArray) {
        // Parse the data from Arduino Feather
        // Format depends on how you've structured your Arduino data
        // Example format: "BP:120/80" or "HR:75" or "BR:16"

        val dataString = String(data)
        Log.d(TAG, "Received data: $dataString")

        try {
            // Split by colon to get type and value
            val parts = dataString.split(":")
            if (parts.size == 2) {
                val type = parts[0]
                val value = parts[1]

                when (type) {
                    "BP" -> {
                        // Blood pressure format: "BP:120/80"
                        val bpValues = value.split("/")
                        if (bpValues.size == 2) {
                            val systolic = bpValues[0].toInt()
                            val diastolic = bpValues[1].toInt()

                            // Save to database
                            val timestamp = System.currentTimeMillis()
                            serviceScope.launch {
                                HealthDataRepository.getInstance().insertBloodPressure(
                                    BloodPressureReading(0, timestamp, systolic, diastolic)
                                )
                            }

                            // Broadcast the data
                            val intent = Intent(ACTION_DATA_AVAILABLE).apply {
                                putExtra(EXTRA_DATA_TYPE, DATA_TYPE_BLOOD_PRESSURE)
                                putExtra(EXTRA_SYSTOLIC, systolic)
                                putExtra(EXTRA_DIASTOLIC, diastolic)
                            }
                            sendBroadcast(intent)
                        }
                    }
                    "HR" -> {
                        // Heart rate format: "HR:75"
                        val heartRate = value.toInt()

                        // Save to database
                        val timestamp = System.currentTimeMillis()
                        serviceScope.launch {
                            HealthDataRepository.getInstance().insertHeartRate(
                                HeartRateReading(0, timestamp, heartRate)
                            )
                        }

                        // Broadcast the data
                        val intent = Intent(ACTION_DATA_AVAILABLE).apply {
                            putExtra(EXTRA_DATA_TYPE, DATA_TYPE_HEART_RATE)
                            putExtra(EXTRA_HEART_RATE, heartRate)
                        }
                        sendBroadcast(intent)
                    }
                    "BR" -> {
                        // Breathing rate format: "BR:16"
                        val breathingRate = value.toInt()

                        // Save to database
                        val timestamp = System.currentTimeMillis()
                        serviceScope.launch {
                            HealthDataRepository.getInstance().insertBreathingRate(
                                BreathingRateReading(0, timestamp, breathingRate)
                            )
                        }

                        // Broadcast the data
                        val intent = Intent(ACTION_DATA_AVAILABLE).apply {
                            putExtra(EXTRA_DATA_TYPE, DATA_TYPE_BREATHING_RATE)
                            putExtra(EXTRA_BREATHING_RATE, breathingRate)
                        }
                        sendBroadcast(intent)
                    }
                    // Add other data types as needed
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing data: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Disconnect all devices
        connectedDevices.values.forEach { it.disconnect() }
        connectedDevices.clear()
    }
}