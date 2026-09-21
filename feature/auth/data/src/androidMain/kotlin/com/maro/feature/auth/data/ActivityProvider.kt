package com.maro.feature.auth.data

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Tracks the activity the user is looking at. Firebase phone verification needs an Activity
 * (for the reCAPTCHA / Play Integrity fallback), and the domain layer must not know about Android.
 */
class ActivityProvider(application: Application) : Application.ActivityLifecycleCallbacks {
    private var resumed: WeakReference<Activity>? = null

    val current: Activity? get() = resumed?.get()

    init {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        resumed = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumed?.get() === activity) resumed = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
