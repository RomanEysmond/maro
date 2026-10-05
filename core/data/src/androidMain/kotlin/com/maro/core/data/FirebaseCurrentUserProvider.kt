package com.maro.core.data

import com.google.firebase.auth.FirebaseAuth
import com.maro.core.domain.auth.CurrentUserProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FirebaseCurrentUserProvider(private val auth: FirebaseAuth) : CurrentUserProvider {

    private val _userIdFlow = MutableStateFlow(auth.currentUser?.uid)
    override val userIdFlow: StateFlow<String?> = _userIdFlow.asStateFlow()

    override val userId: String? get() = auth.currentUser?.uid

    init {
        // A singleton: the listener lives with the process, like FirebaseSessionRepository's.
        auth.addAuthStateListener { _userIdFlow.value = it.currentUser?.uid }
    }
}
