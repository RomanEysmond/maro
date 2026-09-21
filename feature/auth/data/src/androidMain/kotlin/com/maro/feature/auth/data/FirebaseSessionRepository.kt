package com.maro.feature.auth.data

import com.google.firebase.auth.FirebaseAuth
import com.maro.feature.auth.domain.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Firebase Auth persists the session itself; this only exposes it as a flow. */
internal class FirebaseSessionRepository(
    private val auth: FirebaseAuth,
) : SessionRepository {

    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        // Registered once for the lifetime of the process (the repository is a singleton).
        auth.addAuthStateListener { _isLoggedIn.value = it.currentUser != null }
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}
