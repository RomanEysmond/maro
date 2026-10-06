package com.maro.feature.chat.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.maro.feature.chat.domain.ChatImage
import com.maro.feature.chat.presentation.groupinfo.GroupInfoRoot
import com.maro.feature.chat.presentation.groupinfo.GroupInfoViewModel
import com.maro.feature.chat.presentation.image.ImageViewerScreen
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class ChatRoute(val chatId: String)

@Serializable
data class GroupInfoRoute(val chatId: String)

/** A photo on its own; [localPath] is the sender's copy on this device, if there is one. */
@Serializable
data class ImageViewerRoute(val chatId: String, val key: String, val localPath: String? = null)

/** Navigation graph of the conversation and its group screen. Opened from :app via a callback from the chat list. */
fun NavGraphBuilder.chatGraph(navController: NavController, onNavigateBack: () -> Unit) {
    composable<ChatRoute> { backStackEntry ->
        val route: ChatRoute = backStackEntry.toRoute()
        ChatRoot(
            viewModel = koinViewModel<ChatViewModel> { parametersOf(route.chatId) },
            onNavigateBack = onNavigateBack,
            onOpenGroupInfo = { chatId -> navController.navigate(GroupInfoRoute(chatId)) },
            onOpenImage = { image ->
                navController.navigate(ImageViewerRoute(image.source.chatId, image.source.key, image.localPath))
            },
        )
    }
    composable<ImageViewerRoute> { backStackEntry ->
        val route: ImageViewerRoute = backStackEntry.toRoute()
        ImageViewerScreen(
            image = ChatImage(route.chatId, route.key),
            localPath = route.localPath,
            onBackClick = { navController.popBackStack() },
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
