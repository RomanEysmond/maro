package com.maro.core.domain.session

/**
 * Something that keeps the signed-in user's data on the device and must forget it on sign-out, so the next account
 * on this device starts clean: the local database, queued work, shown notifications. Each layer registers its own in
 * Koin; the app runs all of them after signing out.
 */
fun interface SignOutCleaner {
    suspend fun clear()
}
