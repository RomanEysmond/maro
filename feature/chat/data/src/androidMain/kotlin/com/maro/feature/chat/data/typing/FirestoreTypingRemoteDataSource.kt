package com.maro.feature.chat.data.typing

import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.maro.core.data.await
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** `chats/{chatId}/typing/{uid}` = `{typing, at}`; only the user writes their own document (see `firestore.rules`). */
internal class FirestoreTypingRemoteDataSource(private val firestore: FirebaseFirestore) : TypingRemoteDataSource {

    override fun observeTyping(chatId: String): Flow<List<RemoteTyping>> = callbackFlow {
        val registration = typing(chatId).addSnapshotListener { snapshot, _ ->
            // Errors are ignored on purpose: without typing indicators the chat works just the same.
            if (snapshot == null || snapshot.metadata.isFromCache) return@addSnapshotListener
            trySend(
                snapshot.documents.mapNotNull { document ->
                    RemoteTyping(
                        userId = document.id,
                        isTyping = document.getBoolean(FIELD_TYPING) ?: return@mapNotNull null,
                        atMillis = document.getTimestamp(FIELD_AT, ServerTimestampBehavior.ESTIMATE)
                            ?.toDate()?.time ?: return@mapNotNull null,
                    )
                },
            )
        }
        awaitClose { registration.remove() }
    }

    override suspend fun setTyping(chatId: String, userId: String, isTyping: Boolean): EmptyResult<DataError.Network> =
        try {
            typing(chatId).document(userId)
                .set(mapOf(FIELD_TYPING to isTyping, FIELD_AT to FieldValue.serverTimestamp()))
                .await()
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(DataError.Network.UNKNOWN)
        }

    private fun typing(chatId: String) = firestore.collection("chats").document(chatId).collection("typing")

    private companion object {
        const val FIELD_TYPING = "typing"
        const val FIELD_AT = "at"
    }
}
