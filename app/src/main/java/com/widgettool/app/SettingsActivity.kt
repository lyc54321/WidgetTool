package com.widgettool.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.widgettool.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvVersion.text = packageManager.getPackageInfo(packageName, 0).versionName
    }

}
