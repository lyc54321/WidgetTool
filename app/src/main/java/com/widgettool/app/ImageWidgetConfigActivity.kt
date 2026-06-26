package com.widgettool.app

import com.widgettool.app.model.WidgetType

class ImageWidgetConfigActivity : WidgetConfigActivity() {
    override fun getWidgetType(): WidgetType = WidgetType.IMAGE
}
