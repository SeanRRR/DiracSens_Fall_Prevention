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
import android.content.pm.PackageManager
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
    private val viewModel: HealthMetricsViewModel by viewModels()

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

        // Request permissions when app starts
        requestPermissions()

        // Set up UI components
        setupFeatureGrid()

        // Set up Bottom Navigation
        binding.bottomNavigation.setOnNavigationItemSelectedListener {
            when(it.itemId) {
                R.id.nav_home -> {
                    Log.d("MainActivity", "Home selected")
                    true
                }
                R.id.nav_education -> {
                    Log.d("MainActivity", "Education selected")
                    true
                }
                R.id.nav_help -> {
                    Log.d("MainActivity", "Help selected")
                    true
                }
                R.id.nav_settings -> {
                    Log.d("MainActivity", "Settings selected")
                    true
                }
                else -> false
            }
        }
    }

    private fun requestPermissions() {
        Log.d("MainActivity", "Starting permission request process")
        val permissionsToRequest = mutableListOf<String>()

        // Bluetooth permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d("MainActivity", "Device is Android 12+ (API ${Build.VERSION.SDK_INT})")
            // Check if permissions are already granted
            val hasConnect = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            val hasScan = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            Log.d("MainActivity", "Current permission status - BLUETOOTH_CONNECT: $hasConnect, BLUETOOTH_SCAN: $hasScan")
            
            if (!hasConnect || !hasScan) {
                Log.d("MainActivity", "Requesting Android 12+ Bluetooth permissions")
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                Log.d("MainActivity", "Bluetooth permissions already granted")
            }
        } else {
            Log.d("MainActivity", "Device is Android ${Build.VERSION.SDK_INT}, using legacy permissions")
        }

        // Location permissions (required for BLE scanning)
        val hasLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        Log.d("MainActivity", "Current location permission status: $hasLocation")
        if (!hasLocation) {
            Log.d("MainActivity", "Requesting location permission")
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            Log.d("MainActivity", "Location permission already granted")
        }

        // Storage permissions
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            Log.d("MainActivity", "Device is Android 9 or lower, requesting legacy storage permissions")
            val hasWrite = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            val hasRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            Log.d("MainActivity", "Current storage permission status - WRITE: $hasWrite, READ: $hasRead")
            
            if (!hasWrite || !hasRead) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            Log.d("MainActivity", "Device is Android 11, requesting READ_EXTERNAL_STORAGE")
            val hasRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            Log.d("MainActivity", "Current READ_EXTERNAL_STORAGE permission status: $hasRead")
            
            if (!hasRead) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            Log.d("MainActivity", "Device is Android 13+, requesting READ_MEDIA_IMAGES")
            val hasMedia = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            Log.d("MainActivity", "Current READ_MEDIA_IMAGES permission status: $hasMedia")
            
            if (!hasMedia) {
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }

        if (permissionsToRequest.isEmpty()) {
            Log.d("MainActivity", "All permissions already granted, no need to request")
        } else {
            Log.d("MainActivity", "Requesting permissions: ${permissionsToRequest.joinToString()}")
        requestMultiplePermissions.launch(permissionsToRequest.toTypedArray())
        }
    }

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        Log.d("MainActivity", "Permission request results:")
        permissions.entries.forEach { (permission, granted) ->
            Log.d("MainActivity", "  $permission = $granted")
        }
        if (allGranted) {
            Log.d("MainActivity", "All permissions granted, starting Bluetooth service")
            startBluetoothService()
        } else {
            Log.d("MainActivity", "Some permissions were denied")
            // Check which permissions were denied
            permissions.entries.filter { !it.value }.forEach { (permission, _) ->
                Log.d("MainActivity", "Denied permission: $permission")
                // Check if we should show rationale
                if (shouldShowRequestPermissionRationale(permission)) {
                    Log.d("MainActivity", "Should show rationale for $permission")
                } else {
                    Log.d("MainActivity", "Permission $permission permanently denied")
                }
            }
            Toast.makeText(this, "Permissions required for app functionality", Toast.LENGTH_LONG).show()
        }
    }

    private fun startBluetoothService() {
        val serviceIntent = Intent(this, BluetoothService::class.java)
        startService(serviceIntent)
    }

    private fun setupFeatureGrid() {
        val features = listOf(
            FeatureCard(R.drawable.body_balance_icon, "Body Balance", BodyBalanceActivity::class.java),
            FeatureCard(R.drawable.blood_pressure_icon, "Blood Pressure", BloodPressureActivity::class.java),
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

    override fun onDestroy() {
        super.onDestroy()
    }
}