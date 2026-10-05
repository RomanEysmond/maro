package com.maro.di

import com.maro.BuildConfig
import com.maro.MainViewModel
import com.maro.core.domain.session.SignOutCleaner
import com.maro.feature.chat.data.push.PushServerConfig
import com.maro.notifications.MessageNotifications
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    viewModel { MainViewModel(get(), get(), get(), signOutCleaners = getAll()) }
    single { MessageNotifications(androidContext()) }
    single<SignOutCleaner>(named("notifications")) { SignOutCleaner { get<MessageNotifications>().cancelAll() } }
    // Debug: the push server running on the development machine (see server/README.md); release: none yet.
    single { PushServerConfig(BuildConfig.PUSH_SERVER_URL) }
}
