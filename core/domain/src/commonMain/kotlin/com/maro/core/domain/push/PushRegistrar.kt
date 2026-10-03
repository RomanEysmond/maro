package com.maro.core.domain.push

import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult

/**
 * Tells the backend where to deliver pushes for the signed-in user on this device (Android: the FCM token, stored as
 * `users/{uid}/devices/{installationId}`). The push itself carries ids only; see the push server.
 */
interface PushRegistrar {
    /** Idempotent; called on every sign-in / app start and whenever the platform rotates the token. */
    suspend fun register(): EmptyResult<DataError.Network>

    /** Before signing out: this device stops receiving the user's pushes. Best effort. */
    suspend fun unregister()
}
