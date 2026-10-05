package com.maro.core.data

import com.google.firebase.auth.FirebaseAuth
import com.maro.core.domain.auth.IdTokenProvider
import kotlin.coroutines.cancellation.CancellationException

internal class FirebaseIdTokenProvider(private val auth: FirebaseAuth) : IdTokenProvider {
    // Without forceRefresh Firebase serves its cached token and renews it shortly before it expires.
    override suspend fun idToken(forceRefresh: Boolean): String? = try {
        auth.currentUser?.getIdToken(forceRefresh)?.await()?.token
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
