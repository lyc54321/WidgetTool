package com.widgettool.app

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.widgettool.app.data.WidgetRepository
import com.widgettool.app.databinding.ActivityWoodenFishWidgetEditBinding
import com.widgettool.app.model.WidgetItem
import com.widgettool.app.model.WoodenFishWidgetData
import com.widgettool.app.widget.WoodenFishWidgetProvider

class WoodenFishWidgetEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWoodenFishWidgetEditBinding
    private lateinit var repository: WidgetRepository
    private var widgetId: String = ""
    private var widget: WidgetItem? = null
    private var fishData: WoodenFishWidgetData = WoodenFishWidgetData()
    private var opacity: Int = 100
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWoodenFishWidgetEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = WidgetRepository.getInstance(this)
        widgetId = intent.getStringExtra("widget_id") ?: ""

        initVibrator()
        loadWidgetData()
        initViews()
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun loadWidgetData() {
        widget = repository.getWidgetById(widgetId)
        widget?.let { w ->
            opacity = w.opacity
            fishData = w.woodenFishData?.copy() ?: WoodenFishWidgetData()
        }
    }

    private fun initViews() {
        binding.tvTitle.text = widget?.name ?: getString(R.string.wooden_fish_widget)

        updateMeritDisplay()

        binding.sbOpacity.progress = opacity
        binding.tvOpacityValue.text = "$opacity%"
        binding.ivPreviewFish.alpha = opacity / 100f

        binding.cbSoundEnabled.isChecked = fishData.soundEnabled
        binding.cbVibrateEnabled.isChecked = fishData.vibrateEnabled

        binding.ivPreviewFish.setOnClickListener {
            performTap()
        }

        binding.sbOpacity.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                opacity = progress
                binding.tvOpacityValue.text = "$opacity%"
                binding.ivPreviewFish.alpha = opacity / 100f
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.cbSoundEnabled.setOnCheckedChangeListener { _, isChecked ->
            fishData.soundEnabled = isChecked
        }

        binding.cbVibrateEnabled.setOnCheckedChangeListener { _, isChecked ->
            fishData.vibrateEnabled = isChecked
        }

        binding.btnSave.setOnClickListener {
            saveWidget()
            finish()
        }
    }

    private fun performTap() {
        fishData.meritCount++
        updateMeritDisplay()

        val anim = AnimationUtils.loadAnimation(this, R.anim.fish_tap)
        binding.ivPreviewFish.startAnimation(anim)

        if (fishData.vibrateEnabled) {
            vibrate()
        }

        saveWidget()
    }

    private fun updateMeritDisplay() {
        binding.tvPreviewMerit.text = fishData.meritCount.toString()
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(30)
        }
    }

    private fun saveWidget() {
        widget?.let { w ->
            w.opacity = opacity
            w.woodenFishData = fishData
            repository.updateWidget(w)

            val intent = Intent(this, WoodenFishWidgetProvider::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            sendBroadcast(intent)
        }
    }

}
