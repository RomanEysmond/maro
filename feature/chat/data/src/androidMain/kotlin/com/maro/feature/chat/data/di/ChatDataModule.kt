package com.maro.feature.chat.data.di

import com.maro.core.domain.session.SignOutCleaner
import com.maro.feature.chat.data.DefaultMessageRepository
import com.maro.feature.chat.data.FirestoreMessageRemoteDataSource
import com.maro.feature.chat.data.MessageRemoteDataSource
import com.maro.feature.chat.data.WorkManagerOutboxScheduler
import com.maro.feature.chat.data.cancelOutbox
import com.maro.feature.chat.data.group.DefaultGroupRepository
import com.maro.feature.chat.data.group.FirestoreGroupRemoteDataSource
import com.maro.feature.chat.data.group.GroupRemoteDataSource
import com.maro.feature.chat.data.media.AndroidImageFiles
import com.maro.feature.chat.data.media.ChatMediaRemoteDataSource
import com.maro.feature.chat.data.media.ImageFiles
import com.maro.feature.chat.data.media.KtorChatMediaRemoteDataSource
import com.maro.feature.chat.data.media.PhotoUploader
import com.maro.feature.chat.data.push.KtorMessagePushNotifier
import com.maro.feature.chat.data.push.MessagePushNotifier
import com.maro.feature.chat.data.typing.DefaultTypingRepository
import com.maro.feature.chat.data.typing.FirestoreTypingRemoteDataSource
import com.maro.feature.chat.data.typing.TypingRemoteDataSource
import com.maro.feature.chat.domain.ChatImageUrls
import com.maro.feature.chat.domain.GroupRepository
import com.maro.feature.chat.domain.MessageRepository
import com.maro.feature.chat.domain.OutboxScheduler
import com.maro.feature.chat.domain.TypingRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module

/** Needs `firebaseCoreModule` (`:core:data`) and `databaseModule` (`:core:database`). */
val chatDataModule = module {
    single<MessageRemoteDataSource> { FirestoreMessageRemoteDataSource(get()) }
    single<OutboxScheduler> { WorkManagerOutboxScheduler(androidContext()) }
    single<SignOutCleaner>(named("outbox")) { SignOutCleaner { cancelOutbox(androidContext()) } }
    // PushServerConfig comes from :app (the server address is per build type).
    single<MessagePushNotifier> { KtorMessagePushNotifier(get(), get(), get()) }
    // A singleton: the WorkManager worker resolves the same instance, so both share one outbox lock.
    single<MessageRepository> { DefaultMessageRepository(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { PhotoUploader(get(), get(), get()) }
    // Photos: URLs from our server (also what the image loader asks for), files in the app's own storage.
    single { KtorChatMediaRemoteDataSource(get(), get(), get()) } binds
        arrayOf(ChatMediaRemoteDataSource::class, ChatImageUrls::class)
    single<ImageFiles> { AndroidImageFiles(androidContext()) }
    single<SignOutCleaner>(named("media")) { SignOutCleaner { get<ImageFiles>().deleteAll() } }
    single<TypingRemoteDataSource> { FirestoreTypingRemoteDataSource(get()) }
    single<TypingRepository> { DefaultTypingRepository(get(), get()) }
    single<GroupRemoteDataSource> { FirestoreGroupRemoteDataSource(get()) }
    single<GroupRepository> { DefaultGroupRepository(get(), get()) }
}
