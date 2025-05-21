package com.diracsens.android.fallprevention.activities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
import android.view.WindowInsetsController
import androidx.core.content.ContextCompat
import android.util.TypedValue
//import com.diracsens.android.fallprevention.activities.sensor.BloodPressureActivity
//import com.diracsens.android.fallprevention.activities.sensor.BodyBalanceActivity
//import com.diracsens.android.fallprevention.activities.sensor.BreathingRateActivity
//import com.diracsens.android.fallprevention.activities.sensor.GaitAnalysisActivity
//import com.diracsens.android.fallprevention.activities.sensor.HeartRateActivity
//import com.diracsens.android.fallprevention.activities.medication.MedicationActivity
//import com.diracsens.android.fallprevention.activities.survey.SurveyActivity
import com.diracsens.android.fallprevention.databinding.ActivityMainBinding
import com.diracsens.android.fallprevention.services.BluetoothService
import com.diracsens.android.fallprevention.viewmodels.HealthMetricsViewModel
import android.util.Log
import com.diracsens.android.fallprevention.R
import com.diracsens.android.fallprevention.activities.medication.MedicationActivity
import com.diracsens.android.fallprevention.activities.sensor.BaselineActivity
import com.diracsens.android.fallprevention.activities.sensor.BloodPressureActivity
import com.diracsens.android.fallprevention.activities.sensor.BodyBalanceActivity
import com.diracsens.android.fallprevention.activities.sensor.GaitAnalysisActivity
import com.diracsens.android.fallprevention.activities.sensor.HeartRateActivity
import com.diracsens.android.fallprevention.activities.sensor.RespiratoryRateActivity
import com.diracsens.android.fallprevention.activities.survey.SurveyActivity

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

            // Set the height of the card to match its width to make it square
            holder.itemView.post {
                val width = holder.itemView.width
                val layoutParams = holder.itemView.layoutParams
                if (layoutParams != null && width > 0) {
                    layoutParams.height = width
                    holder.itemView.layoutParams = layoutParams

                    // Calculate and set text size based on width
                    val textSize = width * 0.1f // Adjust the factor as needed
                    holder.label?.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                }
            }

        } else {
             Log.e(TAG, "Attempting to bind position $position but features list size is ${features.size}")
        }
    }
    override fun getItemCount(): Int {
        return features.size
    }
}

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: com.diracsens.android.fallprevention.viewmodels.HealthMetricsViewModel by viewModels()

    private val SCAN_PERIOD: Long = 10000
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanning = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar and navigation bar colors
        window.statusBarColor = ContextCompat.getColor(this, R.color.light_background)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.light_background)

        // Set status bar icons to dark for light background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
             window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        // For navigation bar icons before R, it's more complex and theme-dependent,
        // often handled via themes or specific view flags which are less direct.
        // We'll focus on the status bar for broader compatibility with light icons.

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

        // Set up Bottom Navigation
        binding.bottomNavigation.setOnNavigationItemSelectedListener {
            when(it.itemId) {
                R.id.nav_home -> {
                    // Handle Home selection
                    Log.d("MainActivity", "Home selected")
                    true
                }
                R.id.nav_education -> {
                    // Handle Education selection
                    Log.d("MainActivity", "Education selected")
                    true
                }
                R.id.nav_help -> {
                    // Handle Help selection
                    Log.d("MainActivity", "Help selected")
                    true
                }
                R.id.nav_settings -> {
                    // Handle Settings selection
                    Log.d("MainActivity", "Settings selected")
                    true
                }
                else -> false
            }
        }
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
            FeatureCard(R.drawable.body_balance_icon, "Body Balance", BodyBalanceActivity::class.java),
            FeatureCard(R.drawable.blood_pressure_icon, "Blood Pressure", com.diracsens.android.fallprevention.activities.sensor.BloodPressureActivity::class.java),
            FeatureCard(R.drawable.gait_icon, "Gait Analysis", GaitAnalysisActivity::class.java),
            FeatureCard(R.drawable.heart_rate_icon, "Heart Rate", HeartRateActivity::class.java),
            FeatureCard(R.drawable.breathing_rate_icon, "Respiratory Rate", RespiratoryRateActivity::class.java),
            FeatureCard(R.drawable.survey_icon, "Survey", SurveyActivity::class.java),
            FeatureCard(R.drawable.medication_icon, "Medication", MedicationActivity::class.java),
            FeatureCard(R.drawable.baseline_icon, "Baseline", BaselineActivity::class.java)
        )
        val recyclerView = binding.featureGrid

        // Set spanCount based on orientation (2 for portrait, 4 for landscape)
        val orientation = resources.configuration.orientation
        val spanCount = if (orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
            2
        } else {
            4
        }

        recyclerView.layoutManager = GridLayoutManager(this, spanCount)
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