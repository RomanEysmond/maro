package com.maro.feature.auth.data.di

import com.maro.feature.auth.data.ActivityProvider
import com.maro.feature.auth.data.FirebasePhoneAuthenticator
import com.maro.feature.auth.data.FirebaseSessionRepository
import com.maro.feature.auth.domain.PhoneAuthenticator
import com.maro.feature.auth.domain.SessionRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

/** Needs `firebaseCoreModule` (FirebaseAuth) from `:core:data`. */
val authDataModule = module {
    // Eager: it has to be registered before the first activity is resumed.
    single(createdAtStart = true) { ActivityProvider(androidApplication()) }

    single<PhoneAuthenticator> { FirebasePhoneAuthenticator(get(), get()) }
    single<SessionRepository> { FirebaseSessionRepository(get()) }
}
