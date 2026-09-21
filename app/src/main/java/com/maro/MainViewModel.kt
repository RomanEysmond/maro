package com.maro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maro.feature.auth.domain.SessionRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** App-level session state: decides the start destination and reacts to sign-out. */
class MainViewModel(
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = sessionRepository.isLoggedIn

    fun onLogoutClick() {
        viewModelScope.launch { sessionRepository.signOut() }
    }
}
