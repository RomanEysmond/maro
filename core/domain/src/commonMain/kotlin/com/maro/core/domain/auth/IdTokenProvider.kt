package com.maro.core.domain.auth

/** Proves to our own servers who the signed-in user is (Firebase: an ID token the server verifies). */
interface IdTokenProvider {
    /**
     * A current ID token, or `null` when nobody is signed in or it could not be obtained. [forceRefresh] fetches a
     * new one even if the cached one looks valid: the device decides "valid" by its own clock, which may be off.
     */
    suspend fun idToken(forceRefresh: Boolean = false): String?
}
