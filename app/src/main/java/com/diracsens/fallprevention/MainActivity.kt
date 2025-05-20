package com.diracsens.fallprevention

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewTreeObserver
//import com.diracsens.fallprevention.activities.sensor.BloodPressureActivity
//import com.diracsens.fallprevention.activities.BodyBalanceActivity
//import com.diracsens.fallprevention.activities.sensor.BreathingRateActivity
//import com.diracsens.fallprevention.activities.GaitAnalysisActivity
//import com.diracsens.fallprevention.activities.sensor.HeartRateActivity
//import com.diracsens.fallprevention.activities.MedicationActivity
//import com.diracsens.fallprevention.activities.SurveyActivity
import com.diracsens.fallprevention.databinding.ActivityMainBinding
import com.diracsens.fallprevention.services.BluetoothService
import com.diracsens.fallprevention.viewmodels.HealthMetricsViewModel
import android.util.Log

// Data class for feature cards
data class FeatureCard(val iconRes: Int, val label: String, val activityClass: Class<*>)

class FeatureCardAdapter(
    private val features: List<FeatureCard>,
    private val recyclerView: RecyclerView,
    private val onClick: (FeatureCard) -> Unit
) : RecyclerView.Adapter<FeatureCardAdapter.FeatureViewHolder>() {
    private val TAG = "FeatureCardAdapter"

    inner class FeatureViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView? = view.findViewById(R.id.feature_icon)
        val label: TextView? = view.findViewById(R.id.feature_label)
        init {
            if (icon == null) {
                Log.e(TAG, "ImageView with id feature_icon not found!")
            }
            if (label == null) {
                Log.e(TAG, "TextView with id feature_label not found!")
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feature_card, parent, false)
        return FeatureViewHolder(view)
    }
    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        Log.d(TAG, "Binding item at position: $position")
        if (position < features.size) {
            val feature = features[position]
            Log.d(TAG, "Feature data: iconRes=${feature.iconRes}, label=${feature.label}")

            holder.icon?.setImageResource(feature.iconRes)
            holder.label?.text = feature.label
            holder.itemView.setOnClickListener { onClick(feature) }

            // Re-add logic to set the height of the card to match its width after layout
            holder.itemView.post {
                val width = holder.itemView.width
                Log.d(TAG, "Item view width after layout: $width")
                val layoutParams = holder.itemView.layoutParams
                if (layoutParams != null && width > 0) {
                    layoutParams.height = width
                    holder.itemView.layoutParams = layoutParams
                    holder.itemView.requestLayout()
                    Log.d(TAG, "Item height set to: $width")
                } else if (layoutParams == null) {
                    Log.e(TAG, "Layout params are null for position $position")
                } else {
                     Log.d(TAG, "Item view width is 0 for position $position")
                }
            }
        } else {
             Log.e(TAG, "Attempting to bind position $position but features list size is ${features.size}")
        }
    }
    override fun getItemCount() = features.size
}

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: HealthMetricsViewModel by viewModels()

    private val SCAN_PERIOD: Long = 10000
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanning = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Bluetooth adapter
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

        // Check if Bluetooth is supported
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Request necessary permissions
        requestPermissions()

        // Set up UI components
        setupFeatureGrid()
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Bluetooth permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        // Location permissions (required for BLE scanning)
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)

        // Storage permissions
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
        }

        requestMultiplePermissions.launch(permissionsToRequest.toTypedArray())
    }

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Start Bluetooth service
            //startBluetoothService()
        } else {
            Toast.makeText(this, "Permissions required for app functionality", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupFeatureGrid() {
        val features = listOf(
//            FeatureCard(R.drawable.body_balance_icon, "Body Balance", com.diracsens.fallprevention.activities.BodyBalanceActivity::class.java),
            FeatureCard(R.drawable.blood_pressure_icon, "Blood Pressure", com.diracsens.fallprevention.activities.sensor.BloodPressureActivity::class.java),
//            FeatureCard(R.drawable.gait_icon, "Gait Analysis", com.diracsens.fallprevention.activities.GaitAnalysisActivity::class.java),
            FeatureCard(R.drawable.heart_rate_icon, "Heart Rate", com.diracsens.fallprevention.activities.sensor.HeartRateActivity::class.java),
//            FeatureCard(R.drawable.breathing_rate_icon, "Respiratory Rate", com.diracsens.fallprevention.activities.sensor.RespiratoryRateActivity::class.java),
//            FeatureCard(R.drawable.survey_icon, "Survey", com.diracsens.fallprevention.activities.SurveyActivity::class.java),
//            FeatureCard(R.drawable.medication_icon, "Medication", com.diracsens.fallprevention.activities.MedicationActivity::class.java),
//            FeatureCard(R.drawable.baseline_icon, "Baseline", com.diracsens.fallprevention.activities.BaselineActivity::class.java)
        )
        val recyclerView = binding.featureGrid
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = FeatureCardAdapter(features, recyclerView) { feature ->
            startActivity(Intent(this, feature.activityClass))
        }
    }

//    private fun startBluetoothService() {
//        val serviceIntent = Intent(this, BluetoothService::class.java)
//        startService(serviceIntent)
//
//        // Register broadcast receiver for data updates
//        val filter = IntentFilter().apply {
//            addAction(BluetoothService.ACTION_DATA_AVAILABLE)
//        }
//        registerReceiver(dataUpdateReceiver, filter)
//
//        // Start scanning for devices
//        scanLeDevice()
//    }

    private val dataUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothService.ACTION_DATA_AVAILABLE -> {
                    val dataType = intent.getStringExtra(BluetoothService.EXTRA_DATA_TYPE)
                    when (dataType) {
                        BluetoothService.DATA_TYPE_BLOOD_PRESSURE -> {
                            val systolic = intent.getIntExtra(BluetoothService.EXTRA_SYSTOLIC, 0)
                            val diastolic = intent.getIntExtra(BluetoothService.EXTRA_DIASTOLIC, 0)
                            viewModel.updateBloodPressure(systolic, diastolic)
                        }
                        BluetoothService.DATA_TYPE_HEART_RATE -> {
                            val heartRate = intent.getIntExtra(BluetoothService.EXTRA_HEART_RATE, 0)
                            viewModel.updateHeartRate(heartRate)
                        }
                        BluetoothService.DATA_TYPE_BREATHING_RATE -> {
                            val breathingRate = intent.getIntExtra(BluetoothService.EXTRA_BREATHING_RATE, 0)
                            viewModel.updateBreathingRate(breathingRate)
                        }
                        // Handle other data types
                    }
                }
            }
        }
    }

//    private fun scanLeDevice() {
//        if (!scanning) {
//            handler.postDelayed({
//                scanning = false
//                bluetoothLeScanner?.stopScan(leScanCallback)
//            }, SCAN_PERIOD)
//
//            scanning = true
//            bluetoothLeScanner?.startScan(leScanCallback)
//            Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show()
//        } else {
//            scanning = false
//            bluetoothLeScanner?.stopScan(leScanCallback)
//        }
//    }

//    private val leScanCallback = object : ScanCallback() {
//        override fun onScanResult(callbackType: Int, result: ScanResult) {
//            super.onScanResult(callbackType, result)
//            val device = result.device
//            // Check if this is an Arduino Feather device
//            if (isArduinoFeatherDevice(device)) {
//                Toast.makeText(this@MainActivity, "Found DiracSens device: ${device.name}", Toast.LENGTH_SHORT).show()
//                connectToDevice(device.address)
//            }
//        }
//    }
//
//    private fun isArduinoFeatherDevice(device: android.bluetooth.BluetoothDevice): Boolean {
//        // Check device name or address to identify Arduino Feather boards
//        val deviceName = device.name ?: return false
//        return deviceName.contains("Feather", ignoreCase = true) ||
//                deviceName.contains("DiracSens", ignoreCase = true)
//    }
//
//    private fun connectToDevice(address: String) {
//        val intent = Intent(this, BluetoothService::class.java).apply {
//            action = BluetoothService.ACTION_CONNECT
//            putExtra(BluetoothService.EXTRA_DEVICE_ADDRESS, address)
//        }
//        startService(intent)
//    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(dataUpdateReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }
}