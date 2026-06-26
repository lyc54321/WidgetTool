package com.widgettool.app

import android.app.Application
import android.content.Context

class WidgetToolApp : Application() {

    companion object {
        lateinit var instance: WidgetToolApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

}
