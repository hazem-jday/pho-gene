package com.hazemjday.phogene

import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import com.hazemjday.phogene.databinding.ActivityLearnMoreBinding

class LearnMoreActivity : AppCompatActivity() {
    lateinit var binding: ActivityLearnMoreBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLearnMoreBinding.inflate(layoutInflater)
        setContentView(binding.root)



    }



}