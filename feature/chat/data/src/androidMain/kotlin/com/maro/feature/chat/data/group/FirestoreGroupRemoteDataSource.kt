package com.maro.feature.chat.data.group

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Transaction
import com.maro.core.data.await
import com.maro.core.domain.chat.GroupRules
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import kotlin.coroutines.cancellation.CancellationException

internal class FirestoreGroupRemoteDataSource(
    private val firestore: FirebaseFirestore,
) : GroupRemoteDataSource {

    /** Thrown inside a transaction to end it with a typed error instead of a write. */
    private class Refused(val error: DataError.Network) : Exception()

    override suspend fun rename(
        chatId: String,
        title: String,
        actorId: String,
        systemMessageId: String,
    ): EmptyResult<DataError.Network> = transaction(chatId) { transaction, chatRef ->
        val now = FieldValue.serverTimestamp()
        transaction.update(chatRef, mapOf("title" to title, "updatedAt" to now))
        transaction.setSystemMessage(chatRef, systemMessageId, actorId, kind = "renamed", title = title)
    }

    override suspend fun addMember(
        chatId: String,
        member: MemberCard,
        actorId: String,
        systemMessageId: String,
    ): EmptyResult<DataError.Network> = transaction(chatId) { transaction, chatRef ->
        val participants = transaction.get(chatRef).participants()
        if (member.id in participants) throw Refused(DataError.Network.CONFLICT)
        if (participants.size >= GroupRules.MAX_MEMBERS) throw Refused(DataError.Network.PAYLOAD_TOO_LARGE)

        transaction.update(
            chatRef,
            FieldPath.of("participants"), FieldValue.arrayUnion(member.id),
            FieldPath.of("participantInfo", member.id), buildMap {
                put("firstName", member.firstName)
                put("lastName", member.lastName)
                member.username?.let { put("username", it) }
            },
            FieldPath.of("updatedAt"), FieldValue.serverTimestamp(),
        )
        transaction.setSystemMessage(chatRef, systemMessageId, actorId, kind = "added", targets = listOf(member.id))
    }

    override suspend fun leave(
        chatId: String,
        actorId: String,
        systemMessageId: String,
    ): EmptyResult<DataError.Network> = transaction(chatId) { transaction, chatRef ->
        val chat = transaction.get(chatRef)
        val remaining = chat.participants().filter { it != actorId }
        val updates = buildMap<String, Any> {
            put("participants", remaining)
            put("updatedAt", FieldValue.serverTimestamp())
            // The group never stays without someone who can manage it: the earliest remaining member takes over.
            if (chat.getString("createdBy") == actorId && remaining.isNotEmpty()) put("createdBy", remaining.first())
        }
        // participantInfo keeps the leaver: the history still shows their name.
        transaction.update(chatRef, updates)
        transaction.setSystemMessage(chatRef, systemMessageId, actorId, kind = "left")
    }

    private suspend fun transaction(
        chatId: String,
        block: (Transaction, DocumentReference) -> Unit,
    ): EmptyResult<DataError.Network> {
        val chatRef = firestore.collection(CHATS).document(chatId)
        return try {
            firestore.runTransaction { transaction -> block(transaction, chatRef) }.await()
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e.toNetworkError())
        }
    }

    private fun Transaction.setSystemMessage(
        chatRef: DocumentReference,
        messageId: String,
        actorId: String,
        kind: String,
        targets: List<String> = emptyList(),
        title: String? = null,
    ) {
        set(
            chatRef.collection(MESSAGES).document(messageId),
            mapOf(
                "senderId" to actorId,
                "type" to "system",
                "event" to buildMap {
                    put("kind", kind)
                    put("targets", targets)
                    title?.let { put("title", it) }
                },
                "createdAt" to FieldValue.serverTimestamp(),
                "clientId" to messageId,
            ),
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.participants(): List<String> =
        (get("participants") as? List<*>)?.filterIsInstance<String>().orEmpty()

    private fun Exception.toNetworkError(): DataError.Network {
        // A Refused thrown inside the transaction arrives as the cause of the failed task.
        (this as? Refused ?: cause as? Refused)?.let { return it.error }
        if (this !is FirebaseFirestoreException) return DataError.Network.UNKNOWN
        return when (code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            -> DataError.Network.NO_INTERNET
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> DataError.Network.FORBIDDEN
            FirebaseFirestoreException.Code.NOT_FOUND -> DataError.Network.NOT_FOUND
            else -> DataError.Network.UNKNOWN
        }
    }

    private companion object {
        const val CHATS = "chats"
        const val MESSAGES = "messages"
    }
}
