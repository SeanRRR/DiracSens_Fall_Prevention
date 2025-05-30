// HeartRateActivity.kt
package com.diracsens.android.fallprevention.activities.sensor

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.DashPathEffect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HeartRateActivity : BaseSensorActivity<ActivityHeartRateBinding>() {
    private val dataPoints = mutableListOf<Int>()  // Store just the heart rate values
    private var lastUpdateTime = 0L
    private var xIndex = 0f  // Track the current x index, starts at 0 and increases

    companion object {
        private const val X_AXIS_ELEMENTS_COUNT = 40f
        private const val Y_AXIS_PADDING = 10  // Add some padding to y-axis range
        private const val KEY_DATA_POINTS = "data_points"
        private const val KEY_LAST_UPDATE = "last_update"
        private const val KEY_X_INDEX = "x_index"
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityHeartRateBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Restore state if available
        savedInstanceState?.let { bundle ->
            lastUpdateTime = bundle.getLong(KEY_LAST_UPDATE, lastUpdateTime)
            xIndex = bundle.getFloat(KEY_X_INDEX, 0f)
            val savedPoints = bundle.getIntArray(KEY_DATA_POINTS)
            if (savedPoints != null) {
                dataPoints.clear()
                dataPoints.addAll(savedPoints.toList())
            }
        }

        setupToolbar()
        setupChart()
        setupGraphControls()
        setupExportButton()
        setupClearDataButton()

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
            Log.d("HeartRateActivity", "Received new heart rate: $heartRate")
            
            // Handle null case
            if (heartRate == null) {
                binding.textHeartRate.text = "--"
                binding.textStatus.text = "Disconnected"
                binding.textStatus.setTextColor(Color.GRAY)
                return@observe
            }

            binding.textHeartRate.text = "$heartRate"

            // Update status based on heart rate values
            val status = when {
                heartRate > 100 -> "High"
                heartRate < 60 -> "Low"
                else -> "Normal"
            }
            binding.textStatus.text = status

            // Set status color
            val statusColor = when(status) {
                "High" -> Color.RED
                "Low" -> Color.BLUE
                else -> Color.GREEN
            }
            binding.textStatus.setTextColor(statusColor)

            // Update chart with new heart rate
            val currentTime = System.currentTimeMillis()
            val timeSinceLastUpdate = currentTime - lastUpdateTime
            
            // Only add new data point if it's been at least 1 second since last update
            if (timeSinceLastUpdate >= 1000) {
                lastUpdateTime = currentTime
                dataPoints.add(heartRate)
                
                // Keep only last X_AXIS_ELEMENTS_COUNT points
                while (dataPoints.size > X_AXIS_ELEMENTS_COUNT) {
                    dataPoints.removeAt(0)
                }
                
                // Update xIndex after adding point
                xIndex = dataPoints.size - 1f
                updateChart()
            }
        }

        // Update connection status text based on service state
        binding.connectionStatusText.text = when (getConnectionState()) {
            BluetoothService.STATE_CONNECTED -> "Connected"
            BluetoothService.STATE_DISCONNECTED -> "Not Connected"
            BluetoothService.STATE_CONNECTING -> "Connecting..."
            else -> "Not Connected"
        }

        // Update connect button text
        binding.buttonConnect.text = when (getConnectionState()) {
            BluetoothService.STATE_CONNECTED -> "Disconnect"
            BluetoothService.STATE_CONNECTING -> "Cancel"
            else -> "Connect to Sensor"
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_LAST_UPDATE, lastUpdateTime)
        outState.putFloat(KEY_X_INDEX, xIndex)
        outState.putIntArray(KEY_DATA_POINTS, dataPoints.toIntArray())
    }

    private fun setupChart() {
        binding.chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)  // Enable touch interaction
            setDrawGridBackground(false)
            isDragEnabled = true
            setScaleEnabled(true)  // Enable scaling
            setPinchZoom(true)     // Enable pinch-to-zoom

            // Set colors based on theme
            val isDarkTheme = resources.configuration.uiMode and 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK == 
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            
            if (isDarkTheme) {
                setBackgroundColor(Color.TRANSPARENT)
                xAxis.gridColor = Color.WHITE
                xAxis.textColor = Color.WHITE
                axisLeft.gridColor = Color.WHITE
                axisLeft.textColor = Color.WHITE
            } else {
                setBackgroundColor(Color.WHITE)
                xAxis.gridColor = Color.BLACK
                xAxis.textColor = Color.BLACK
                axisLeft.gridColor = Color.BLACK
                axisLeft.textColor = Color.BLACK
            }

            xAxis.apply {
                setDrawGridLines(true)
                axisMinimum = 0f  // Always start from 0
                axisMaximum = X_AXIS_ELEMENTS_COUNT  // Initial maximum
                setAvoidFirstLastClipping(true)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f  // Set minimum zoom level
            }

            axisLeft.apply {
                setDrawGridLines(true)
                // Initial y-axis range will be set in updateChart
                axisMinimum = 0f
                axisMaximum = 100f
                granularity = 1f  // Set minimum zoom level
            }

            axisRight.isEnabled = false

            // Initialize with empty data
            data = LineData().apply {
                addDataSet(LineDataSet(mutableListOf(), "Heart Rate").apply {
                    setDrawIcons(false)
                    setDrawValues(false)
                    
                    // Set colors based on theme
                    if (isDarkTheme) {
                        color = Color.WHITE
                        setCircleColor(Color.WHITE)
                    } else {
                        color = Color.BLACK
                        setCircleColor(Color.BLACK)
                    }

                    lineWidth = 2f
                    circleRadius = 3f  // Show small circles at data points
                    setDrawCircles(true)  // Draw circles
                    mode = LineDataSet.Mode.LINEAR  // Straight lines between points
                    setDrawFilled(false)  // No fill under the line
                })
            }

            // Set zoom constraints
            setScaleXEnabled(true)
            setScaleYEnabled(true)
            setVisibleXRange(5f, 40f)  // Show between 5 and 40 points on X axis
            setVisibleYRange(20f, 100f, YAxis.AxisDependency.LEFT)  // Show between 20 and 100 on Y axis

            invalidate()
        }
    }

    private fun updateChart() {
        binding.chart.data?.let { lineData ->
            lineData.getDataSetByIndex(0)?.let { dataSet ->
                // Create entries with proper x indices
                val entries = dataPoints.mapIndexed { i, v ->
                    // Calculate x position relative to the current window, starting from 0
                    val xPos = i.toFloat()  // Simple index-based x position
                    Entry(xPos, v.toFloat())
                }

                // Update dataset
                dataSet.clear()
                entries.forEach { entry ->
                    dataSet.addEntry(entry)
                }

                // Update x-axis range to show the current window
                binding.chart.xAxis.apply {
                    axisMinimum = 0f  // Always start from 0
                    axisMaximum = dataPoints.size.toFloat().coerceAtMost(X_AXIS_ELEMENTS_COUNT)  // Scale up to max points
                }

                // Update y-axis range based on current data
                if (dataPoints.isNotEmpty()) {
                    val min = dataPoints.minOrNull() ?: 0
                    val max = dataPoints.maxOrNull() ?: 100
                    val range = max - min
                    
                    binding.chart.axisLeft.apply {
                        // Add padding to the range
                        axisMinimum = (min - Y_AXIS_PADDING).toFloat().coerceAtLeast(0f)
                        axisMaximum = (max + Y_AXIS_PADDING).toFloat()
                    }
                }

                lineData.notifyDataChanged()
                binding.chart.notifyDataSetChanged()
                binding.chart.invalidate()
            }
        }
    }

    override fun updateUI() {
        // Update connection status text based on service state
        binding.connectionStatusText.text = when (getConnectionState()) {
            BluetoothService.STATE_CONNECTED -> "Connected"
            BluetoothService.STATE_DISCONNECTED -> "Not Connected"
            BluetoothService.STATE_CONNECTING -> "Connecting..."
            else -> "Not Connected"
        }

        // Update connect button text
        binding.buttonConnect.text = when (getConnectionState()) {
            BluetoothService.STATE_CONNECTED -> "Disconnect"
            BluetoothService.STATE_CONNECTING -> "Cancel"
            else -> "Connect to Sensor"
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Heart Rate"
    }

    private fun setupGraphControls() {
        binding.buttonResetChart.setOnClickListener {
            resetChart()
        }
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
}