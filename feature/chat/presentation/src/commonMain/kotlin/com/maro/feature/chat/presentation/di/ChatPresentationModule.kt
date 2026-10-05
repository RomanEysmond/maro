package com.maro.feature.chat.presentation.di

import com.maro.feature.chat.presentation.ChatViewModel
import com.maro.feature.chat.presentation.groupinfo.GroupInfoViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val chatPresentationModule = module {
    // Takes the id of the chat as a parameter.
    viewModel { params -> ChatViewModel(params.get(), get(), get()) }
    viewModel { params -> GroupInfoViewModel(params.get(), get(), get(), get()) }
}
