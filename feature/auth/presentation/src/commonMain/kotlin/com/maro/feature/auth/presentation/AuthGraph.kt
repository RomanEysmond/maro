package com.maro.feature.auth.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.maro.feature.auth.presentation.registration.RegistrationRoot
import com.maro.feature.auth.presentation.verifycode.VerifyCodeRoot
import com.maro.feature.auth.presentation.verifycode.VerifyCodeViewModel
import com.maro.feature.auth.presentation.welcome.WelcomeRoot
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data object AuthGraphRoute

@Serializable
data object WelcomeRoute

@Serializable
data object RegistrationRoute

/** The data typed on the registration screen travels with the route: it is needed once the code is confirmed. */
@Serializable
data class VerifyCodeRoute(
    val firstName: String,
    val lastName: String,
    /** International format, e.g. "+79001234567". */
    val phone: String,
)

/**
 * Navigation graph of the auth feature.
 * @param onAuthenticated cross-feature callback: called when the user is signed in and has a profile; `isNewUser` is true right after the first sign-in.
 */
fun NavGraphBuilder.authGraph(
    navController: NavController,
    onAuthenticated: (isNewUser: Boolean) -> Unit,
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
                onNavigateToVerifyCode = { event ->
                    navController.navigate(VerifyCodeRoute(event.firstName, event.lastName, event.phone))
                },
                onAuthenticated = onAuthenticated,
            )
        }
        composable<VerifyCodeRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<VerifyCodeRoute>()
            VerifyCodeRoot(
                viewModel = koinViewModel<VerifyCodeViewModel> { parametersOf(route) },
                onNavigateBack = { navController.popBackStack() },
                onAuthenticated = onAuthenticated,
            )
        }
    }
}
