package com.maro.feature.profile.presentation.di

import com.maro.feature.profile.presentation.edit.EditProfileViewModel
import com.maro.feature.profile.presentation.profile.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profilePresentationModule = module {
    viewModelOf(::ProfileViewModel)
    // Takes the mode (first-time setup or editing) as a parameter.
    viewModel { params -> EditProfileViewModel(params.get(), get()) }
}
