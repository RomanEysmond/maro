package com.maro.notifications

/** Whether the app's UI is on screen (the single activity is started); kept by [com.maro.MainActivity]. */
object AppVisibility {
    @Volatile
    var isVisible: Boolean = false
        private set

    fun onActivityStarted() {
        isVisible = true
    }

    fun onActivityStopped() {
        isVisible = false
    }
}
