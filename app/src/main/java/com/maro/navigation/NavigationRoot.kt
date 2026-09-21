package com.maro.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.maro.feature.auth.presentation.AuthGraphRoute
import com.maro.feature.auth.presentation.authGraph
import com.maro.feature.chat.presentation.chatGraph
import com.maro.feature.chatlist.presentation.ChatListGraphRoute
import com.maro.feature.chatlist.presentation.chatListGraph
import com.maro.feature.profile.presentation.HelpRoute
import com.maro.feature.profile.presentation.ProfileRoute
import com.maro.feature.profile.presentation.SettingsRoute
import com.maro.feature.profile.presentation.profileGraph

/**
 * Assembles the navigation graphs of all features. Features never import each other's routes:
 * cross-feature navigation is expressed here as callbacks.
 */
@Composable
fun NavigationRoot(
    // Stage 2 replaces this with the real session state from the auth feature.
    isLoggedIn: Boolean = false,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) ChatListGraphRoute else AuthGraphRoute,
    ) {
        authGraph(
            navController = navController,
            onAuthenticated = {
                navController.navigate(ChatListGraphRoute) {
                    popUpTo(AuthGraphRoute) { inclusive = true }
                }
            },
        )
        chatListGraph(
            onOpenProfile = { navController.navigate(ProfileRoute) },
            onOpenSettings = { navController.navigate(SettingsRoute) },
            onOpenHelp = { navController.navigate(HelpRoute) },
        )
        chatGraph()
        profileGraph(navController)
    }
}
