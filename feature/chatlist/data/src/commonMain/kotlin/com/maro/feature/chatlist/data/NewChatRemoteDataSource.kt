package com.maro.feature.chatlist.data

import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import com.maro.feature.chatlist.domain.FoundUser

/** What is written about a participant on the chat document itself. */
data class ParticipantCard(
    val id: String,
    val firstName: String,
    val lastName: String,
    val username: String?,
)

interface NewChatRemoteDataSource {
    /** `null` in the success case means nobody has this username. [username] is already normalized. */
    suspend fun findUser(username: String): Result<FoundUser?, DataError.Network>

    /** Creates `chats/{chatId}` unless it exists already. [participants] are the two cards, ordered by id. */
    suspend fun createChatIfAbsent(chatId: String, participants: List<ParticipantCard>): EmptyResult<DataError.Network>
}
