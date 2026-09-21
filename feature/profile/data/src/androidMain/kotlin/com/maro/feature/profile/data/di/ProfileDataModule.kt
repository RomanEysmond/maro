package com.maro.feature.profile.data.di

import com.maro.core.domain.profile.UserProfileRepository
import com.maro.feature.profile.data.DefaultUserProfileRepository
import com.maro.feature.profile.data.FirestoreUserProfileRemoteDataSource
import com.maro.feature.profile.data.UserProfileRemoteDataSource
import org.koin.dsl.module

/** Needs `firebaseCoreModule` (FirebaseAuth, FirebaseFirestore) from `:core:data`. */
val profileDataModule = module {
    single<UserProfileRemoteDataSource> { FirestoreUserProfileRemoteDataSource(get(), get()) }
    // A singleton: it caches the current profile, which the profile screens observe.
    single<UserProfileRepository> { DefaultUserProfileRepository(get()) }
}
