package com.maro

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.maro.core.data.di.firebaseCoreModule
import com.maro.core.database.di.databaseModule
import com.maro.di.appModule
import com.maro.feature.auth.data.di.authDataModule
import com.maro.feature.auth.presentation.di.authPresentationModule
import com.maro.feature.chat.data.di.chatDataModule
import com.maro.feature.chat.domain.ChatImageUrls
import com.maro.feature.chat.presentation.di.chatPresentationModule
import com.maro.feature.chat.presentation.image.addChatImages
import com.maro.feature.chatlist.data.di.chatListDataModule
import com.maro.feature.chatlist.presentation.di.chatListPresentationModule
import com.maro.feature.profile.data.di.profileDataModule
import com.maro.feature.profile.presentation.di.profilePresentationModule
import com.maro.notifications.MessageNotifications
import io.ktor.client.HttpClient
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MaroApplication :
    Application(),
    SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MaroApplication)
            modules(
                appModule,
                firebaseCoreModule,
                databaseModule,
                // features (a Koin module is added only when a layer has something to provide)
                authDataModule,
                authPresentationModule,
                chatDataModule,
                chatPresentationModule,
                chatListDataModule,
                chatListPresentationModule,
                profileDataModule,
                profilePresentationModule,
            )
        }
        // Before any push can arrive: a notification posted to a missing channel is dropped.
        get<MessageNotifications>().createChannel()
    }

    /**
     * The one image loader of the app (Coil): pictures over our Ktor client, and chat photos through the server's
     * signed URLs, cached on disk under their storage key.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(KtorNetworkFetcherFactory(httpClient = { get<HttpClient>() }))
            addChatImages(urls = get<ChatImageUrls>())
        }
        .build()
}
