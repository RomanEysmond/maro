package com.maro.feature.chatlist.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.maro.feature.chatlist.presentation.newchat.NewChatRoot
import kotlinx.serialization.Serializable

@Serializable
data object ChatListGraphRoute

@Serializable
data object ChatListRoute

@Serializable
data object NewChatRoute

/** Navigation graph of the chat list. Every destination outside this feature is a callback. */
fun NavGraphBuilder.chatListGraph(
    navController: NavController,
    onOpenChat: (chatId: String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    navigation<ChatListGraphRoute>(startDestination = ChatListRoute) {
        composable<ChatListRoute> {
            ChatListRoot(
                onNewChatClick = { navController.navigate(NewChatRoute) },
                onOpenChat = onOpenChat,
                onOpenProfile = onOpenProfile,
                onOpenSettings = onOpenSettings,
                onOpenHelp = onOpenHelp,
            )
        }
        composable<NewChatRoute> {
            NewChatRoot(
                onNavigateBack = { navController.popBackStack() },
                onOpenChat = { chatId ->
                    // The search screen is done: back from the conversation goes to the list, not to the search.
                    navController.popBackStack()
                    onOpenChat(chatId)
                },
            )
        }
    }
}
