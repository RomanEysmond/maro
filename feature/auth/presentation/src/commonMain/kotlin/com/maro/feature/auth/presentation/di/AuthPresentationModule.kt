package com.maro.feature.auth.presentation.di

import com.maro.feature.auth.presentation.registration.RegistrationViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authPresentationModule = module {
    viewModelOf(::RegistrationViewModel)
}
