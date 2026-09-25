package com.maro.feature.chatlist.data.di

import com.maro.feature.chatlist.data.ChatRemoteDataSource
import com.maro.feature.chatlist.data.DefaultChatRepository
import com.maro.feature.chatlist.data.FirestoreChatRemoteDataSource
import com.maro.feature.chatlist.domain.ChatRepository
import org.koin.dsl.module

/** Needs `firebaseCoreModule` (`:core:data`) and `databaseModule` (`:core:database`). */
val chatListDataModule = module {
    single<ChatRemoteDataSource> { FirestoreChatRemoteDataSource(get(), get()) }
    // A singleton: it owns the always-on Firestore listener that keeps Room current.
    single<ChatRepository> { DefaultChatRepository(get(), get()) }
}
