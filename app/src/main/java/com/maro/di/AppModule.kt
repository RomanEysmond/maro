package com.maro.di

import com.maro.BuildConfig
import com.maro.MainViewModel
import com.maro.feature.chat.data.push.PushServerConfig
import com.maro.notifications.MessageNotifications
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    viewModelOf(::MainViewModel)
    single { MessageNotifications(androidContext()) }
    // Debug: the push server running on the development machine (see server/README.md); release: none yet.
    single { PushServerConfig(BuildConfig.PUSH_SERVER_URL) }
}
