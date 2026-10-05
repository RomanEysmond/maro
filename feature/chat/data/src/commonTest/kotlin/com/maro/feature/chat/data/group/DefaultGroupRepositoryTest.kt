package com.maro.feature.chat.data.group

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.maro.core.domain.auth.CurrentUserProvider
import com.maro.core.domain.user.UserCard
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import com.maro.feature.chat.domain.GroupError
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class DefaultGroupRepositoryTest {

    private class FakeGroupRemote : GroupRemoteDataSource {
        var result: EmptyResult<DataError.Network> = Result.Success(Unit)
        var lastCall: List<Any>? = null

        override suspend fun rename(chatId: String, title: String, actorId: String, systemMessageId: String) =
            record(listOf("rename", chatId, title, actorId, systemMessageId))

        override suspend fun addMember(chatId: String, member: MemberCard, actorId: String, systemMessageId: String) =
            record(listOf("add", chatId, member, actorId, systemMessageId))

        override suspend fun leave(chatId: String, actorId: String, systemMessageId: String) =
            record(listOf("leave", chatId, actorId, systemMessageId))

        private fun record(call: List<Any>): EmptyResult<DataError.Network> {
            lastCall = call
            return result
        }
    }

    private val remote = FakeGroupRemote()
    private val me = object : CurrentUserProvider {
        override val userId: String = "me"
    }
    private val repository = DefaultGroupRepository(remote, me, newId = { "msg-1" })

    @Test
    fun `renaming trims the title and writes a system message as the user`() = runTest {
        assertThat(repository.rename("g", "  Новое имя ")).isEqualTo(Result.Success(Unit))
        assertThat(remote.lastCall).isEqualTo(listOf("rename", "g", "Новое имя", "me", "msg-1"))
    }

    @Test
    fun `an empty title never reaches the server`() = runTest {
        assertThat(repository.rename("g", "   ")).isEqualTo(Result.Error(GroupError.INVALID_TITLE))
        assertThat(remote.lastCall).isNull()
    }

    @Test
    fun `adding passes the person's card on`() = runTest {
        repository.addMember("g", UserCard("anna", "Анна", "Петрова", "anna_p"))

        assertThat(remote.lastCall).isEqualTo(listOf("add", "g", MemberCard("anna", "Анна", "Петрова", "anna_p"), "me", "msg-1"))
    }

    @Test
    fun `server answers become group errors`() = runTest {
        val anna = UserCard("anna", "Анна", "", "anna_p")

        remote.result = Result.Error(DataError.Network.FORBIDDEN)
        assertThat(repository.rename("g", "X")).isEqualTo(Result.Error(GroupError.NOT_ALLOWED))

        remote.result = Result.Error(DataError.Network.CONFLICT)
        assertThat(repository.addMember("g", anna)).isEqualTo(Result.Error(GroupError.ALREADY_MEMBER))

        remote.result = Result.Error(DataError.Network.PAYLOAD_TOO_LARGE)
        assertThat(repository.addMember("g", anna)).isEqualTo(Result.Error(GroupError.TOO_MANY_MEMBERS))

        remote.result = Result.Error(DataError.Network.NO_INTERNET)
        assertThat(repository.leave("g")).isEqualTo(Result.Error(GroupError.NO_INTERNET))
    }

    @Test
    fun `adding yourself is refused locally`() = runTest {
        assertThat(repository.addMember("g", UserCard("me", "Я", "", "me_me"))).isEqualTo(Result.Error(GroupError.ALREADY_MEMBER))
        assertThat(remote.lastCall).isNull()
    }
}
