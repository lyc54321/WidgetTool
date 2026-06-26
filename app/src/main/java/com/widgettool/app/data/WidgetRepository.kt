package com.widgettool.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.widgettool.app.model.WidgetItem
import com.widgettool.app.model.WidgetType
import java.util.UUID

class WidgetRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val WIDGET_LIST_KEY = "widget_list"
    private val WIDGET_ID_MAP_KEY = "widget_id_map"

    companion object {
        @Volatile
        private var instance: WidgetRepository? = null

        fun getInstance(context: Context): WidgetRepository {
            return instance ?: synchronized(this) {
                instance ?: WidgetRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getWidgetList(): MutableList<WidgetItem> {
        val json = prefs.getString(WIDGET_LIST_KEY, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<WidgetItem>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveWidgetList(list: List<WidgetItem>) {
        prefs.edit().putString(WIDGET_LIST_KEY, gson.toJson(list)).apply()
    }

    fun addWidget(widget: WidgetItem) {
        val list = getWidgetList()
        list.add(widget)
        saveWidgetList(list)
    }

    fun updateWidget(widget: WidgetItem) {
        val list = getWidgetList()
        val index = list.indexOfFirst { it.id == widget.id }
        if (index >= 0) {
            list[index] = widget
            saveWidgetList(list)
        }
    }

    fun deleteWidget(widgetId: String) {
        val list = getWidgetList()
        list.removeAll { it.id == widgetId }
        saveWidgetList(list)
    }

    fun getWidgetById(id: String): WidgetItem? {
        return getWidgetList().find { it.id == id }
    }

    fun generateWidgetId(): String {
        return UUID.randomUUID().toString()
    }

    fun bindAppWidgetId(widgetId: Int, itemId: String) {
        val map = getWidgetIdMap()
        map[widgetId.toString()] = itemId
        prefs.edit().putString(WIDGET_ID_MAP_KEY, gson.toJson(map)).apply()
    }

    fun unbindAppWidgetId(widgetId: Int) {
        val map = getWidgetIdMap()
        map.remove(widgetId.toString())
        prefs.edit().putString(WIDGET_ID_MAP_KEY, gson.toJson(map)).apply()
    }

    fun getWidgetItemIdByAppWidgetId(widgetId: Int): String? {
        val map = getWidgetIdMap()
        return map[widgetId.toString()]
    }

    private fun getWidgetIdMap(): MutableMap<String, String> {
        val json = prefs.getString(WIDGET_ID_MAP_KEY, null) ?: return mutableMapOf()
        val type = object : TypeToken<MutableMap<String, String>>() {}.type
        return gson.fromJson(json, type)
    }

    fun getWidgetTypeString(type: WidgetType): String {
        return when (type) {
            WidgetType.IMAGE -> "图片"
            WidgetType.WOODEN_FISH -> "木鱼"
            WidgetType.COUNTDOWN -> "倒计时"
        }
    }

}
