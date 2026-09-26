package com.maro

import android.app.Application
import com.maro.di.appModule
import com.maro.core.data.di.firebaseCoreModule
import com.maro.core.database.di.databaseModule
import com.maro.feature.auth.data.di.authDataModule
import com.maro.feature.auth.presentation.di.authPresentationModule
import com.maro.feature.chat.data.di.chatDataModule
import com.maro.feature.chat.presentation.di.chatPresentationModule
import com.maro.feature.chatlist.data.di.chatListDataModule
import com.maro.feature.chatlist.presentation.di.chatListPresentationModule
import com.maro.feature.profile.data.di.profileDataModule
import com.maro.feature.profile.presentation.di.profilePresentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MaroApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MaroApplication)
            modules(
                appModule,
                firebaseCoreModule,
                databaseModule,
                // features (a Koin module is added only when a layer has something to provide)
                authDataModule,
                authPresentationModule,
                chatDataModule,
                chatPresentationModule,
                chatListDataModule,
                chatListPresentationModule,
                profileDataModule,
                profilePresentationModule,
            )
        }
    }
}
