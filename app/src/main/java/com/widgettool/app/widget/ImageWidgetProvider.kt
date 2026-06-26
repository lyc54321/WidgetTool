package com.widgettool.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.widgettool.app.ImageWidgetEditActivity
import com.widgettool.app.MainActivity
import com.widgettool.app.R
import com.widgettool.app.data.WidgetRepository
import com.widgettool.app.model.ClickAction
import com.widgettool.app.model.WidgetItem
import com.widgettool.app.util.SoundPlayer
import java.io.File
import android.graphics.BitmapFactory

class ImageWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_CLICK = "com.widgettool.app.widget.IMAGE_CLICK"
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

        if (intent.action == ACTION_CLICK) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            handleClick(context, appWidgetId)
        } else if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val mgr = AppWidgetManager.getInstance(context)
            val cn = ComponentName(context, ImageWidgetProvider::class.java)
            val ids = mgr.getAppWidgetIds(cn)
            for (id in ids) {
                updateAppWidget(context, mgr, id)
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
        val views = RemoteViews(context.packageName, R.layout.widget_image)
        val widget = getWidgetItem(context, appWidgetId)

        val alpha = ((widget?.opacity ?: 100) / 100f * 255).toInt()
        views.setInt(R.id.ivWidgetImage, "setImageAlpha", alpha)

        widget?.imageData?.let { data ->
            if (data.imagePath.isNotEmpty()) {
                val file = File(data.imagePath)
                if (file.exists()) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            views.setImageViewBitmap(R.id.ivWidgetImage, bitmap)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        val clickIntent = Intent(context, ImageWidgetProvider::class.java)
        clickIntent.action = ACTION_CLICK
        clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(context, appWidgetId, clickIntent, flags)
        views.setOnClickPendingIntent(R.id.ivWidgetImage, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun handleClick(context: Context, appWidgetId: Int) {
        val widget = getWidgetItem(context, appWidgetId) ?: return
        val data = widget.imageData ?: return

        if (data.soundEnabled && data.soundUri.isNotEmpty()) {
            SoundPlayer.getInstance(context).playSound(Uri.parse(data.soundUri))
        }

        when (data.clickAction) {
            ClickAction.OPEN_APP -> {
                if (data.openAppPackage.isNotEmpty()) {
                    try {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(data.openAppPackage)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            ClickAction.OPEN_URL -> {
                if (data.openUrl.isNotEmpty()) {
                    try {
                        val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(data.openUrl))
                        urlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(urlIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            ClickAction.NONE -> {
                val openIntent = Intent(context, ImageWidgetEditActivity::class.java)
                openIntent.putExtra("widget_id", widget.id)
                openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(openIntent)
            }
        }
    }

}
