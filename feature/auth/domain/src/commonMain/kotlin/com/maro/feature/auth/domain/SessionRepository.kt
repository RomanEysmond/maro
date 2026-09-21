package com.maro.feature.auth.domain

import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    /** Has a value from the very first read, so the start destination can be chosen without a splash screen. */
    val isLoggedIn: StateFlow<Boolean>

    suspend fun signOut()
}
