package com.maro.feature.chatlist.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import kotlinx.serialization.Serializable

@Serializable
data object ChatListGraphRoute

@Serializable
data object ChatListRoute

/** Navigation graph of the chat list. Every destination outside this feature is a callback. */
fun NavGraphBuilder.chatListGraph(
    onOpenChat: (chatId: String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    navigation<ChatListGraphRoute>(startDestination = ChatListRoute) {
        composable<ChatListRoute> {
            ChatListRoot(
                onOpenChat = onOpenChat,
                onOpenProfile = onOpenProfile,
                onOpenSettings = onOpenSettings,
                onOpenHelp = onOpenHelp,
            )
        }
    }
}
