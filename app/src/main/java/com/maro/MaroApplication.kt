package com.maro

import android.app.Application
import com.maro.feature.auth.presentation.di.authPresentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MaroApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MaroApplication)
            modules(
                // features (a Koin module is added only when a layer has something to provide)
                authPresentationModule,
            )
        }
    }
}
