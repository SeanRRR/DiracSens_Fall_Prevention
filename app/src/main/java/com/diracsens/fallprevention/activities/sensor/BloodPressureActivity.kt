// BloodPressureActivity.kt
package com.diracsens.fallprevention.activities.sensor

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.diracsens.fallprevention.databinding.ActivityBloodPressureBinding
import com.diracsens.fallprevention.models.BloodPressureReading
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

class BloodPressureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBloodPressureBinding
    private val viewModel: HealthMetricsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBloodPressureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupChart()
        observeData()
        setupExportButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Blood Pressure"
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
                axisMinimum = 40f  // Minimum value for diastolic
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
        // Observe current blood pressure
        viewModel.currentBloodPressure.observe(this) { (systolic, diastolic) ->
            binding.textSystolic.text = "$systolic"
            binding.textDiastolic.text = "$diastolic"

            // Update status based on blood pressure values
            val status = when {
                systolic >= 140 || diastolic >= 90 -> "High"
                systolic >= 120 || diastolic >= 80 -> "Elevated"
                else -> "Normal"
            }
            binding.textStatus.text = status

            // Set status color
            val statusColor = when(status) {
                "High" -> Color.RED
                "Elevated" -> Color.YELLOW
                else -> Color.GREEN
            }
            binding.textStatus.setTextColor(statusColor)
        }

        // Observe blood pressure history for chart
        viewModel.bloodPressureHistory.observe(this) { readings ->
            updateChart(readings)
        }
    }

    private fun updateChart(readings: List<BloodPressureReading>) {
        if (readings.isEmpty()) return

        // Prepare data for systolic readings
        val systolicEntries = readings.map { reading ->
            Entry(reading.timestamp.toFloat(), reading.systolic.toFloat())
        }

        // Prepare data for diastolic readings
        val diastolicEntries = readings.map { reading ->
            Entry(reading.timestamp.toFloat(), reading.diastolic.toFloat())
        }

        // Create datasets
        val systolicDataSet = LineDataSet(systolicEntries, "Systolic").apply {
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

        val diastolicDataSet = LineDataSet(diastolicEntries, "Diastolic").apply {
            color = Color.BLUE
            setCircleColor(Color.BLUE)
            lineWidth = 2f
            circleRadius = 3f
            setDrawCircleHole(false)
            valueTextSize = 9f
            setDrawFilled(true)
            fillColor = Color.BLUE
            fillAlpha = 50
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        // Create line data with both datasets
        val lineData = LineData(systolicDataSet, diastolicDataSet)

        // Set data to chart
        binding.chart.data = lineData
        binding.chart.invalidate()
    }

    private fun setupExportButton() {
        binding.buttonExport.setOnClickListener {
            viewModel.exportData(BluetoothService.DATA_TYPE_BLOOD_PRESSURE) { uri ->
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