package com.maro.feature.chatlist.presentation.di

import com.maro.feature.chatlist.presentation.ChatListViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val chatListPresentationModule = module {
    viewModelOf(::ChatListViewModel)
}
