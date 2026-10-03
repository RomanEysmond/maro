package com.maro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maro.core.domain.push.PushRegistrar
import com.maro.core.domain.util.Result
import com.maro.feature.auth.domain.SessionRepository
import com.maro.feature.chat.domain.MessageRepository
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** App-level session state: decides the start destination, reacts to sign-out and keeps chats synced meanwhile. */
class MainViewModel(
    private val sessionRepository: SessionRepository,
    private val messageRepository: MessageRepository,
    private val pushRegistrar: PushRegistrar,
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = sessionRepository.isLoggedIn

    init {
        viewModelScope.launch {
            // While signed in (and the app is open) every chat with new messages is caught up in the background,
            // so a conversation opens with its messages already there and can be read offline later.
            isLoggedIn.collectLatest { loggedIn ->
                if (!loggedIn) return@collectLatest
                // On every start, not only at sign-in: keeps this device's push token current on the server.
                // Retried until it works: right after boot there is often no network yet, and a device that never
                // registered would silently get no pushes.
                launch {
                    while (pushRegistrar.register() is Result.Error) delay(REGISTER_RETRY_DELAY)
                }
                messageRepository.keepAllChatsInSync()
            }
        }
    }

    fun onLogoutClick() {
        viewModelScope.launch {
            // While still signed in: removing the device needs the user's own rights.
            pushRegistrar.unregister()
            sessionRepository.signOut()
        }
    }

    private companion object {
        val REGISTER_RETRY_DELAY = 30.seconds
    }
}
