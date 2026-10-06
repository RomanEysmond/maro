package com.maro.feature.chatlist.data

import com.maro.core.database.chat.ChatDao
import com.maro.core.domain.auth.CurrentUserProvider
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.asEmptyResult
import com.maro.core.domain.util.onSuccess
import com.maro.feature.chatlist.domain.Chat
import com.maro.feature.chatlist.domain.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultChatRepository(
    private val chatDao: ChatDao,
    private val remote: ChatRemoteDataSource,
    private val currentUser: CurrentUserProvider,
    // No lifecycle owner to scope this to yet (same as FirebaseSessionRepository's listener): lives with the
    // singleton for the process by default, same as every other always-on listener in the app so far.
    // Overridable so tests can drive it with a TestScope instead of a real background dispatcher.
    private val listenerScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : ChatRepository {

    // A StateFlow (not a plain Flow) so callers can read `.value` synchronously right after `sync()` returns,
    // instead of racing a separate collector of this same flow (see ChatListViewModel).
    // Follows the signed-in user: this singleton outlives a sign-out, and the next account in the same process must
    // get its own unread counts, not the previous user's.
    override val chats: StateFlow<List<Chat>> = currentUser.userIdFlow
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptyList())
            } else {
                chatDao.observeAllWithUnread(userId).map { rows -> rows.map { it.toDomain(userId) } }
            }
        }
        .stateIn(listenerScope, SharingStarted.Eagerly, emptyList())

    init {
        // Accelerator only: keeps Room fresh in the background. `sync()` is what the UI awaits and can retry.
        // Restarted for every signed-in user, stopped while nobody is: the query is bound to the uid.
        currentUser.userIdFlow
            .flatMapLatest { userId -> if (userId == null) emptyFlow() else remote.observeChats() }
            .onEach { result ->
                result.onSuccess { chats ->
                    chatDao.replaceAll(chats.map { it.toEntity() }, chats.flatMap { it.toMemberEntities() })
                }
            }
            .launchIn(listenerScope)
    }

    override suspend fun sync(): EmptyResult<DataError.Network> = remote.fetchChats()
        .onSuccess { chats ->
            chatDao.replaceAll(chats.map { it.toEntity() }, chats.flatMap { it.toMemberEntities() })
        }
        .asEmptyResult()
}
