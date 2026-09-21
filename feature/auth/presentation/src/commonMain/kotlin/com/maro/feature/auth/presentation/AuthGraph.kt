package com.maro.feature.auth.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.maro.feature.auth.presentation.registration.RegistrationRoot
import com.maro.feature.auth.presentation.welcome.WelcomeRoot
import kotlinx.serialization.Serializable

@Serializable
data object AuthGraphRoute

@Serializable
data object WelcomeRoute

@Serializable
data object RegistrationRoute

/**
 * Navigation graph of the auth feature.
 * @param onAuthenticated cross-feature callback: called when the user is signed in.
 */
fun NavGraphBuilder.authGraph(
    navController: NavController,
    onAuthenticated: () -> Unit,
) {
    navigation<AuthGraphRoute>(startDestination = WelcomeRoute) {
        composable<WelcomeRoute> {
            WelcomeRoot(
                onNavigateToRegistration = { navController.navigate(RegistrationRoute) },
            )
        }
        composable<RegistrationRoute> {
            RegistrationRoot(
                onNavigateBack = { navController.popBackStack() },
                onNavigateNext = onAuthenticated,
            )
        }
    }
}
