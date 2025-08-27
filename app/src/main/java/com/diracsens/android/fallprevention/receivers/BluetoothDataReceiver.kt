package com.diracsens.android.fallprevention.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.diracsens.android.fallprevention.services.BluetoothService

class BluetoothDataReceiver(
    private val onDataReceived: (String, Float) -> Unit,
    private val onConnectionStateChanged: (Int) -> Unit,
    private val onDataBatchReceived: ((String, IntArray) -> Unit)? = null
) : BroadcastReceiver() {
    companion object {
        private const val TAG = "BluetoothDataReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")
        when (intent.action) {
            BluetoothService.ACTION_DATA_AVAILABLE -> {
                val dataType = intent.getStringExtra(BluetoothService.EXTRA_DATA_TYPE)
                val intArray = intent.getIntArrayExtra(BluetoothService.EXTRA_DATA)
                if (dataType != null && intArray != null) {
                    Log.d(TAG, "Received data batch - Type: $dataType, Values: ${intArray.joinToString()}")
                    onDataBatchReceived?.invoke(dataType, intArray)
                } else {
                    val value = intent.getFloatExtra(BluetoothService.EXTRA_DATA, 0f)
                    Log.d(TAG, "Received data - Type: $dataType, Value: $value")
                    if (dataType != null) {
                        onDataReceived(dataType, value)
                    }
                }
            }
            BluetoothService.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothService.EXTRA_CONNECTION_STATE, -1)
                Log.d(TAG, "Connection state changed to: $state")
                onConnectionStateChanged(state)
            }
        }
    }
} 