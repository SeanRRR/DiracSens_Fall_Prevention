// HeartRateActivity.kt
package com.diracsens.android.fallprevention.activities.sensor

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.DashPathEffect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsetsController
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.diracsens.android.fallprevention.R
import com.diracsens.android.fallprevention.databinding.ActivityHeartRateBinding
import com.diracsens.android.fallprevention.models.HeartRateReading
import com.diracsens.android.fallprevention.services.BluetoothService
import com.diracsens.android.fallprevention.viewmodels.HealthMetricsViewModel
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.diracsens.android.fallprevention.DiracSensApplication

class HeartRateActivity : BaseSensorActivity<ActivityHeartRateBinding>() {
    private val dataPoints = mutableListOf<Int>()  // Store just the heart rate values
    private var lastUpdateTime = 0L
    private var xIndex = 0f  // Track the current x index, starts at 0 and increases
    private var currentDeviceName: String? = null
    private var isStopped = false
    private val prefs by lazy { getSharedPreferences("heart_rate_prefs", MODE_PRIVATE) }

    private val connectionStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothService.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothService.EXTRA_CONNECTION_STATE, BluetoothService.STATE_DISCONNECTED)
                    currentDeviceName = intent.getStringExtra(BluetoothService.EXTRA_DEVICE_NAME)
                    updateUI()
                }
            }
        }
    }

    companion object {
        private const val X_AXIS_ELEMENTS_COUNT = 100f // 5 seconds at 20 Hz
        private const val Y_AXIS_PADDING = 100  // Increased padding for piezo values
        private const val KEY_DATA_POINTS = "data_points"
        private const val KEY_LAST_UPDATE = "last_update"
        private const val KEY_X_INDEX = "x_index"
        private const val KEY_IS_STOPPED = "is_stopped"
        
        // Piezo sensor thresholds
        private const val PIEZO_THRESHOLD_HIGH = 800  // Adjust these values based on your sensor
        private const val PIEZO_THRESHOLD_LOW = 200
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("HeartRateActivity", "onCreate called")
        binding = ActivityHeartRateBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
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

        // Register for connection state broadcasts
        val filter = IntentFilter(BluetoothService.ACTION_CONNECTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(connectionStateReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(connectionStateReceiver, filter)
        }

        // Restore state if available
        savedInstanceState?.let { bundle ->
            lastUpdateTime = bundle.getLong(KEY_LAST_UPDATE, lastUpdateTime)
            xIndex = bundle.getFloat(KEY_X_INDEX, 0f)
            val savedPoints = bundle.getIntArray(KEY_DATA_POINTS)
            if (savedPoints != null) {
                dataPoints.clear()
                dataPoints.addAll(savedPoints.toList())
            }
            isStopped = bundle.getBoolean(KEY_IS_STOPPED, false)
        }

        // Restore persistent state
        isStopped = prefs.getBoolean("is_stopped", false)
        val lastHeartRate = prefs.getInt("last_heart_rate", -1)
        Log.d("HeartRateActivity", "Restored isStopped=$isStopped, lastHeartRate=$lastHeartRate")
        if (lastHeartRate != -1) {
            viewModel.updateHeartRate(lastHeartRate)
            Log.d("HeartRateActivity", "Set ViewModel currentHeartRate to $lastHeartRate")
        }
        // Explicitly update UI to reflect restored state
        updateUI()
        viewModel.currentHeartRate.value?.let { heartRate ->
            Log.d("HeartRateActivity", "Setting heart rate display to $heartRate in onCreate")
            if (getConnectionState() == BluetoothService.STATE_CONNECTED) {
                binding.textHeartRate.text = "${heartRate ?: 0}"
            } else {
                binding.textHeartRate.text = "--"
            }
        }

        setupToolbar()
        setupChart()
        setupExportButton()
        setupClearDataButton()
        setupStopButton()

        // Set initial button style and text
        if (isStopped) {
            binding.buttonStop.text = "Stopped"
            binding.buttonStop.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            binding.buttonStop.strokeColor = ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
        } else {
            binding.buttonStop.text = "Running"
            binding.buttonStop.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            binding.buttonStop.strokeColor = ContextCompat.getColorStateList(this, android.R.color.holo_green_dark)
        }

        // Load historical data first
        viewModel.recentHeartRate.observe(this) { readings ->
            if (dataPoints.isEmpty() && readings.isNotEmpty()) {
                // Only load historical data if we don't have any current data points
                dataPoints.clear()
                dataPoints.addAll(readings.map { it.heartRate })
                xIndex = dataPoints.size - 1f
                updateChart()
            }
        }

        // Observe current heart rate
        viewModel.currentHeartRate.observe(this) { heartRate ->
            Log.d("HeartRateActivity", "Received new piezo value: $heartRate")
            if (isStopped) return@observe
            if (getConnectionState() == BluetoothService.STATE_CONNECTED) {
                binding.textHeartRate.text = "${heartRate ?: 0}"
                if (heartRate != null) {
                    val status = when {
                        heartRate > PIEZO_THRESHOLD_HIGH -> "High Activity"
                        heartRate < PIEZO_THRESHOLD_LOW -> "Low Activity"
                        else -> "Normal Activity"
                    }
                    binding.textStatus.text = status
                    val statusColorRes = when(status) { 
                        "High Activity" -> R.color.status_high
                        "Low Activity" -> R.color.status_low
                        else -> R.color.status_normal
                    }
                    binding.statusDot.background.setTint(ContextCompat.getColor(this, statusColorRes))
                }
                val currentTime = System.currentTimeMillis()
                val timeSinceLastUpdate = currentTime - lastUpdateTime
                if (timeSinceLastUpdate >= 100) {
                    lastUpdateTime = currentTime
                    dataPoints.add(heartRate ?: 0)
                    while (dataPoints.size > X_AXIS_ELEMENTS_COUNT) {
                        dataPoints.removeAt(0)
                    }
                    xIndex = dataPoints.size - 1f
                    updateChart()
                }
            }
        }

        // Listen for batch updates and add all samples to the chart at once
        if (intent.hasExtra("BATCH_HEART_RATE")) {
            val batch = intent.getIntArrayExtra("BATCH_HEART_RATE")
            if (batch != null) {
                addBatchToChart(batch)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(connectionStateReceiver)
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_LAST_UPDATE, lastUpdateTime)
        outState.putFloat(KEY_X_INDEX, xIndex)
        outState.putIntArray(KEY_DATA_POINTS, dataPoints.toIntArray())
        outState.putBoolean(KEY_IS_STOPPED, isStopped)
    }

    private fun setupChart() {
        binding.chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)

            // Set colors based on theme
            val isDarkTheme = resources.configuration.uiMode and 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK == 
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            
            setBackgroundColor(Color.TRANSPARENT)
            
            if (isDarkTheme) {
                xAxis.gridColor = Color.argb(128, 255, 255, 255)
                xAxis.textColor = Color.WHITE
                axisLeft.gridColor = Color.argb(128, 255, 255, 255)
                axisLeft.textColor = Color.WHITE
            } else {
                xAxis.gridColor = Color.argb(128, 0, 0, 0)
                xAxis.textColor = Color.BLACK
                axisLeft.gridColor = Color.argb(128, 0, 0, 0)
                axisLeft.textColor = Color.BLACK
            }

            xAxis.apply {
                setDrawGridLines(false)
                setAvoidFirstLastClipping(true)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
                axisMaximum = 1023f
                granularity = 50f
                labelCount = 11
                setDrawAxisLine(true)
                setDrawGridLines(true)
                enableGridDashedLine(5f, 5f, 0f)
                
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.toInt().toString()
                    }
                }
            }

            axisRight.isEnabled = false

            // Initialize with empty data
            data = LineData().apply {
                addDataSet(LineDataSet(mutableListOf(), "Heart Rate").apply {
                    setDrawIcons(false)
                    setDrawValues(false)
                    
                    if (isDarkTheme) {
                        color = Color.WHITE
                        setCircleColor(Color.WHITE)
                    } else {
                        color = Color.BLACK
                        setCircleColor(Color.BLACK)
                    }

                    lineWidth = 2f
                    circleRadius = 3f
                    setDrawCircles(false)
                    setDrawCircleHole(false)
                    mode = LineDataSet.Mode.LINEAR
                    setDrawFilled(false)
                })
            }

            // Set zoom constraints
            setScaleXEnabled(true)
            setScaleYEnabled(true)
            setVisibleYRange(100f, 1023f, YAxis.AxisDependency.LEFT)
            
            // Enable auto scaling
            isAutoScaleMinMaxEnabled = true
            
            invalidate()
        }
    }

    private fun updateChart() {
        binding.chart.data?.let { lineData ->
            lineData.getDataSetByIndex(0)?.let { dataSet ->
                // Create entries with time in seconds as x value
                val entries = dataPoints.mapIndexed { i, v ->
                    Entry(i.toFloat() / 20f, v.toFloat()) // 20 Hz sampling rate
                }

                // Update dataset
                dataSet.clear()
                entries.forEach { entry ->
                    dataSet.addEntry(entry)
                }

                // Sliding window for x-axis: show last X_AXIS_ELEMENTS_COUNT samples (in seconds)
                val totalPoints = dataPoints.size
                val windowSize = X_AXIS_ELEMENTS_COUNT
                val secondsPerSample = 1f / 20f
                val windowSeconds = windowSize * secondsPerSample
                val totalSeconds = totalPoints * secondsPerSample
                val axisMin: Float
                val axisMax: Float
                if (totalPoints < windowSize) {
                    axisMin = 0f
                    axisMax = totalSeconds.coerceAtLeast(1f)
                } else {
                    axisMin = totalSeconds - windowSeconds
                    axisMax = totalSeconds
                }
                binding.chart.xAxis.apply {
                    this.axisMinimum = axisMin
                    this.axisMaximum = axisMax
                }

                // Set x-axis to display seconds as integers
                binding.chart.xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.toInt().toString()
                    }
                }
                binding.chart.xAxis.granularity = 1.0f

                // Auto-scale Y axis to current data
                if (dataPoints.isNotEmpty()) {
                    val minY = dataPoints.minOrNull() ?: 0
                    val maxY = dataPoints.maxOrNull() ?: 1023
                    val padding = ((maxY - minY) * 0.1f).toInt().coerceAtLeast(5)
                    var axisMin = (minY - padding).toFloat().coerceAtLeast(0f)
                    var axisMax = (maxY + padding).toFloat().coerceAtMost(1023f)
                    val minRange = 5f
                    if (axisMax - axisMin < minRange) {
                        val center = (axisMax + axisMin) / 2f
                        axisMin = (center - minRange / 2f).coerceAtLeast(0f)
                        axisMax = (center + minRange / 2f).coerceAtMost(1023f)
                    }
                    // Ensure axisMax is at least 10
                    if (axisMax < 10f) axisMax = 10f
                    binding.chart.axisLeft.axisMinimum = axisMin
                    binding.chart.axisLeft.axisMaximum = axisMax
                    // Adjust granularity and label count for small ranges
                    val range = axisMax - axisMin
                    binding.chart.axisLeft.granularity = when {
                        range <= 100f -> 10f
                        range <= 200f -> 20f
                        else -> 50f
                    }
                    binding.chart.axisLeft.labelCount = when {
                        range <= 100f -> 6
                        range <= 200f -> 8
                        else -> 10
                    }
                }

                lineData.notifyDataChanged()
                binding.chart.notifyDataSetChanged()
                binding.chart.invalidate()
            }
        }
    }

    override fun updateUI() {
        val connectionState = getConnectionState()
        Log.d("HeartRateActivity", "updateUI: connectionState=$connectionState, isStopped=$isStopped")
        // Update device name text based on connection state only
        binding.deviceNameText.text = when (connectionState) {
            BluetoothService.STATE_CONNECTED -> "PiezoSensor"  // Hardcoded device name
            BluetoothService.STATE_CONNECTING -> "Connecting..."
            else -> "No Device Connected"
        }

        // Update connect button text
        binding.buttonConnect.text = when (connectionState) {
            BluetoothService.STATE_CONNECTED -> "Disconnect"
            BluetoothService.STATE_CONNECTING -> "Cancel"
            else -> "Connect to Sensor"
        }

        // Always show last known heart rate and running/stopped state
        val heartRate = viewModel.currentHeartRate.value ?: prefs.getInt("last_heart_rate", 0)
        binding.textHeartRate.text = "$heartRate"
        if (connectionState == BluetoothService.STATE_CONNECTED) {
            binding.textStatus.text = if (isStopped) "Stopped" else "Running"
        } else {
            binding.textStatus.text = "Disconnected (showing last known data)"
        }
        // Update stopped/running button
        if (connectionState == BluetoothService.STATE_CONNECTED) {
            binding.buttonStop.isEnabled = true
            if (isStopped) {
                binding.buttonStop.text = "Stopped"
                binding.buttonStop.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                binding.buttonStop.strokeColor = ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
            } else {
                binding.buttonStop.text = "Running"
                binding.buttonStop.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                binding.buttonStop.strokeColor = ContextCompat.getColorStateList(this, android.R.color.holo_green_dark)
            }
        } else {
            binding.buttonStop.isEnabled = false
            binding.buttonStop.text = "Not Connected"
            binding.buttonStop.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            binding.buttonStop.strokeColor = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Heart Rate"
    }

    private fun setupExportButton() {
        binding.buttonExport.setOnClickListener {
            // Check if we have any data to export
            if (dataPoints.isEmpty()) {
                Toast.makeText(
                    this,
                    "No data available to export",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Show progress dialog
            val progressDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Exporting Data")
                .setMessage("Please wait while we export your heart rate data...")
                .setCancelable(false)
                .create()
            progressDialog.show()

            viewModel.exportData(BluetoothService.DATA_TYPE_HEART_RATE) { uri ->
                progressDialog.dismiss()
                
                if (uri != null) {
                    // Show success message with file location
                    val successDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Export Successful")
                        .setMessage("Heart rate data has been exported to your Downloads folder. Would you like to open the file?")
                        .setPositiveButton("Open") { _, _ ->
                            try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "text/csv")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(
                            this,
                            "No app found to open CSV files",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                        }
                        .setNegativeButton("Close", null)
                        .show()
                } else {
                    // Show error dialog
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Export Failed")
                        .setMessage("Failed to export heart rate data. Please check storage permissions and try again.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    private fun setupClearDataButton() {
        binding.buttonClearData.setOnClickListener {
            // Show confirmation dialog
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Clear Heart Rate Data")
                .setMessage("Are you sure you want to delete all heart rate data? This action cannot be undone.")
                .setPositiveButton("Clear") { _, _ ->
                    viewModel.deleteAllHeartRateReadings()
                    resetChart(clearData = true)  // Pass true to indicate we're clearing data
                    Toast.makeText(this, "Heart rate data cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupStopButton() {
        binding.buttonStop?.setOnClickListener {
            isStopped = !isStopped
            // Persist stopped state
            prefs.edit().putBoolean("is_stopped", isStopped).apply()
            if (isStopped) {
                binding.buttonStop.text = "Stopped"
                binding.buttonStop.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                binding.buttonStop.strokeColor = ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
                Toast.makeText(this, "Data collection stopped", Toast.LENGTH_SHORT).show()
            } else {
                binding.buttonStop.text = "Running"
                binding.buttonStop.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                binding.buttonStop.strokeColor = ContextCompat.getColorStateList(this, android.R.color.holo_green_dark)
                Toast.makeText(this, "Data collection resumed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetChart(clearData: Boolean = false) {
        // Clear data points
        dataPoints.clear()
        lastUpdateTime = 0L
        xIndex = 0f
        
        // Only load historical data if we're not explicitly clearing data
        if (!clearData) {
            viewModel.recentHeartRate.value?.let { readings ->
                if (readings.isNotEmpty()) {
                    dataPoints.addAll(readings.map { it.heartRate })
                    xIndex = dataPoints.size - 1f
                }
            }
        }
        
        updateChart()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
        }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Add this method to handle batch updates
    private fun addBatchToChart(batch: IntArray) {
        if (isStopped) return
        if (getConnectionState() == BluetoothService.STATE_CONNECTED) {
            batch.forEach { heartRate ->
                dataPoints.add(heartRate)
                while (dataPoints.size > X_AXIS_ELEMENTS_COUNT) {
                    dataPoints.removeAt(0)
                }
                xIndex = dataPoints.size - 1f
            }
            // Update current heart rate with the average of the batch
            if (batch.isNotEmpty()) {
                val avg = batch.average().toInt()
                viewModel.updateHeartRate(avg)
                // Persist last heart rate
                prefs.edit().putInt("last_heart_rate", avg).apply()
            }
            updateChart()
        }
    }

    override fun onHeartRateBatchReceived(batch: IntArray) {
        addBatchToChart(batch)
    }

    override fun onBluetoothServiceReady() {
        super.onBluetoothServiceReady()
        // Only auto-reconnect if not the first app launch
        val lastDeviceAddress = prefs.getString("last_device_address", null)
        if (DiracSensApplication.hasLaunchedOnce) {
            if (getConnectionState() != BluetoothService.STATE_CONNECTED && lastDeviceAddress != null) {
                bluetoothService?.autoReconnect()
                Log.d("HeartRateActivity", "Attempting auto-reconnect to $lastDeviceAddress (not first launch)")
            }
        } else {
            Log.d("HeartRateActivity", "First app launch, skipping auto-reconnect")
        }
        DiracSensApplication.hasLaunchedOnce = true
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        // If connected and chart is empty, reload recent data
        if (getConnectionState() == BluetoothService.STATE_CONNECTED && dataPoints.isEmpty()) {
            viewModel.recentHeartRate.value?.let { readings ->
                if (readings.isNotEmpty()) {
                    dataPoints.clear()
                    dataPoints.addAll(readings.map { it.heartRate })
                    xIndex = dataPoints.size - 1f
                    updateChart()
                }
            }
        }
        // Always show last known heart rate and running/stopped state
        val heartRate = viewModel.currentHeartRate.value ?: prefs.getInt("last_heart_rate", 0)
        binding.textHeartRate.text = "$heartRate"
        // Removed auto-reconnect logic from here
    }
}