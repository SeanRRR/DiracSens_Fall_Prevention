package com.diracsens.fallprevention.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.diracsens.fallprevention.databinding.ActivityMedicationBinding

class MedicationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMedicationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        // TODO: Implement checklist logic for medications
        // TODO: Implement saving/submitting medication data
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Medication Checklist"
    }

    // TODO: Add functions for managing medication checklist UI and data
} 