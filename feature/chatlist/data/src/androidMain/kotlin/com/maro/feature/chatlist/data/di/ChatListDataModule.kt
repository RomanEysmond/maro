package com.maro.feature.chatlist.data.di

import com.maro.feature.chatlist.data.ChatRemoteDataSource
import com.maro.feature.chatlist.data.DefaultChatRepository
import com.maro.feature.chatlist.data.DefaultNewChatRepository
import com.maro.feature.chatlist.data.FirestoreChatRemoteDataSource
import com.maro.feature.chatlist.data.FirestoreNewChatRemoteDataSource
import com.maro.feature.chatlist.data.NewChatRemoteDataSource
import com.maro.feature.chatlist.domain.ChatRepository
import com.maro.feature.chatlist.domain.NewChatRepository
import org.koin.dsl.module

/** Needs `firebaseCoreModule` (`:core:data`), `databaseModule` (`:core:database`) and `profileDataModule`. */
val chatListDataModule = module {
    single<ChatRemoteDataSource> { FirestoreChatRemoteDataSource(get(), get()) }
    // A singleton: it owns the always-on Firestore listener that keeps Room current.
    single<ChatRepository> { DefaultChatRepository(get(), get()) }

    single<NewChatRemoteDataSource> { FirestoreNewChatRemoteDataSource(get()) }
    single<NewChatRepository> { DefaultNewChatRepository(get(), get(), get()) }
}
