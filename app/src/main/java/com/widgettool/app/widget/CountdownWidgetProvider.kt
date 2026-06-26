package com.widgettool.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.widgettool.app.R
import com.widgettool.app.data.WidgetRepository
import com.widgettool.app.model.CountdownWidgetData
import com.widgettool.app.model.WidgetItem

class CountdownWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TICK = "com.widgettool.app.widget.COUNTDOWN_TICK"
        const val ACTION_TOGGLE = "com.widgettool.app.widget.COUNTDOWN_TOGGLE"
        const val ACTION_RESET = "com.widgettool.app.widget.COUNTDOWN_RESET"
        const val EXTRA_WIDGET_ID = "extra_widget_id"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        when (intent.action) {
            ACTION_TICK -> handleTick(context)
            ACTION_TOGGLE -> handleToggle(context, appWidgetId)
            ACTION_RESET -> handleReset(context, appWidgetId)
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val mgr = AppWidgetManager.getInstance(context)
                val cn = ComponentName(context, CountdownWidgetProvider::class.java)
                val ids = mgr.getAppWidgetIds(cn)
                for (id in ids) {
                    updateAppWidget(context, mgr, id)
                }
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleTick(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelTick(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val repository = WidgetRepository.getInstance(context)
        for (id in appWidgetIds) {
            repository.unbindAppWidgetId(id)
        }
    }

    private fun getWidgetItem(context: Context, appWidgetId: Int): WidgetItem? {
        val repository = WidgetRepository.getInstance(context)
        val itemId = repository.getWidgetItemIdByAppWidgetId(appWidgetId) ?: return null
        return repository.getWidgetById(itemId)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_countdown)
        val widget = getWidgetItem(context, appWidgetId)
        val data = widget?.countdownData ?: CountdownWidgetData()

        val alpha = ((widget?.opacity ?: 100) / 100f * 255).toInt()
        views.setInt(R.id.tvCountdownTime, "setTextColor", android.graphics.Color.argb(
            alpha,
            (0xE9 and 0xFF),
            (0x45 and 0xFF),
            (0x60 and 0xFF)
        ))

        val remaining = if (data.isRunning) {
            val elapsed = (System.currentTimeMillis() - data.startTime) / 1000
            (data.remainingSeconds - elapsed).coerceAtLeast(0)
        } else {
            data.remainingSeconds
        }

        val hours = remaining / 3600
        val minutes = (remaining % 3600) / 60
        val seconds = remaining % 60

        val timeText = if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }

        views.setTextViewText(R.id.tvCountdownTime, timeText)
        views.setTextViewText(R.id.tvCountdownLabel, data.label)

        val toggleIntent = Intent(context, CountdownWidgetProvider::class.java)
        toggleIntent.action = ACTION_TOGGLE
        toggleIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

        val resetIntent = Intent(context, CountdownWidgetProvider::class.java)
        resetIntent.action = ACTION_RESET
        resetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val togglePendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 10 + 1,
            toggleIntent,
            flags
        )
        val resetPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 10 + 2,
            resetIntent,
            flags
        )

        views.setOnClickPendingIntent(R.id.tvCountdownTime, togglePendingIntent)
        views.setOnClickPendingIntent(R.id.tvCountdownLabel, resetPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun handleTick(context: Context) {
        val repository = WidgetRepository.getInstance(context)
        val widgets = repository.getWidgetList()
        var needUpdate = false

        for (widget in widgets) {
            val data = widget.countdownData ?: continue
            if (data.isRunning && !data.isFinished) {
                val elapsed = (System.currentTimeMillis() - data.startTime) / 1000
                val remaining = data.remainingSeconds - elapsed
                if (remaining <= 0) {
                    data.remainingSeconds = 0
                    data.isRunning = false
                    data.isFinished = true
                    widget.countdownData = data
                    repository.updateWidget(widget)
                    needUpdate = true
                }
            }
        }

        if (needUpdate || hasRunningCountdown(context)) {
            val mgr = AppWidgetManager.getInstance(context)
            val cn = ComponentName(context, CountdownWidgetProvider::class.java)
            val ids = mgr.getAppWidgetIds(cn)
            for (id in ids) {
                updateAppWidget(context, mgr, id)
            }
        }
    }

    private fun hasRunningCountdown(context: Context): Boolean {
        val repository = WidgetRepository.getInstance(context)
        return repository.getWidgetList().any { widget ->
            widget.countdownData?.isRunning == true && !widget.countdownData!!.isFinished
        }
    }

    private fun handleToggle(context: Context, appWidgetId: Int) {
        val repository = WidgetRepository.getInstance(context)
        val widget = getWidgetItem(context, appWidgetId) ?: return
        val data = widget.countdownData ?: return

        if (data.isFinished) {
            data.remainingSeconds = data.totalSeconds
            data.isFinished = false
        }

        if (data.isRunning) {
            val elapsed = (System.currentTimeMillis() - data.startTime) / 1000
            data.remainingSeconds = (data.remainingSeconds - elapsed).coerceAtLeast(0)
            data.isRunning = false
        } else {
            data.startTime = System.currentTimeMillis()
            data.isRunning = true
        }

        widget.countdownData = data
        repository.updateWidget(widget)

        val mgr = AppWidgetManager.getInstance(context)
        val cn = ComponentName(context, CountdownWidgetProvider::class.java)
        val allIds = mgr.getAppWidgetIds(cn)
        for (id in allIds) {
            val boundItemId = repository.getWidgetItemIdByAppWidgetId(id)
            if (boundItemId == widget.id) {
                updateAppWidget(context, mgr, id)
            }
        }

        scheduleTick(context)
    }

    private fun handleReset(context: Context, appWidgetId: Int) {
        val repository = WidgetRepository.getInstance(context)
        val widget = getWidgetItem(context, appWidgetId) ?: return
        val data = widget.countdownData ?: return

        data.remainingSeconds = data.totalSeconds
        data.isRunning = false
        data.isFinished = false
        data.startTime = 0

        widget.countdownData = data
        repository.updateWidget(widget)

        val mgr = AppWidgetManager.getInstance(context)
        val cn = ComponentName(context, CountdownWidgetProvider::class.java)
        val allIds = mgr.getAppWidgetIds(cn)
        for (id in allIds) {
            val boundItemId = repository.getWidgetItemIdByAppWidgetId(id)
            if (boundItemId == widget.id) {
                updateAppWidget(context, mgr, id)
            }
        }
    }

    private fun scheduleTick(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CountdownWidgetProvider::class.java)
        intent.action = ACTION_TICK

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)

        val triggerTime = System.currentTimeMillis() + 1000
        alarmManager.setExact(AlarmManager.RTC, triggerTime, pendingIntent)
    }

    private fun cancelTick(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CountdownWidgetProvider::class.java)
        intent.action = ACTION_TICK

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
        alarmManager.cancel(pendingIntent)
    }

}
