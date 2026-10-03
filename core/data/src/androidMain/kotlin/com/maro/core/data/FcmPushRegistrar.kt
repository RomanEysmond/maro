package com.maro.core.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.maro.core.domain.push.PushRegistrar
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/**
 * `users/{uid}/devices/{installationId}` = `{token, platform, updatedAt}`; the shape is enforced by `firestore.rules`.
 * The installation id is stable for the app install, so a rotated token overwrites its device instead of piling up.
 */
internal class FcmPushRegistrar(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val messaging: FirebaseMessaging,
    private val installations: FirebaseInstallations,
) : PushRegistrar {

    override suspend fun register(): EmptyResult<DataError.Network> {
        val uid = auth.currentUser?.uid ?: return Result.Error(DataError.Network.UNAUTHORIZED)
        return try {
            val token = messaging.token.await()
            device(uid, installations.id.await()).set(
                mapOf(
                    "token" to token,
                    "platform" to PLATFORM,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            Result.Error(
                if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) DataError.Network.FORBIDDEN
                else DataError.Network.NO_INTERNET,
            )
        } catch (e: Exception) {
            // Getting the FCM token fails without Play Services or without a network.
            Result.Error(DataError.Network.UNKNOWN)
        }
    }

    override suspend fun unregister() {
        val uid = auth.currentUser?.uid ?: return
        try {
            // Bounded: offline, Firestore would wait for the server forever and the sign-out with it.
            withTimeoutOrNull(UNREGISTER_TIMEOUT_MS) {
                device(uid, installations.id.await()).delete().await()
                // A fresh token next time: whoever signs in next on this device must not get this user's pushes.
                messaging.deleteToken().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Best effort: the server also drops tokens FCM reports as dead.
        }
    }

    private fun device(uid: String, installationId: String) =
        firestore.collection("users").document(uid).collection("devices").document(installationId)

    private companion object {
        const val PLATFORM = "android"
        const val UNREGISTER_TIMEOUT_MS = 3_000L
    }
}
