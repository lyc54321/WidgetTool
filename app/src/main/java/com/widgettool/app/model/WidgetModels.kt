package com.widgettool.app.model

enum class WidgetType {
    IMAGE,
    WOODEN_FISH,
    COUNTDOWN
}

enum class ClickAction {
    NONE,
    OPEN_APP,
    OPEN_URL
}

data class WidgetItem(
    val id: String,
    val type: WidgetType,
    var name: String,
    var opacity: Int = 100,
    var imageData: ImageWidgetData? = null,
    var woodenFishData: WoodenFishWidgetData? = null,
    var countdownData: CountdownWidgetData? = null
)

data class ImageWidgetData(
    var imagePath: String = "",
    var soundEnabled: Boolean = false,
    var soundUri: String = "",
    var clickAction: ClickAction = ClickAction.NONE,
    var openAppPackage: String = "",
    var openUrl: String = ""
)

data class WoodenFishWidgetData(
    var meritCount: Long = 0,
    var soundEnabled: Boolean = true,
    var vibrateEnabled: Boolean = true
)

data class CountdownWidgetData(
    var totalSeconds: Long = 300,
    var remainingSeconds: Long = 300,
    var label: String = "倒计时",
    var isRunning: Boolean = false,
    var startTime: Long = 0,
    var isFinished: Boolean = false
)
