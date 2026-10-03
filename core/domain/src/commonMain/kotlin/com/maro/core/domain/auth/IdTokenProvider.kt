package com.maro.core.domain.auth

/** Proves to our own servers who the signed-in user is (Firebase: an ID token the server verifies). */
interface IdTokenProvider {
    /** A current ID token, or `null` when nobody is signed in or it could not be obtained. */
    suspend fun idToken(): String?
}
