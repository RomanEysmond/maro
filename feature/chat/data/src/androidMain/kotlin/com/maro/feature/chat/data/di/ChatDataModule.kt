package com.maro.feature.chat.data.di

import com.maro.feature.chat.data.DefaultMessageRepository
import com.maro.feature.chat.data.FirestoreMessageRemoteDataSource
import com.maro.feature.chat.data.MessageRemoteDataSource
import com.maro.feature.chat.data.WorkManagerOutboxScheduler
import com.maro.feature.chat.data.push.KtorMessagePushNotifier
import com.maro.feature.chat.data.push.MessagePushNotifier
import com.maro.feature.chat.domain.MessageRepository
import com.maro.feature.chat.domain.OutboxScheduler
import com.maro.feature.chat.domain.TypingRepository
import com.maro.feature.chat.data.typing.DefaultTypingRepository
import com.maro.feature.chat.data.typing.FirestoreTypingRemoteDataSource
import com.maro.feature.chat.data.typing.TypingRemoteDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Needs `firebaseCoreModule` (`:core:data`) and `databaseModule` (`:core:database`). */
val chatDataModule = module {
    single<MessageRemoteDataSource> { FirestoreMessageRemoteDataSource(get()) }
    single<OutboxScheduler> { WorkManagerOutboxScheduler(androidContext()) }
    // PushServerConfig comes from :app (the server address is per build type).
    single<MessagePushNotifier> { KtorMessagePushNotifier(get(), get(), get()) }
    // A singleton: the WorkManager worker resolves the same instance, so both share one outbox lock.
    single<MessageRepository> { DefaultMessageRepository(get(), get(), get(), get(), get(), get(), get()) }
    single<TypingRemoteDataSource> { FirestoreTypingRemoteDataSource(get()) }
    single<TypingRepository> { DefaultTypingRepository(get(), get()) }
}
