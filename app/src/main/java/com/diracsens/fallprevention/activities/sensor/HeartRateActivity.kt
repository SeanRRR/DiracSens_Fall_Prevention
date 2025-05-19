// HeartRateActivity.kt
package com.diracsens.fallprevention.activities.sensor

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.diracsens.fallprevention.databinding.ActivityHeartRateBinding
import com.diracsens.fallprevention.models.HeartRateReading
import com.diracsens.fallprevention.services.BluetoothService
import com.diracsens.fallprevention.viewmodels.HealthMetricsViewModel
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HeartRateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHeartRateBinding
    private val viewModel: HealthMetricsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeartRateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupChart()
        observeData()
        setupExportButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Heart Rate"
    }

    private fun setupChart() {
        binding.chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                valueFormatter = DateAxisValueFormatter()
            }

            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 40f  // Minimum heart rate
            }

            axisRight.isEnabled = false

            legend.apply {
                form = Legend.LegendForm.LINE
                textSize = 11f
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }
        }
    }

    private fun observeData() {
        // Observe current heart rate
        viewModel.currentHeartRate.observe(this) { heartRate ->
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
        }

        // Observe heart rate history for chart
        viewModel.heartRateHistory.observe(this) { readings ->
            updateChart(readings)
        }
    }

    private fun updateChart(readings: List<HeartRateReading>) {
        if (readings.isEmpty()) return

        // Prepare data for heart rate readings
        val heartRateEntries = readings.map { reading ->
            Entry(reading.timestamp.toFloat(), reading.heartRate.toFloat())
        }

        // Create dataset
        val heartRateDataSet = LineDataSet(heartRateEntries, "Heart Rate").apply {
            color = Color.RED
            setCircleColor(Color.RED)
            lineWidth = 2f
            circleRadius = 3f
            setDrawCircleHole(false)
            valueTextSize = 9f
            setDrawFilled(true)
            fillColor = Color.RED
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        // Create line data
        val lineData = LineData(heartRateDataSet)

        // Set data to chart
        binding.chart.data = lineData
        binding.chart.invalidate()
    }

    private fun setupExportButton() {
        binding.buttonExport.setOnClickListener {
            viewModel.exportData(BluetoothService.DATA_TYPE_HEART_RATE) { uri ->
                if (uri != null) {
                    // Show success message
                    Toast.makeText(
                        this,
                        "Data exported to Downloads folder",
                        Toast.LENGTH_LONG
                    ).show()

                    // Open the file
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "text/csv")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    try {
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(
                            this,
                            "No app found to open CSV files",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Failed to export data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Date formatter for X-axis
    inner class DateAxisValueFormatter : ValueFormatter() {
        private val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

        override fun getFormattedValue(value: Float): String {
            return dateFormat.format(Date(value.toLong()))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}