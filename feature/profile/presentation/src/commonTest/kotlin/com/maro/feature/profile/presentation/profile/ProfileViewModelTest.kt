package com.maro.feature.profile.presentation.profile

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.maro.core.domain.profile.UserProfile
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.Result
import com.maro.feature.profile.presentation.fakes.FakeUserProfileRepository
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
class ProfileViewModelTest {

    private val sampleProfile = UserProfile("uid", "Иван", "Иванов", "+79001234567")

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `profile is loaded on start`() {
        val repository = FakeUserProfileRepository().apply { profileAfterRefresh = sampleProfile }

        val state = ProfileViewModel(repository).state.value

        assertThat(repository.refreshCalls).isEqualTo(1)
        assertThat(state.profile).isEqualTo(sampleProfile)
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun `load failure without a profile is shown`() {
        val repository = FakeUserProfileRepository().apply {
            refreshResult = Result.Error(DataError.Network.NO_INTERNET)
        }

        val state = ProfileViewModel(repository).state.value

        assertThat(state.profile).isNull()
        assertThat(state.error).isNotNull()
    }

    @Test
    fun `load failure keeps the known profile and hides the error`() {
        val repository = FakeUserProfileRepository(sampleProfile).apply {
            refreshResult = Result.Error(DataError.Network.NO_INTERNET)
        }

        val state = ProfileViewModel(repository).state.value

        assertThat(state.profile).isEqualTo(sampleProfile)
        assertThat(state.error).isNull()
    }

    @Test
    fun `retry loads the profile again`() {
        val repository = FakeUserProfileRepository().apply {
            refreshResult = Result.Error(DataError.Network.NO_INTERNET)
        }
        val viewModel = ProfileViewModel(repository)

        repository.refreshResult = Result.Success(Unit)
        repository.profileAfterRefresh = sampleProfile
        viewModel.onAction(ProfileAction.OnRetryClick)

        assertThat(repository.refreshCalls).isEqualTo(2)
        assertThat(viewModel.state.value.profile).isEqualTo(sampleProfile)
        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `edit click opens the edit screen`() = runTest {
        val viewModel = ProfileViewModel(FakeUserProfileRepository(sampleProfile))

        viewModel.events.test {
            viewModel.onAction(ProfileAction.OnEditClick)

            assertThat(awaitItem()).isEqualTo(ProfileEvent.NavigateToEdit)
        }
    }
}
