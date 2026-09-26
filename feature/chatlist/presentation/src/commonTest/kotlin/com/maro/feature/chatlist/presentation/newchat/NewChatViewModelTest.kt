package com.maro.feature.chatlist.presentation.newchat

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
import com.maro.feature.chatlist.domain.NewChatRepository
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
class NewChatViewModelTest {

    private class FakeNewChatRepository : NewChatRepository {
        var findResult: Result<FoundUser, NewChatError> = Result.Error(NewChatError.USER_NOT_FOUND)
        var startResult: Result<String, NewChatError> = Result.Success("chat-1")
        val lookups = mutableListOf<String>()
        var started: FoundUser? = null

        override suspend fun findUser(username: String): Result<FoundUser, NewChatError> {
            lookups += username
            return findResult
        }

        override suspend fun startChat(user: FoundUser): Result<String, NewChatError> {
            started = user
            return startResult
        }
    }

    private val anna = FoundUser("uid-a", "Анна", "Петрова", "anna_p")
    private lateinit var repository: FakeNewChatRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeNewChatRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = NewChatViewModel(repository)

    @Test
    fun `the query keeps only what a username may contain`() {
        val viewModel = viewModel()

        viewModel.onAction(NewChatAction.OnQueryChange(" @Anna-P!_1 "))

        assertThat(viewModel.state.value.query).isEqualTo("annap_1")
    }

    @Test
    fun `search needs a query`() {
        val viewModel = viewModel()
        assertThat(viewModel.state.value.canSearch).isFalse()

        viewModel.onAction(NewChatAction.OnSearchClick)
        assertThat(repository.lookups).isEqualTo(emptyList())

        viewModel.onAction(NewChatAction.OnQueryChange("anna_p"))
        assertThat(viewModel.state.value.canSearch).isTrue()
    }

    @Test
    fun `a found user is shown`() {
        repository.findResult = Result.Success(anna)
        val viewModel = viewModel()
        viewModel.onAction(NewChatAction.OnQueryChange("anna_p"))

        viewModel.onAction(NewChatAction.OnSearchClick)

        assertThat(repository.lookups).isEqualTo(listOf("anna_p"))
        assertThat(viewModel.state.value.foundUser).isEqualTo(anna)
        assertThat(viewModel.state.value.isSearching).isFalse()
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `a failed search shows the error and no user`() {
        val viewModel = viewModel()
        viewModel.onAction(NewChatAction.OnQueryChange("nobody_here"))

        viewModel.onAction(NewChatAction.OnSearchClick)

        assertThat(viewModel.state.value.foundUser).isNull()
        assertThat(viewModel.state.value.error).isNotNull()
    }

    @Test
    fun `typing again clears the previous result and error`() {
        repository.findResult = Result.Success(anna)
        val viewModel = viewModel()
        viewModel.onAction(NewChatAction.OnQueryChange("anna_p"))
        viewModel.onAction(NewChatAction.OnSearchClick)

        viewModel.onAction(NewChatAction.OnQueryChange("anna_pe"))

        assertThat(viewModel.state.value.foundUser).isNull()
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `tapping the found user opens the conversation`() = runTest {
        repository.findResult = Result.Success(anna)
        val viewModel = viewModel()
        viewModel.onAction(NewChatAction.OnQueryChange("anna_p"))
        viewModel.onAction(NewChatAction.OnSearchClick)

        viewModel.events.test {
            viewModel.onAction(NewChatAction.OnUserClick)

            assertThat(awaitItem()).isEqualTo(NewChatEvent.NavigateToChat("chat-1"))
        }
        assertThat(repository.started).isEqualTo(anna)
    }

    @Test
    fun `a failure to start the chat is shown and nothing opens`() = runTest {
        repository.findResult = Result.Success(anna)
        repository.startResult = Result.Error(NewChatError.NO_INTERNET)
        val viewModel = viewModel()
        viewModel.onAction(NewChatAction.OnQueryChange("anna_p"))
        viewModel.onAction(NewChatAction.OnSearchClick)

        viewModel.events.test {
            viewModel.onAction(NewChatAction.OnUserClick)

            expectNoEvents()
        }
        assertThat(viewModel.state.value.error).isNotNull()
        assertThat(viewModel.state.value.isStarting).isFalse()
    }

    @Test
    fun `tapping with nobody found does nothing`() {
        val viewModel = viewModel()

        viewModel.onAction(NewChatAction.OnUserClick)

        assertThat(repository.started).isNull()
    }

    @Test
    fun `back emits NavigateBack`() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(NewChatAction.OnBackClick)

            assertThat(awaitItem()).isEqualTo(NewChatEvent.NavigateBack)
        }
    }
}
