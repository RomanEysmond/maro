package com.maro.core.data

import com.google.firebase.auth.FirebaseAuth
import com.maro.core.domain.auth.IdTokenProvider
import kotlin.coroutines.cancellation.CancellationException

internal class FirebaseIdTokenProvider(private val auth: FirebaseAuth) : IdTokenProvider {
    // forceRefresh = false: Firebase caches the token and renews it by itself shortly before it expires.
    override suspend fun idToken(): String? = try {
        auth.currentUser?.getIdToken(false)?.await()?.token
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
