package com.maro.core.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.maro.core.domain.profile.ProfileRules
import com.maro.core.domain.user.UserCard
import com.maro.core.domain.user.UserDirectory
import com.maro.core.domain.user.UserSearchError
import com.maro.core.domain.util.Result
import kotlin.coroutines.cancellation.CancellationException

/** Looks people up in `usernames/{name}`: the public card (uid + name), whose shape `firestore.rules` enforces. */
internal class FirestoreUserDirectory(private val firestore: FirebaseFirestore) : UserDirectory {

    override suspend fun findByUsername(username: String): Result<UserCard, UserSearchError> {
        val normalized = ProfileRules.normalizeUsername(username)
        if (!ProfileRules.isValidUsername(normalized)) return Result.Error(UserSearchError.INVALID_USERNAME)
        return try {
            // Source.SERVER: offline must be an error, not "nobody found".
            val card = firestore.collection(USERNAMES).document(normalized).get(Source.SERVER).await()
            if (!card.exists()) return Result.Error(UserSearchError.USER_NOT_FOUND)
            Result.Success(
                UserCard(
                    id = card.getString("uid") ?: return Result.Error(UserSearchError.USER_NOT_FOUND),
                    // A card written before names were public has no name yet:
                    // the owner fixes it by saving the profile.
                    firstName = card.getString("firstName") ?: return Result.Error(UserSearchError.USER_NOT_FOUND),
                    lastName = card.getString("lastName").orEmpty(),
                    username = normalized,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            Result.Error(
                when (e.code) {
                    FirebaseFirestoreException.Code.UNAVAILABLE,
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                    -> UserSearchError.NO_INTERNET

                    else -> UserSearchError.UNKNOWN
                },
            )
        } catch (e: Exception) {
            Result.Error(UserSearchError.UNKNOWN)
        }
    }

    private companion object {
        const val USERNAMES = "usernames"
    }
}
