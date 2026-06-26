package com.widgettool.app

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.widgettool.app.data.WidgetRepository
import com.widgettool.app.databinding.ActivityCountdownWidgetEditBinding
import com.widgettool.app.model.CountdownWidgetData
import com.widgettool.app.model.WidgetItem
import com.widgettool.app.widget.CountdownWidgetProvider

class CountdownWidgetEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCountdownWidgetEditBinding
    private lateinit var repository: WidgetRepository
    private var widgetId: String = ""
    private var widget: WidgetItem? = null
    private var countdownData: CountdownWidgetData = CountdownWidgetData()
    private var opacity: Int = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountdownWidgetEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = WidgetRepository.getInstance(this)
        widgetId = intent.getStringExtra("widget_id") ?: ""

        loadWidgetData()
        initViews()
    }

    private fun loadWidgetData() {
        widget = repository.getWidgetById(widgetId)
        widget?.let { w ->
            opacity = w.opacity
            countdownData = w.countdownData?.copy() ?: CountdownWidgetData()
        }
    }

    private fun initViews() {
        binding.tvTitle.text = widget?.name ?: getString(R.string.countdown_widget)

        binding.etLabel.setText(countdownData.label)

        val totalSeconds = countdownData.totalSeconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        binding.npHours.minValue = 0
        binding.npHours.maxValue = 23
        binding.npHours.value = hours.toInt()

        binding.npMinutes.minValue = 0
        binding.npMinutes.maxValue = 59
        binding.npMinutes.value = minutes.toInt()

        binding.npSeconds.minValue = 0
        binding.npSeconds.maxValue = 59
        binding.npSeconds.value = seconds.toInt()

        binding.sbOpacity.progress = opacity
        binding.tvOpacityValue.text = "$opacity%"

        binding.sbOpacity.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                opacity = progress
                binding.tvOpacityValue.text = "$opacity%"
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.btnSave.setOnClickListener {
            saveWidget()
            finish()
        }
    }

    private fun saveWidget() {
        widget?.let { w ->
            val hours = binding.npHours.value.toLong()
            val minutes = binding.npMinutes.value.toLong()
            val seconds = binding.npSeconds.value.toLong()
            val total = hours * 3600 + minutes * 60 + seconds

            countdownData.totalSeconds = total
            if (!countdownData.isRunning) {
                countdownData.remainingSeconds = total
            }
            countdownData.label = binding.etLabel.text.toString().ifEmpty { "倒计时" }
            countdownData.isFinished = false

            w.opacity = opacity
            w.countdownData = countdownData
            repository.updateWidget(w)

            val intent = Intent(this, CountdownWidgetProvider::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            sendBroadcast(intent)
        }
    }

}
