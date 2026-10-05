package com.maro.feature.chat.presentation.groupinfo

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.maro.core.domain.user.UserCard
import com.maro.core.domain.user.UserDirectory
import com.maro.core.domain.user.UserSearchError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import com.maro.feature.chat.domain.ChatHeader
import com.maro.feature.chat.domain.GroupError
import com.maro.feature.chat.domain.GroupRepository
import com.maro.feature.chat.presentation.FakeMessageRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class GroupInfoViewModelTest {

    private class FakeGroups : GroupRepository {
        var result: EmptyResult<GroupError> = Result.Success(Unit)
        val calls = mutableListOf<String>()

        override suspend fun rename(chatId: String, title: String): EmptyResult<GroupError> {
            calls += "rename:$title"
            return result
        }

        override suspend fun addMember(chatId: String, user: UserCard): EmptyResult<GroupError> {
            calls += "add:${user.id}"
            return result
        }

        override suspend fun leave(chatId: String): EmptyResult<GroupError> {
            calls += "leave"
            return result
        }
    }

    private class FakeDirectory : UserDirectory {
        var users: Map<String, UserCard> = emptyMap()

        override suspend fun findByUsername(username: String): Result<UserCard, UserSearchError> =
            users[username]?.let { Result.Success(it) } ?: Result.Error(UserSearchError.USER_NOT_FOUND)
    }

    private lateinit var messages: FakeMessageRepository
    private lateinit var groups: FakeGroups
    private lateinit var directory: FakeDirectory

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        messages = FakeMessageRepository().apply {
            headerFlow.value = ChatHeader("Поход", "П", isGroup = true, canManage = true, createdBy = "me")
        }
        groups = FakeGroups()
        directory = FakeDirectory().apply { users = mapOf("anna_p" to UserCard("anna", "Анна", "", "anna_p")) }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = GroupInfoViewModel("g", messages, groups, directory)

    @Test
    fun `the rename dialog starts with the current title and closes when saved`() {
        val viewModel = viewModel()

        viewModel.onAction(GroupInfoAction.OnRenameClick)
        assertThat(viewModel.state.value.renameDraft).isEqualTo("Поход")

        viewModel.onAction(GroupInfoAction.OnRenameChange("Сплав"))
        viewModel.onAction(GroupInfoAction.OnRenameConfirm)

        assertThat(groups.calls).isEqualTo(listOf("rename:Сплав"))
        assertThat(viewModel.state.value.renameDraft).isNull()
    }

    @Test
    fun `a failed rename keeps the dialog and shows why`() {
        groups.result = Result.Error(GroupError.NOT_ALLOWED)
        val viewModel = viewModel()
        viewModel.onAction(GroupInfoAction.OnRenameClick)

        viewModel.onAction(GroupInfoAction.OnRenameConfirm)

        assertThat(viewModel.state.value.renameDraft).isEqualTo("Поход")
        assertThat(viewModel.state.value.error).isNotNull()
    }

    @Test
    fun `a person found by username is added and the field is cleared`() {
        val viewModel = viewModel()

        viewModel.onAction(GroupInfoAction.OnAddQueryChange("@Anna_P"))
        viewModel.onAction(GroupInfoAction.OnAddClick)

        assertThat(groups.calls).isEqualTo(listOf("add:anna"))
        assertThat(viewModel.state.value.addQuery).isEqualTo("")
    }

    @Test
    fun `an unknown username is an error and nothing is added`() {
        val viewModel = viewModel()

        viewModel.onAction(GroupInfoAction.OnAddQueryChange("nobody_here"))
        viewModel.onAction(GroupInfoAction.OnAddClick)

        assertThat(groups.calls).isEqualTo(emptyList())
        assertThat(viewModel.state.value.error).isNotNull()
    }

    @Test
    fun `only the creator may add people`() {
        messages.headerFlow.value = ChatHeader("Поход", "П", isGroup = true, canManage = false, createdBy = "anna")
        val viewModel = viewModel()

        viewModel.onAction(GroupInfoAction.OnAddQueryChange("anna_p"))

        assertThat(viewModel.state.value.canAdd).isFalse()
    }

    @Test
    fun `leaving asks first, then reports that the user is out`() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(GroupInfoAction.OnLeaveClick)
        assertThat(viewModel.state.value.isLeaveConfirmVisible).isTrue()
        assertThat(groups.calls).isEqualTo(emptyList())

        viewModel.events.test {
            viewModel.onAction(GroupInfoAction.OnLeaveConfirm)
            assertThat(awaitItem()).isEqualTo(GroupInfoEvent.LeftGroup)
        }
        assertThat(groups.calls).isEqualTo(listOf("leave"))
    }
}
