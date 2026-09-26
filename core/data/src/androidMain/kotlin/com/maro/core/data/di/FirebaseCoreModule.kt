package com.maro.core.data.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.maro.core.data.AndroidConnectivityObserver
import com.maro.core.data.FirebaseCurrentUserProvider
import com.maro.core.domain.auth.CurrentUserProvider
import com.maro.core.domain.connectivity.ConnectivityObserver
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Firebase entry points (and other platform services) shared by the data layers of all features. */
val firebaseCoreModule = module {
    single { FirebaseAuth.getInstance() }
    single {
        FirebaseFirestore.getInstance().apply {
            // Room is the single source of truth (see CLAUDE.md): Firestore is only a transport, no disk cache.
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        }
    }
    single<CurrentUserProvider> { FirebaseCurrentUserProvider(get()) }
    single<ConnectivityObserver> { AndroidConnectivityObserver(androidContext()) }
}
