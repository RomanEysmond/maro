package com.maro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maro.feature.auth.domain.SessionRepository
import com.maro.feature.chat.domain.MessageRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** App-level session state: decides the start destination, reacts to sign-out and keeps chats synced meanwhile. */
class MainViewModel(
    private val sessionRepository: SessionRepository,
    private val messageRepository: MessageRepository,
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = sessionRepository.isLoggedIn

    init {
        viewModelScope.launch {
            // While signed in (and the app is open) every chat with new messages is caught up in the background,
            // so a conversation opens with its messages already there and can be read offline later.
            isLoggedIn.collectLatest { loggedIn ->
                if (loggedIn) messageRepository.keepAllChatsInSync()
            }
        }
    }

    fun onLogoutClick() {
        viewModelScope.launch { sessionRepository.signOut() }
    }
}
