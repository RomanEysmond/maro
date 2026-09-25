package com.maro.feature.chatlist.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.maro.core.data.await
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import com.maro.feature.chatlist.domain.FoundUser
import kotlin.coroutines.cancellation.CancellationException

/**
 * Looks people up in `usernames/{name}` (the public card: uid + name) and creates `chats/{chatId}`;
 * both shapes are enforced by `firestore.rules`.
 */
internal class FirestoreNewChatRemoteDataSource(
    private val firestore: FirebaseFirestore,
) : NewChatRemoteDataSource {

    override suspend fun findUser(username: String): Result<FoundUser?, DataError.Network> {
        return try {
            // Source.SERVER for the same reason as the chat list: offline must be an error, not "nobody found".
            val card = firestore.collection(USERNAMES).document(username).get(Source.SERVER).await()
            if (!card.exists()) return Result.Success(null)
            Result.Success(
                FoundUser(
                    id = card.getString("uid") ?: return Result.Success(null),
                    // A card written before names were public has no name yet: the owner fixes it by saving the profile.
                    firstName = card.getString("firstName") ?: return Result.Success(null),
                    lastName = card.getString("lastName").orEmpty(),
                    username = username,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e.toNetworkError())
        }
    }

    override suspend fun createChatIfAbsent(
        chatId: String,
        participants: List<ParticipantCard>,
    ): EmptyResult<DataError.Network> {
        val chatRef = firestore.collection(CHATS).document(chatId)
        return try {
            firestore.runTransaction { transaction ->
                if (transaction.get(chatRef).exists()) return@runTransaction true

                val now = FieldValue.serverTimestamp()
                transaction.set(
                    chatRef,
                    mapOf(
                        "type" to "direct",
                        "participants" to participants.map { it.id },
                        "participantInfo" to participants.associate { card ->
                            card.id to buildMap<String, Any> {
                                put("firstName", card.firstName)
                                put("lastName", card.lastName)
                                card.username?.let { put("username", it) }
                            }
                        },
                        "updatedAt" to now,
                        "createdAt" to now,
                    ),
                )
                true
            }.await()
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e.toNetworkError())
        }
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
        const val USERNAMES = "usernames"
        const val CHATS = "chats"
    }
}
