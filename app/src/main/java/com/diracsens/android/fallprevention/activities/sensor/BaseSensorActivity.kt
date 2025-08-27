package com.diracsens.android.fallprevention.activities.sensor

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.diracsens.android.fallprevention.R
import com.diracsens.android.fallprevention.databinding.ActivitySensorBinding
import com.diracsens.android.fallprevention.services.BluetoothService
import com.diracsens.android.fallprevention.viewmodels.HealthMetricsViewModel
import androidx.activity.viewModels
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.viewbinding.ViewBinding
import com.google.android.material.button.MaterialButton
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import com.diracsens.android.fallprevention.receivers.BluetoothDataReceiver
import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.Job
import android.view.ViewGroup
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.diracsens.android.fallprevention.databinding.ItemDeviceBinding
import com.diracsens.android.fallprevention.databinding.DialogDeviceSelectionBinding
import kotlinx.coroutines.isActive
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

abstract class BaseSensorActivity<B : ViewBinding> : AppCompatActivity() {
    protected lateinit var binding: B
    protected val viewModel: HealthMetricsViewModel by viewModels()
    protected var bluetoothService: BluetoothService? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanning = false
    private val handler = Handler(Looper.getMainLooper())
    private val SCAN_PERIOD: Long = 10000 // 10 seconds
    private var deviceSelectionDialog: AlertDialog? = null
    private var isReceiverRegistered = false
    private var bluetoothReceiver: BluetoothDataReceiver? = null
    private var dataJob: Job? = null
    private var deviceAdapter: DeviceAdapter? = null
    private var pairedDeviceAdapter: DeviceAdapter? = null
    private var serviceBinder: BluetoothService.LocalBinder? = null
    private var scanningJob: Job? = null
    private var isScanning = false
    private val scanHandler = Handler(Looper.getMainLooper())
    private val stopScanRunnable = Runnable {
        if (isScanning) {
            Log.d("BaseSensorActivity", "Stopping scan after 1 second")
            stopScanning()
        }
    }
    private var currentConnectionState = BluetoothService.STATE_DISCONNECTED
    private var deviceDialogBinding: DialogDeviceSelectionBinding? = null

    protected val buttonConnect: MaterialButton by ConnectButtonDelegate()

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d("BaseSensorActivity", "Permission request results:")
        permissions.entries.forEach { (permission, granted) ->
            Log.d("BaseSensorActivity", "  $permission = $granted")
        }
        
        if (permissions.all { it.value }) {
            Log.d("BaseSensorActivity", "All permissions granted, showing device selection dialog")
            if (bluetoothAdapter?.isEnabled == true) {
                updateDeviceSelectionDialog(emptyList())
            } else {
                // Request Bluetooth enable
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                bluetoothEnableLauncher.launch(enableBtIntent)
            }
        } else {
            Log.d("BaseSensorActivity", "Some permissions were denied")
            permissions.entries.filter { !it.value }.forEach { (permission, _) ->
                Log.d("BaseSensorActivity", "Denied permission: $permission")
                if (shouldShowRequestPermissionRationale(permission)) {
                    Log.d("BaseSensorActivity", "Should show rationale for $permission")
                } else {
                    Log.d("BaseSensorActivity", "Permission $permission permanently denied")
                }
            }
            Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_LONG).show()
        }
    }

    private val bluetoothEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Just bind to service instead of starting it
            try {
                val intent = Intent(this, BluetoothService::class.java)
                bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                Log.d("BaseSensorActivity", "Binding to Bluetooth service after enabling Bluetooth")
            } catch (e: Exception) {
                Log.e("BaseSensorActivity", "Error binding to service after enabling Bluetooth", e)
            }
        } else {
            Toast.makeText(this, "Bluetooth must be enabled", Toast.LENGTH_LONG).show()
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("BaseSensorActivity", "Service connected")
            try {
                if (service !is BluetoothService.LocalBinder) {
                    Log.e("BaseSensorActivity", "Service binder is not LocalBinder")
                    return
                }
                
                serviceBinder = service
                bluetoothService = service.getService()
                
                // Set up device discovery callback only when explicitly connecting
                service.setDeviceDiscoveryCallback { devices ->
                    updateDeviceSelectionDialog(devices)
                }

                // Check service state and update UI
                checkServiceState()
                // Notify subclass that bluetoothService is ready
                onBluetoothServiceReady()
            } catch (e: Exception) {
                Log.e("BaseSensorActivity", "Error in service connection", e)
                bluetoothService = null
                serviceBinder = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("BaseSensorActivity", "Service disconnected")
            bluetoothService = null
            serviceBinder = null
            dataJob?.cancel()
            dataJob = null
        }
    }

    private fun checkServiceState() {
        Log.d("BaseSensorActivity", "Checking service state")
        try {
            bluetoothService?.let { service ->
                // Check if we have an active connection by looking at the heart rate
                val heartRate = service.heartRateFlow.value
                val isConnected = heartRate > 0
                Log.d("BaseSensorActivity", "Service state - heartRate: $heartRate, isConnected: $isConnected")
                
                if (isConnected) {
                    Log.d("BaseSensorActivity", "Service has active connection, updating UI")
                    buttonConnect.text = "Disconnect"
                    if (dataJob == null) {
                        startObservingData()
                    }
                } else {
                    Log.d("BaseSensorActivity", "Service has no active connection")
                    buttonConnect.text = "Connect to Sensor"
                    dataJob?.cancel()
                    dataJob = null
                }
                updateUI()
            } ?: run {
                Log.d("BaseSensorActivity", "No service available")
                buttonConnect.text = "Connect to Sensor"
                updateUI()
            }
        } catch (e: Exception) {
            Log.e("BaseSensorActivity", "Error checking service state", e)
            buttonConnect.text = "Connect to Sensor"
            updateUI()
        }
    }

    private fun startObservingData() {
        Log.d("BaseSensorActivity", "startObservingData called. dataJob is ${if (dataJob == null) "null" else "active"}")
        try {
            // Cancel any existing job
            dataJob?.cancel()
            Log.d("BaseSensorActivity", "Existing dataJob cancelled before starting new one")
            dataJob = lifecycleScope.launch {
                try {
                    bluetoothService?.heartRateFlow?.collectLatest { heartRate ->
                        Log.d("BaseSensorActivity", "Received heart rate update: $heartRate (job: $this)")
                        // Update heart rate regardless of connection state
                        viewModel.updateHeartRate(heartRate)
                        runOnUiThread {
                            updateUI()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BaseSensorActivity", "Error collecting heart rate data", e)
                }
            }
            Log.d("BaseSensorActivity", "New dataJob started: $dataJob")
        } catch (e: Exception) {
            Log.e("BaseSensorActivity", "Error starting data observation", e)
        }
    }

    private inner class ConnectButtonDelegate : ReadOnlyProperty<BaseSensorActivity<B>, MaterialButton> {
        override fun getValue(thisRef: BaseSensorActivity<B>, property: KProperty<*>): MaterialButton {
            return binding.root.findViewById(R.id.buttonConnect)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("BaseSensorActivity", "onCreate")
        setupBluetooth()
        setupUI()
        
        // Create and register receiver if not already
        if (bluetoothReceiver == null) {
            setupBluetoothReceiver()
        }

        // Just bind to existing service
        try {
            val intent = Intent(this, BluetoothService::class.java)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.d("BaseSensorActivity", "Binding to Bluetooth service")
        } catch (e: Exception) {
            Log.e("BaseSensorActivity", "Error binding to service", e)
        }
    }

    private fun setupBluetooth() {
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        // Don't create service instance here, it will be created by bindService
    }

    private fun setupUI() {
        Log.d("BaseSensorActivity", "Setting up UI, attaching click listener to connect button")
        try {
            buttonConnect.setOnClickListener {
                Log.d("BaseSensorActivity", "Connect button clicked - button state: enabled=${buttonConnect.isEnabled}, visible=${buttonConnect.visibility}")
                try {
                    when (buttonConnect.text.toString()) {
                        "Disconnect" -> {
                            // Disconnect from current device
                            Log.d("BaseSensorActivity", "Disconnecting from device")
                            bluetoothService?.let { service ->
                                // Stop any ongoing scanning first
                                service.stopScanning()
                                // Disconnect and clean up
                                service.disconnect()
                                // Update UI immediately
                                buttonConnect.text = "Connect to Sensor"
                                viewModel.updateHeartRate(0)
                                updateUI()
                                // Cancel any ongoing data collection
                                dataJob?.cancel()
                                dataJob = null
                            } ?: run {
                                Log.e("BaseSensorActivity", "Cannot disconnect - service is null")
                                buttonConnect.text = "Connect to Sensor"
                            }
                        }
                        "Connect to Sensor" -> {
                            // Show device selection dialog
                            if (checkBluetoothPermissions()) {
                                Log.d("BaseSensorActivity", "Bluetooth permissions granted, checking if Bluetooth is enabled")
                                buttonConnect.text = "Connecting..."
                                
                                // Ensure service is running and bound
                                ensureServiceRunning()
                                
                                // Show dialog without starting scan
                                if (bluetoothAdapter?.isEnabled == true) {
                                    updateDeviceSelectionDialog(emptyList())
                                } else {
                                    // Request Bluetooth enable
                                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                    bluetoothEnableLauncher.launch(enableBtIntent)
                                }
                            } else {
                                Log.d("BaseSensorActivity", "Bluetooth permissions not granted, permission request launched")
                            }
                        }
                        "Cancel" -> {
                            // Cancel connection attempt
                            Log.d("BaseSensorActivity", "Canceling connection attempt")
                            bluetoothService?.let { service ->
                                service.stopScanning()
                                service.disconnect()
                            }
                            buttonConnect.text = "Connect to Sensor"
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BaseSensorActivity", "Error in button click handler", e)
                    buttonConnect.text = "Connect to Sensor"
                }
            }
            Log.d("BaseSensorActivity", "Successfully attached click listener to connect button")
        } catch (e: Exception) {
            Log.e("BaseSensorActivity", "Error setting up button click listener", e)
        }
    }

    private fun ensureServiceRunning() {
        Log.d("BaseSensorActivity", "Ensuring Bluetooth service is running and bound")
        try {
            // Start the service if not already running
            val intent = Intent(this, BluetoothService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            // Bind to service if not already bound
            if (serviceBinder == null) {
                Log.d("BaseSensorActivity", "Service not bound, attempting to bind")
                bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            } else {
                Log.d("BaseSensorActivity", "Service already bound")
            }
        } catch (e: Exception) {
            Log.e("BaseSensorActivity", "Error ensuring service is running", e)
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        Log.d("BaseSensorActivity", "Checking Bluetooth permissions - Android version: ${Build.VERSION.SDK_INT}")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.d("BaseSensorActivity", "Device is Android 12+ (API ${Build.VERSION.SDK_INT})")
                val hasConnect = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
                val hasScan = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED
                
                Log.d("BaseSensorActivity", "Current permission status - BLUETOOTH_CONNECT: $hasConnect, BLUETOOTH_SCAN: $hasScan")
                
                if (!hasConnect || !hasScan) {
                    Log.d("BaseSensorActivity", "Requesting Android 12+ Bluetooth permissions")
                    bluetoothPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                        )
                    )
                    return false
                }
                Log.d("BaseSensorActivity", "Android 12+ Bluetooth permissions already granted")
            } else {
                Log.d("BaseSensorActivity", "Device is Android ${Build.VERSION.SDK_INT}, checking location permission")
                val hasLocation = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                
                Log.d("BaseSensorActivity", "Current location permission status: $hasLocation")
                
                if (!hasLocation) {
                    Log.d("BaseSensorActivity", "Requesting location permission")
                    bluetoothPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                    return false
                }
                Log.d("BaseSensorActivity", "Location permission already granted")
            }
            return true
        } catch (e: Exception) {
            Log.e("BaseSensorActivity", "Error checking Bluetooth permissions", e)
            return false
        }
    }

    private fun scanLeDevice() {
        if (!scanning) {
            handler.postDelayed({
                scanning = false
                bluetoothLeScanner?.stopScan(leScanCallback)
            }, SCAN_PERIOD)

            scanning = true
            bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
            bluetoothLeScanner?.startScan(leScanCallback)
            Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show()
        } else {
            scanning = false
            bluetoothLeScanner?.stopScan(leScanCallback)
        }
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            // Check if this is our Arduino Feather device
            if (isArduinoFeatherDevice(device)) {
                Toast.makeText(this@BaseSensorActivity, "Found DiracSens device: ${device.name}", Toast.LENGTH_SHORT).show()
                connectToDevice(device)
            }
        }
    }

    private fun isArduinoFeatherDevice(device: android.bluetooth.BluetoothDevice): Boolean {
        val deviceName = device.name ?: return false
        return deviceName.contains("WirelessMonitor-100", ignoreCase = true) ||
               deviceName.contains("TestSignalSender", ignoreCase = true)
    }

    private fun connectToDevice(device: android.bluetooth.BluetoothDevice) {
        Log.d("BaseSensorActivity", "Attempting to connect to device: ${device.name} (${device.address})")
        val intent = Intent(this, BluetoothService::class.java).apply {
            action = BluetoothService.ACTION_CONNECT
            putExtra(BluetoothService.EXTRA_DEVICE_ADDRESS, device.address)
        }
        startService(intent)
    }

    private fun cleanupBluetoothService() {
        Log.d("BaseSensorActivity", "Cleaning up Bluetooth service")
        try {
            bluetoothService?.let { service ->
                // Stop scanning if in progress
                service.stopScanning()
                
                // Disconnect from any connected device
                service.disconnect()
                
                // Update UI
                viewModel.updateHeartRate(0)
                updateUI()
                
                // Cancel any ongoing data collection
                dataJob?.cancel()
                dataJob = null
                
                // Unbind from service
                if (serviceBinder != null) {
                    unbindService(serviceConnection)
                    bluetoothService = null
                    serviceBinder = null
                }
            }
            
            Log.d("BaseSensorActivity", "Bluetooth service cleanup completed")
        } catch (e: Exception) {
            Log.e("BaseSensorActivity", "Error during Bluetooth service cleanup", e)
        }
    }

    private inner class DeviceAdapter(
        private val devices: MutableList<BluetoothDevice>,
        private val onDeviceClick: (BluetoothDevice) -> Unit
    ) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

        inner class DeviceViewHolder(private val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(device: BluetoothDevice) {
                binding.textDeviceName.text = device.name ?: "Unknown Device"
                binding.textDeviceAddress.text = device.address
                binding.root.setOnClickListener { onDeviceClick(device) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val binding = ItemDeviceBinding.inflate(layoutInflater, parent, false)
            return DeviceViewHolder(binding)
        }

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            holder.bind(devices[position])
        }

        override fun getItemCount() = devices.size

        fun updateDevices(newDevices: List<BluetoothDevice>) {
            val oldSize = devices.size
            devices.clear()
            devices.addAll(newDevices)
            // Use notifyItemRangeChanged for smoother updates
            if (oldSize == newDevices.size) {
                notifyItemRangeChanged(0, oldSize)
            } else if (oldSize < newDevices.size) {
                notifyItemRangeChanged(0, oldSize)
                notifyItemRangeInserted(oldSize, newDevices.size - oldSize)
            } else {
                notifyItemRangeChanged(0, newDevices.size)
                notifyItemRangeRemoved(newDevices.size, oldSize - newDevices.size)
            }
        }
    }

    private fun updateDeviceSelectionDialog(devices: List<BluetoothDevice>) {
        Log.d("BaseSensorActivity", "updateDeviceSelectionDialog called - dialog showing: ${deviceSelectionDialog?.isShowing}")
        // If dialog is already showing, just update the device list
        if (deviceSelectionDialog?.isShowing == true) {
            deviceAdapter?.updateDevices(devices)
            return
        }

        // Ensure service is running before showing dialog
        ensureServiceRunning()

        val dialogView = layoutInflater.inflate(R.layout.dialog_device_selection, null)
        val binding = DialogDeviceSelectionBinding.bind(dialogView)
        deviceDialogBinding = binding

        // Set up RecyclerViews
        binding.recyclerAvailableDevices.layoutManager = LinearLayoutManager(this)
        binding.recyclerPairedDevices.layoutManager = LinearLayoutManager(this)

        // Get paired devices
        val pairedDevices = bluetoothAdapter?.bondedDevices?.filter { 
            it.name?.contains("WirelessMonitor-100", ignoreCase = true) == true ||
            it.name?.contains("TestSignalSender", ignoreCase = true) == true
        }?.toMutableList() ?: mutableListOf()

        // Set up adapters
        deviceAdapter = DeviceAdapter(mutableListOf()) { device ->
            Log.d("BaseSensorActivity", "Device selected from available devices")
            stopScanning()
            deviceSelectionDialog?.dismiss()
            deviceSelectionDialog = null
            Log.d("BaseSensorActivity", "Attempting to connect to device: ${device.name} (${device.address})")
            bluetoothService?.connect(device)
        }

        pairedDeviceAdapter = DeviceAdapter(pairedDevices) { device ->
            Log.d("BaseSensorActivity", "Paired device selected")
            stopScanning()
            deviceSelectionDialog?.dismiss()
            deviceSelectionDialog = null
            Log.d("BaseSensorActivity", "Attempting to connect to paired device: ${device.name} (${device.address})")
            bluetoothService?.connect(device)
        }

        binding.recyclerAvailableDevices.adapter = deviceAdapter
        binding.recyclerPairedDevices.adapter = pairedDeviceAdapter

        // Show/hide paired devices section
        if (pairedDevices.isNotEmpty()) {
            binding.textPairedDevices.visibility = View.VISIBLE
            binding.recyclerPairedDevices.visibility = View.VISIBLE
        }

        // Set up scan button
        binding.buttonScan.visibility = View.VISIBLE
        binding.buttonScan.setOnClickListener {
            startScanning(binding)
        }
        binding.progressScanning.visibility = View.GONE

        // Create and show dialog
        deviceSelectionDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .setOnCancelListener {
                Log.d("BaseSensorActivity", "Dialog cancelled")
                stopScanning()
                cleanupBluetoothService()
                buttonConnect.text = "Connect to Sensor"
                deviceSelectionDialog = null
            }
            .create()

        deviceSelectionDialog?.show()
        Log.d("BaseSensorActivity", "Dialog shown - starting initial scan")
        
        // Do initial scan
        startScanning(binding)
        
        // Update available devices
        deviceAdapter?.updateDevices(devices)
    }

    private fun stopScanning() {
        Log.d("BaseSensorActivity", "stopScanning called")
        isScanning = false
        scanHandler.removeCallbacks(stopScanRunnable)
        bluetoothService?.stopScanning()
        // Re-enable Scan button after scan ends
        deviceDialogBinding?.let { binding ->
            binding.buttonScan.isEnabled = true
            binding.progressScanning.visibility = View.GONE
        }
    }

    private fun startScanning(binding: DialogDeviceSelectionBinding) {
        if (serviceBinder == null) {
            Log.e("BaseSensorActivity", "Cannot scan - service binder is null")
            return
        }

        Log.d("BaseSensorActivity", "startScanning called")
        stopScanning() // Stop any existing scanning
        isScanning = true
        binding.progressScanning.visibility = View.VISIBLE
        binding.buttonScan.isEnabled = false
        bluetoothService?.startScanning()
        // Stop after 1 second (scan duration)
        scanHandler.postDelayed(stopScanRunnable, 1000)
    }

    private fun setupBluetoothReceiver() {
        Log.d("BaseSensorActivity", "setupBluetoothReceiver called. bluetoothReceiver is ${if (bluetoothReceiver == null) "null" else "already set"}")
        bluetoothReceiver = BluetoothDataReceiver(
            onDataReceived = { dataType, value ->
                Log.d("BaseSensorActivity", "Received data: type=$dataType, value=$value")
                when (dataType) {
                    BluetoothService.DATA_TYPE_HEART_RATE -> {
                        viewModel.updateHeartRate(value.toInt())
                        runOnUiThread {
                            updateUI()
                        }
                    }
                }
            },
            onConnectionStateChanged = { state ->
                Log.d("BaseSensorActivity", "Connection state changed: $state, current button text: ${buttonConnect.text}")
                currentConnectionState = state
                runOnUiThread {
                    when (state) {
                        BluetoothService.STATE_CONNECTED -> {
                            Log.d("BaseSensorActivity", "Device connected - stopping scan and dismissing dialog")
                            // Stop scanning and dismiss dialog if showing
                            stopScanning()
                            deviceSelectionDialog?.dismiss()
                            deviceSelectionDialog = null
                            
                            buttonConnect.text = "Disconnect"
                            Toast.makeText(this@BaseSensorActivity, "Connected to device", Toast.LENGTH_SHORT).show()
                            
                            // Start observing data
                            startObservingData()
                            updateUI()
                        }
                        BluetoothService.STATE_DISCONNECTED -> {
                            Log.d("BaseSensorActivity", "Device disconnected")
                            buttonConnect.text = "Connect to Sensor"
                            Toast.makeText(this@BaseSensorActivity, "Disconnected from device", Toast.LENGTH_SHORT).show()
                            
                            // Stop observing data and clear heart rate
                            dataJob?.cancel()
                            dataJob = null
                            viewModel.updateHeartRate(0)
                            updateUI()
                        }
                        BluetoothService.STATE_CONNECTING -> {
                            Log.d("BaseSensorActivity", "Device connecting")
                            buttonConnect.text = "Cancel"
                            updateUI()
                        }
                    }
                }
            },
            onDataBatchReceived = { dataType, values ->
                Log.d("BaseSensorActivity", "Received data batch: type=$dataType, values=${values.joinToString()}")
                when (dataType) {
                    BluetoothService.DATA_TYPE_HEART_RATE -> {
                        onHeartRateBatchReceived(values)
                        runOnUiThread {
                            updateUI()
                        }
                    }
                }
            }
        )
        
        val filter = IntentFilter().apply {
            addAction(BluetoothService.ACTION_DATA_AVAILABLE)
            addAction(BluetoothService.ACTION_CONNECTION_STATE_CHANGED)
        }
        registerReceiver(bluetoothReceiver, filter)
        Log.d("BaseSensorActivity", "Registered BluetoothDataReceiver")
    }

    override fun onResume() {
        super.onResume()
        Log.d("BaseSensorActivity", "onResume: Checking service state. dataJob is $dataJob")
        // Check service state when returning to activity
        checkServiceState()
    }

    override fun onPause() {
        super.onPause()
        Log.d("BaseSensorActivity", "onPause: Cancelling dataJob $dataJob")
        // Cancel data collection job to avoid duplicate observers
        dataJob?.cancel()
        dataJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BaseSensorActivity", "onDestroy")
        try {
            stopScanning()
            bluetoothReceiver?.let { receiver ->
                unregisterReceiver(receiver)
                bluetoothReceiver = null
                Log.d("BaseSensorActivity", "Unregistered BluetoothDataReceiver")
            }
            
            if (serviceBinder != null) {
                unbindService(serviceConnection)
                bluetoothService = null
                serviceBinder = null
                Log.d("BaseSensorActivity", "Unbound from Bluetooth service")
            }
        } catch (e: Exception) {
            Log.e("BaseSensorActivity", "Error during cleanup", e)
        }
    }

    abstract fun updateUI()

    protected fun getConnectionState(): Int {
        return currentConnectionState
    }

    // Allow subclasses to handle heart rate batch updates
    protected open fun onHeartRateBatchReceived(batch: IntArray) {}

    // Allow subclasses to react when bluetoothService is ready
    protected open fun onBluetoothServiceReady() {}
} 