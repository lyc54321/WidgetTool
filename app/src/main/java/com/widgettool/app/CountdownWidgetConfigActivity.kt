package com.widgettool.app

import com.widgettool.app.model.WidgetType

class CountdownWidgetConfigActivity : WidgetConfigActivity() {
    override fun getWidgetType(): WidgetType = WidgetType.COUNTDOWN
}
