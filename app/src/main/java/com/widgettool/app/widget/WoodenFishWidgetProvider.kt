package com.widgettool.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.RemoteViews
import com.widgettool.app.R
import com.widgettool.app.data.WidgetRepository
import com.widgettool.app.model.WidgetItem
import com.widgettool.app.model.WoodenFishWidgetData

class WoodenFishWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_TAP = "com.widgettool.app.widget.WOODEN_FISH_TAP"
        const val ACTION_RESET = "com.widgettool.app.widget.WOODEN_FISH_RESET"
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
            ACTION_TAP -> handleTap(context, appWidgetId)
            ACTION_RESET -> handleReset(context, appWidgetId)
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val mgr = AppWidgetManager.getInstance(context)
                val cn = ComponentName(context, WoodenFishWidgetProvider::class.java)
                val ids = mgr.getAppWidgetIds(cn)
                for (id in ids) {
                    updateAppWidget(context, mgr, id)
                }
            }
        }
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
        val views = RemoteViews(context.packageName, R.layout.widget_wooden_fish)
        val widget = getWidgetItem(context, appWidgetId)
        val data = widget?.woodenFishData ?: WoodenFishWidgetData()

        val alpha = ((widget?.opacity ?: 100) / 100f * 255).toInt()
        views.setInt(R.id.ivWoodenFish, "setImageAlpha", alpha)

        views.setTextViewText(R.id.tvMeritCount, data.meritCount.toString())

        val tapIntent = Intent(context, WoodenFishWidgetProvider::class.java)
        tapIntent.action = ACTION_TAP
        tapIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

        val resetIntent = Intent(context, WoodenFishWidgetProvider::class.java)
        resetIntent.action = ACTION_RESET
        resetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val tapPendingIntent = PendingIntent.getBroadcast(context, appWidgetId * 10 + 1, tapIntent, flags)
        val resetPendingIntent = PendingIntent.getBroadcast(context, appWidgetId * 10 + 2, resetIntent, flags)

        views.setOnClickPendingIntent(R.id.ivWoodenFish, tapPendingIntent)
        views.setOnClickPendingIntent(R.id.btnReset, resetPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun handleTap(context: Context, appWidgetId: Int) {
        val repository = WidgetRepository.getInstance(context)
        val widget = getWidgetItem(context, appWidgetId) ?: return
        val data = widget.woodenFishData ?: return

        data.meritCount++
        widget.woodenFishData = data
        repository.updateWidget(widget)

        if (data.vibrateEnabled) {
            vibrate(context)
        }

        val mgr = AppWidgetManager.getInstance(context)
        val cn = ComponentName(context, WoodenFishWidgetProvider::class.java)
        val allIds = mgr.getAppWidgetIds(cn)
        for (id in allIds) {
            val boundItemId = repository.getWidgetItemIdByAppWidgetId(id)
            if (boundItemId == widget.id) {
                updateAppWidget(context, mgr, id)
            }
        }
    }

    private fun handleReset(context: Context, appWidgetId: Int) {
        val repository = WidgetRepository.getInstance(context)
        val widget = getWidgetItem(context, appWidgetId) ?: return
        val data = widget.woodenFishData ?: return

        data.meritCount = 0
        widget.woodenFishData = data
        repository.updateWidget(widget)

        val mgr = AppWidgetManager.getInstance(context)
        val cn = ComponentName(context, WoodenFishWidgetProvider::class.java)
        val allIds = mgr.getAppWidgetIds(cn)
        for (id in allIds) {
            val boundItemId = repository.getWidgetItemIdByAppWidgetId(id)
            if (boundItemId == widget.id) {
                updateAppWidget(context, mgr, id)
            }
        }
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

}
