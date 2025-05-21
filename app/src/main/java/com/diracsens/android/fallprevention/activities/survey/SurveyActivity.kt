package com.diracsens.android.fallprevention.activities.survey

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.diracsens.android.fallprevention.databinding.ActivitySurveyBinding

class SurveyActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySurveyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySurveyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        // TODO: Implement checklist logic for survey questions
        // TODO: Implement saving/submitting survey data
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Functional Activities Survey"
    }

    // TODO: Add functions for managing survey checklist UI and data
} 