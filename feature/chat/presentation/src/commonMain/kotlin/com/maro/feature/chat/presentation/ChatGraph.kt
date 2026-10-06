package com.maro.feature.chat.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.maro.feature.chat.presentation.groupinfo.GroupInfoRoot
import com.maro.feature.chat.presentation.groupinfo.GroupInfoViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class ChatRoute(val chatId: String)

@Serializable
data class GroupInfoRoute(val chatId: String)

/** Navigation graph of the conversation and its group screen. Opened from :app via a callback from the chat list. */
fun NavGraphBuilder.chatGraph(navController: NavController, onNavigateBack: () -> Unit) {
    composable<ChatRoute> { backStackEntry ->
        val route: ChatRoute = backStackEntry.toRoute()
        ChatRoot(
            viewModel = koinViewModel<ChatViewModel> { parametersOf(route.chatId) },
            onNavigateBack = onNavigateBack,
            onOpenGroupInfo = { chatId -> navController.navigate(GroupInfoRoute(chatId)) },
        )
    }
    composable<GroupInfoRoute> { backStackEntry ->
        val route: GroupInfoRoute = backStackEntry.toRoute()
        GroupInfoRoot(
            viewModel = koinViewModel<GroupInfoViewModel> { parametersOf(route.chatId) },
            onNavigateBack = { navController.popBackStack() },
            // Out of the group: the conversation is pointless too. Back to wherever it was opened from.
            onLeftGroup = { navController.popBackStack<ChatRoute>(inclusive = true) },
        )
    }
}
