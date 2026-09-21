package com.maro.feature.profile.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.maro.feature.profile.presentation.edit.EditProfileMode
import com.maro.feature.profile.presentation.edit.EditProfileRoot
import com.maro.feature.profile.presentation.edit.EditProfileViewModel
import com.maro.feature.profile.presentation.help.HelpScreen
import com.maro.feature.profile.presentation.help.HelpScreenChangedPhone
import com.maro.feature.profile.presentation.help.HelpScreenCreateGroupChat
import com.maro.feature.profile.presentation.help.HelpScreenHideLastSession
import com.maro.feature.profile.presentation.profile.ProfileRoot
import com.maro.feature.profile.presentation.settings.SettingsScreen
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data object ProfileRoute

/** The optional "tell us about yourself" screen shown once, right after the first sign-in. */
@Serializable
data object ProfileSetupRoute

@Serializable
data object EditProfileRoute

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
 * Profile, settings and help screens. ProfileRoute, ProfileSetupRoute, SettingsRoute and HelpRoute are
 * entry points for the app; the edit screen and the help sub-screens are only reachable from inside this feature.
 * @param onLogout cross-feature callback: the user asked to sign out (the app clears the session).
 * @param onProfileSetupFinished cross-feature callback: the first-time setup was saved or skipped.
 */
fun NavGraphBuilder.profileGraph(
    navController: NavController,
    onLogout: () -> Unit,
    onProfileSetupFinished: () -> Unit,
) {
    composable<ProfileRoute> {
        ProfileRoot(
            onEditClick = { navController.navigate(EditProfileRoute) },
            onLogoutClick = onLogout,
        )
    }
    composable<ProfileSetupRoute> {
        EditProfileRoot(
            viewModel = koinViewModel<EditProfileViewModel> { parametersOf(EditProfileMode.SETUP) },
            onClose = onProfileSetupFinished,
        )
    }
    composable<EditProfileRoute> {
        EditProfileRoot(
            viewModel = koinViewModel<EditProfileViewModel> { parametersOf(EditProfileMode.EDIT) },
            onClose = { navController.popBackStack() },
        )
    }
    composable<SettingsRoute> {
        SettingsScreen(onLogoutClick = onLogout)
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
