package com.maro.feature.chat.data.group

import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult

/** What is written about a member on the group document (`participantInfo`). */
data class MemberCard(
    val id: String,
    val firstName: String,
    val lastName: String,
    val username: String?,
)

/**
 * Group changes on the server. Each is one transaction: the change of `chats/{chatId}` and its system message
 * `chats/{chatId}/messages/{systemMessageId}`; the shapes are enforced by `firestore.rules`.
 */
interface GroupRemoteDataSource {
    suspend fun rename(chatId: String, title: String, actorId: String, systemMessageId: String): EmptyResult<DataError.Network>

    /** [DataError.Network.CONFLICT] if [member] is in the group already, [DataError.Network.PAYLOAD_TOO_LARGE] if it is full. */
    suspend fun addMember(
        chatId: String,
        member: MemberCard,
        actorId: String,
        systemMessageId: String,
    ): EmptyResult<DataError.Network>

    /** Takes [actorId] out of the group; the creator's rights pass on to the earliest remaining member. */
    suspend fun leave(chatId: String, actorId: String, systemMessageId: String): EmptyResult<DataError.Network>
}
