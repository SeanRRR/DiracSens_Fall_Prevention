package com.diracsens.fallprevention.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.diracsens.fallprevention.databinding.ActivityBaselineBinding

class BaselineActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBaselineBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBaselineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        // TODO: Implement data observation and UI updates for Baseline
        // TODO: Implement chart setup for Baseline history
        // TODO: Implement export functionality for Baseline data
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Baseline"
    }

    // TODO: Add functions for observing Baseline data and updating UI
    // TODO: Add functions for setting up and updating Baseline chart
    // TODO: Add function for exporting Baseline data
} 