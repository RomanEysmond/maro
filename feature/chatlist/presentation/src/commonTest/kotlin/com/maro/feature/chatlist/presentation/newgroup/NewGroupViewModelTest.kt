package com.maro.feature.chatlist.presentation.newgroup

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.maro.core.domain.util.Result
import com.maro.feature.chatlist.domain.FoundUser
import com.maro.feature.chatlist.domain.NewChatError
import com.maro.feature.chatlist.presentation.fakes.FakeNewChatRepository
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
class NewGroupViewModelTest {

    private val anna = FoundUser("uid-a", "Анна", "Петрова", "anna_p")
    private val petr = FoundUser("uid-p", "Пётр", "", "petr_s")
    private lateinit var repository: FakeNewChatRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeNewChatRepository().apply { users = mapOf("anna_p" to anna, "petr_s" to petr) }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = NewGroupViewModel(repository)

    private fun NewGroupViewModel.add(username: String) {
        onAction(NewGroupAction.OnQueryChange(username))
        onAction(NewGroupAction.OnAddClick)
    }

    @Test
    fun `people are added one by one and the field is cleared for the next`() {
        val viewModel = viewModel()

        viewModel.add("@Anna_P")
        viewModel.add("petr_s")

        assertThat(viewModel.state.value.members).isEqualTo(listOf(anna, petr))
        assertThat(viewModel.state.value.query).isEqualTo("")
    }

    @Test
    fun `the same person is not added twice and an unknown one is an error`() {
        val viewModel = viewModel()
        viewModel.add("anna_p")

        viewModel.add("anna_p")
        assertThat(viewModel.state.value.members).isEqualTo(listOf(anna))
        assertThat(viewModel.state.value.error).isNotNull()

        viewModel.add("nobody_here")
        assertThat(viewModel.state.value.members).isEqualTo(listOf(anna))
        assertThat(viewModel.state.value.error).isNotNull()
    }

    @Test
    fun `a group can be created only with a title and at least one person`() {
        val viewModel = viewModel()
        assertThat(viewModel.state.value.canCreate).isFalse()

        viewModel.onAction(NewGroupAction.OnTitleChange("Поход"))
        assertThat(viewModel.state.value.canCreate).isFalse()

        viewModel.add("anna_p")
        assertThat(viewModel.state.value.canCreate).isTrue()

        viewModel.onAction(NewGroupAction.OnRemoveClick(anna.id))
        assertThat(viewModel.state.value.canCreate).isFalse()
    }

    @Test
    fun `creating opens the new group`() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(NewGroupAction.OnTitleChange("Поход"))
        viewModel.add("anna_p")

        viewModel.events.test {
            viewModel.onAction(NewGroupAction.OnCreateClick)

            assertThat(awaitItem()).isEqualTo(NewGroupEvent.NavigateToChat("group-1"))
        }
        assertThat(repository.createdGroup).isEqualTo("Поход" to listOf(anna))
    }

    @Test
    fun `a failed creation is shown and the form stays`() {
        repository.createResult = Result.Error(NewChatError.NO_INTERNET)
        val viewModel = viewModel()
        viewModel.onAction(NewGroupAction.OnTitleChange("Поход"))
        viewModel.add("anna_p")

        viewModel.onAction(NewGroupAction.OnCreateClick)

        assertThat(viewModel.state.value.error).isNotNull()
        assertThat(viewModel.state.value.isCreating).isFalse()
        assertThat(viewModel.state.value.members).isEqualTo(listOf(anna))
    }

    @Test
    fun `the title is capped at the limit`() {
        val viewModel = viewModel()

        viewModel.onAction(NewGroupAction.OnTitleChange("а".repeat(100)))

        assertThat(viewModel.state.value.title.length).isEqualTo(64)
        assertThat(viewModel.state.value.error).isNull()
    }
}
