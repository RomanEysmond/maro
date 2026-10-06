package com.maro.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.maro.LogoutConfirmation
import com.maro.MainViewModel
import com.maro.R
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
import com.maro.notifications.MessageNotifications
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Assembles the navigation graphs of all features. Features never import each other's routes:
 * cross-feature navigation is expressed here as callbacks.
 */
@Composable
fun NavigationRoot(
    openChatRequest: String?,
    onOpenChatRequestHandled: () -> Unit,
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = koinViewModel(),
    notifications: MessageNotifications = koinInject(),
) {
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsState()
    val logoutConfirmation by mainViewModel.logoutConfirmation.collectAsState()

    NotificationPermissionRequest(isLoggedIn = isLoggedIn)

    logoutConfirmation?.let { confirmation ->
        LogoutDialog(
            confirmation = confirmation,
            onConfirm = mainViewModel::onLogoutConfirm,
            onDismiss = mainViewModel::onLogoutDismiss,
        )
    }

    val openChat: (String) -> Unit = { chatId ->
        // Opening the chat makes its notification obsolete, however the chat was opened.
        notifications.cancel(chatId)
        navController.navigate(ChatRoute(chatId)) { launchSingleTop = true }
    }

    // A tapped message notification. Ignored when signed out: the login flow comes first.
    LaunchedEffect(openChatRequest, isLoggedIn) {
        val chatId = openChatRequest ?: return@LaunchedEffect
        if (isLoggedIn) openChat(chatId)
        onOpenChatRequestHandled()
    }
    // The session is known synchronously, so the start destination is chosen once and there is no splash screen.
    val startDestination = remember { if (mainViewModel.isLoggedIn.value) ChatListGraphRoute else AuthGraphRoute }

    // Signing out (from any screen, or the session ending) sends the user back to the login flow
    // with a clean back stack.
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
            onOpenChat = openChat,
            onOpenProfile = { navController.navigate(ProfileRoute) },
            onOpenSettings = { navController.navigate(SettingsRoute) },
            onOpenHelp = { navController.navigate(HelpRoute) },
        )
        chatGraph(
            navController = navController,
            onNavigateBack = { navController.popBackStack() },
        )
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

/** "Log out?", plus how many messages would be lost when some have not reached the server. */
@Composable
private fun LogoutDialog(confirmation: LogoutConfirmation, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.logout_title)) },
        text = if (confirmation.unsentMessages > 0) {
            {
                Text(
                    pluralStringResource(
                        R.plurals.logout_unsent_messages,
                        confirmation.unsentMessages,
                        confirmation.unsentMessages,
                    ),
                )
            }
        } else {
            null
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.logout_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.logout_cancel)) }
        },
    )
}

/** Android 13+: message notifications need a runtime permission; asked once the user is signed in. */
@Composable
private fun NotificationPermissionRequest(isLoggedIn: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    // The answer needs no handling: without the permission the notifications are simply not shown.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(isLoggedIn) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        // After two refusals Android stops showing the dialog by itself, so asking on each sign-in does not nag.
        if (isLoggedIn && !granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
