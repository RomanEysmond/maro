package com.maro.feature.chatlist.data

import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult

/** What is written about a participant on the chat document itself. */
data class ParticipantCard(
    val id: String,
    val firstName: String,
    val lastName: String,
    val username: String?,
)

interface NewChatRemoteDataSource {
    /** Creates `chats/{chatId}` unless it exists already. [participants] are the two cards, ordered by id. */
    suspend fun createChatIfAbsent(chatId: String, participants: List<ParticipantCard>): EmptyResult<DataError.Network>

    /**
     * Creates the group `chats/{chatId}` with [participants] (the creator first) together with its first, system
     * message `chats/{chatId}/messages/{systemMessageId}` ("… created the group"), in one write.
     */
    suspend fun createGroup(
        chatId: String,
        title: String,
        creatorId: String,
        participants: List<ParticipantCard>,
        systemMessageId: String,
    ): EmptyResult<DataError.Network>
}
