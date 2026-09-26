package com.maro.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import com.maro.MainViewModel
import com.maro.feature.auth.presentation.AuthGraphRoute
import com.maro.feature.auth.presentation.authGraph
import com.maro.feature.chat.presentation.ChatRoute
import com.maro.feature.chat.presentation.chatGraph
import com.maro.feature.chatlist.presentation.ChatListGraphRoute
import com.maro.feature.chatlist.presentation.chatListGraph
import com.maro.feature.profile.presentation.HelpRoute
import com.maro.feature.profile.presentation.ProfileRoute
import com.maro.feature.profile.presentation.ProfileSetupRoute
import com.maro.feature.profile.presentation.SettingsRoute
import com.maro.feature.profile.presentation.profileGraph
import org.koin.compose.viewmodel.koinViewModel

/**
 * Assembles the navigation graphs of all features. Features never import each other's routes:
 * cross-feature navigation is expressed here as callbacks.
 */
@Composable
fun NavigationRoot(
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = koinViewModel(),
) {
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsState()
    // The session is known synchronously, so the start destination is chosen once and there is no splash screen.
    val startDestination = remember { if (mainViewModel.isLoggedIn.value) ChatListGraphRoute else AuthGraphRoute }

    // Signing out (from any screen, or the session ending) sends the user back to the login flow with a clean back stack.
    // Signing in is handled by `onAuthenticated`: the profile has to be saved before the chat list opens.
    LaunchedEffect(isLoggedIn) {
        val inAuthFlow = navController.currentBackStackEntry?.destination
            ?.hierarchy?.any { it.hasRoute<AuthGraphRoute>() } ?: true
        if (!isLoggedIn && !inAuthFlow) {
            navController.navigate(AuthGraphRoute) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        authGraph(
            navController = navController,
            onAuthenticated = { isNewUser ->
                // A new user is offered the optional profile details first; the screen can be skipped.
                navController.navigate(if (isNewUser) ProfileSetupRoute else ChatListGraphRoute) {
                    popUpTo(AuthGraphRoute) { inclusive = true }
                }
            },
        )
        chatListGraph(
            navController = navController,
            onOpenChat = { chatId -> navController.navigate(ChatRoute(chatId)) },
            onOpenProfile = { navController.navigate(ProfileRoute) },
            onOpenSettings = { navController.navigate(SettingsRoute) },
            onOpenHelp = { navController.navigate(HelpRoute) },
        )
        chatGraph(onNavigateBack = { navController.popBackStack() })
        profileGraph(
            navController = navController,
            onLogout = mainViewModel::onLogoutClick,
            onProfileSetupFinished = {
                navController.navigate(ChatListGraphRoute) {
                    popUpTo(ProfileSetupRoute) { inclusive = true }
                }
            },
        )
    }
}
