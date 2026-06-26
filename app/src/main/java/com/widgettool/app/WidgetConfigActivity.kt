package com.widgettool.app

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.widgettool.app.data.WidgetRepository
import com.widgettool.app.databinding.ActivityWidgetConfigBinding
import com.widgettool.app.model.WidgetItem
import com.widgettool.app.model.WidgetType
import com.widgettool.app.ui.WidgetListAdapter

open class WidgetConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWidgetConfigBinding
    private lateinit var repository: WidgetRepository
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var widgetType: WidgetType? = null

    open fun getWidgetType(): WidgetType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWidgetConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = WidgetRepository.getInstance(this)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_CANCELED, resultValue)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        widgetType = getWidgetType() ?: getWidgetTypeFromProvider()
        initViews()
        loadWidgets()
    }

    private fun getWidgetTypeFromProvider(): WidgetType? {
        return when {
            intent?.component?.className?.contains("ImageWidgetProvider") == true -> WidgetType.IMAGE
            intent?.component?.className?.contains("WoodenFishWidgetProvider") == true -> WidgetType.WOODEN_FISH
            intent?.component?.className?.contains("CountdownWidgetProvider") == true -> WidgetType.COUNTDOWN
            else -> null
        }
    }

    private fun initViews() {
        binding.tvTitle.text = getString(R.string.widget_config)

        val adapter = WidgetListAdapter(
            onItemClick = { widget ->
                selectWidget(widget)
            },
            onItemLongClick = {}
        )

        binding.rvWidgetSelect.layoutManager = LinearLayoutManager(this)
        binding.rvWidgetSelect.adapter = adapter
    }

    private fun loadWidgets() {
        var widgets = repository.getWidgetList()
        widgetType?.let { type ->
            widgets = widgets.filter { it.type == type }.toMutableList()
        }

        val adapter = WidgetListAdapter(
            onItemClick = { widget ->
                selectWidget(widget)
            },
            onItemLongClick = {}
        )
        binding.rvWidgetSelect.adapter = adapter
        adapter.submitList(widgets)
    }

    private fun selectWidget(widget: WidgetItem) {
        repository.bindAppWidgetId(appWidgetId, widget.id)

        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)

        val updateIntent = Intent(this, MainActivity::class.java)
        updateIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(updateIntent)

        finish()
    }

}
