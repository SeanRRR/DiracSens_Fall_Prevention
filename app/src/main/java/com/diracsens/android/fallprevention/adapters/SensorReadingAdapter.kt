package com.diracsens.android.fallprevention.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.diracsens.android.fallprevention.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SensorReadingAdapter(
    private val valueFormatter: (Float) -> String = { it.toString() }
) : ListAdapter<SensorReading, SensorReadingAdapter.ViewHolder>(SensorReadingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sensor_reading, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        private val valueText: TextView = itemView.findViewById(R.id.valueText)
        private val dateFormat = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())

        fun bind(reading: SensorReading) {
            timestampText.text = dateFormat.format(Date(reading.timestamp))
            valueText.text = valueFormatter(reading.value)
        }
    }

    private class SensorReadingDiffCallback : DiffUtil.ItemCallback<SensorReading>() {
        override fun areItemsTheSame(oldItem: SensorReading, newItem: SensorReading): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: SensorReading, newItem: SensorReading): Boolean {
            return oldItem == newItem
        }
    }
}

data class SensorReading(
    val timestamp: Long,
    val value: Float
) 