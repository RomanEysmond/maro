package com.maro.feature.auth.presentation.di

import com.maro.feature.auth.presentation.registration.RegistrationViewModel
import com.maro.feature.auth.presentation.verifycode.VerifyCodeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authPresentationModule = module {
    viewModelOf(::RegistrationViewModel)
    // Takes the route (name and phone typed on the previous screen) as a parameter.
    viewModel { params -> VerifyCodeViewModel(params.get(), get(), get()) }
}
