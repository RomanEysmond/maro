package com.maro.feature.profile.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.maro.core.data.await
import com.maro.core.domain.profile.BirthDate
import com.maro.core.domain.profile.ProfileError
import com.maro.core.domain.profile.ProfileUpdate
import com.maro.core.domain.profile.UserProfile
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import kotlin.coroutines.cancellation.CancellationException

/**
 * Firestore documents `users/{uid}` and `usernames/{username}` (-> uid + name: makes usernames unique and is the public card).
 * Their shape is enforced by `firestore.rules`.
 */
internal class FirestoreUserProfileRemoteDataSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : UserProfileRemoteDataSource {

    override suspend fun fetchCurrent(): Result<UserProfile?, DataError.Network> {
        val uid = auth.currentUser?.uid ?: return Result.Error(DataError.Network.UNAUTHORIZED)
        return try {
            Result.Success(firestore.collection(USERS).document(uid).get().await().toProfile())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e.toNetworkError())
        }
    }

    override suspend fun createCurrent(
        firstName: String,
        lastName: String,
        phone: String,
    ): Result<UserProfile, DataError.Network> {
        val uid = auth.currentUser?.uid ?: return Result.Error(DataError.Network.UNAUTHORIZED)
        return try {
            val data = mapOf(
                "firstName" to firstName,
                "lastName" to lastName,
                "phone" to phone,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            firestore.collection(USERS).document(uid).set(data).await()
            Result.Success(UserProfile(uid, firstName, lastName, phone))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e.toNetworkError())
        }
    }

    override suspend fun updateCurrent(update: ProfileUpdate): EmptyResult<ProfileError> {
        val uid = auth.currentUser?.uid ?: return Result.Error(ProfileError.UNKNOWN)
        return try {
            val userRef = firestore.collection(USERS).document(uid)
            val isFree = firestore.runTransaction { transaction ->
                val oldUsername = transaction.get(userRef).getString("username")
                val newUsername = update.username

                if (newUsername != null) {
                    val claim = firestore.collection(USERNAMES).document(newUsername)
                    // A failed check returns without writing anything, so nothing is committed.
                    if (newUsername != oldUsername && transaction.get(claim).exists()) return@runTransaction false
                    // The public card: what anybody who finds this @username may see (their uid and name).
                    // Written on every save, not only when the username changes, so a renamed person is
                    // never found under their old name.
                    transaction.set(
                        claim,
                        mapOf("uid" to uid, "firstName" to update.firstName, "lastName" to update.lastName),
                    )
                }
                if (oldUsername != null && oldUsername != newUsername) {
                    transaction.delete(firestore.collection(USERNAMES).document(oldUsername))
                }

                transaction.update(
                    userRef,
                    mapOf(
                        "firstName" to update.firstName,
                        "lastName" to update.lastName,
                        "username" to (newUsername ?: FieldValue.delete()),
                        "bio" to update.bio,
                        "birthDate" to (update.birthDate?.toIsoString() ?: FieldValue.delete()),
                    ),
                )
                true
            }.await()

            if (isFree) Result.Success(Unit) else Result.Error(ProfileError.USERNAME_TAKEN)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(if (e.toNetworkError() == DataError.Network.NO_INTERNET) ProfileError.NO_INTERNET else ProfileError.UNKNOWN)
        }
    }

    private fun DocumentSnapshot.toProfile(): UserProfile? {
        if (!exists()) return null
        return UserProfile(
            id = id,
            firstName = getString("firstName") ?: return null,
            lastName = getString("lastName").orEmpty(),
            phone = getString("phone").orEmpty(),
            username = getString("username"),
            bio = getString("bio").orEmpty(),
            birthDate = getString("birthDate")?.let(BirthDate::parse),
        )
    }

    private fun Exception.toNetworkError(): DataError.Network {
        if (this !is FirebaseFirestoreException) return DataError.Network.UNKNOWN
        return when (code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            -> DataError.Network.NO_INTERNET
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> DataError.Network.FORBIDDEN
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> DataError.Network.UNAUTHORIZED
            else -> DataError.Network.UNKNOWN
        }
    }

    private companion object {
        const val USERS = "users"
        const val USERNAMES = "usernames"
    }
}
