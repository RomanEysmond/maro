package com.maro.feature.chat.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class ChatRoute(val chatId: String)

/** Navigation graph of the conversation screen. Opened from :app via a callback from the chat list. */
fun NavGraphBuilder.chatGraph(
    onNavigateBack: () -> Unit,
) {
    composable<ChatRoute> { backStackEntry ->
        val route: ChatRoute = backStackEntry.toRoute()
        ChatRoot(
            viewModel = koinViewModel<ChatViewModel> { parametersOf(route.chatId) },
            onNavigateBack = onNavigateBack,
        )
    }
}
