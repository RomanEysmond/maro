package com.maro.feature.profile.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.maro.feature.profile.presentation.help.HelpScreen
import com.maro.feature.profile.presentation.help.HelpScreenChangedPhone
import com.maro.feature.profile.presentation.help.HelpScreenCreateGroupChat
import com.maro.feature.profile.presentation.help.HelpScreenHideLastSession
import com.maro.feature.profile.presentation.profile.ProfileScreen
import com.maro.feature.profile.presentation.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object ProfileRoute

@Serializable
data object SettingsRoute

@Serializable
data object HelpRoute

@Serializable
data object HelpChangedPhoneRoute

@Serializable
data object HelpHideLastSessionRoute

@Serializable
data object HelpCreateGroupChatRoute

/**
 * Profile, settings and help screens. ProfileRoute, SettingsRoute and HelpRoute are entry points
 * for the app's menu; the help sub-screens are only reachable from inside this feature.
 */
fun NavGraphBuilder.profileGraph(navController: NavController) {
    composable<ProfileRoute> {
        ProfileScreen()
    }
    composable<SettingsRoute> {
        SettingsScreen()
    }
    composable<HelpRoute> {
        HelpScreen(
            onNavigateBack = { navController.popBackStack() },
            onChangedPhoneClick = { navController.navigate(HelpChangedPhoneRoute) },
            onHideLastSessionClick = { navController.navigate(HelpHideLastSessionRoute) },
            onCreateGroupChatClick = { navController.navigate(HelpCreateGroupChatRoute) },
        )
    }
    composable<HelpChangedPhoneRoute> {
        HelpScreenChangedPhone()
    }
    composable<HelpHideLastSessionRoute> {
        HelpScreenHideLastSession()
    }
    composable<HelpCreateGroupChatRoute> {
        HelpScreenCreateGroupChat()
    }
}
