package com.maro.feature.profile.presentation.edit

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.maro.core.domain.profile.BirthDate
import com.maro.core.domain.profile.ProfileError
import com.maro.core.domain.profile.ProfileUpdate
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
class EditProfileViewModelTest {

    private val existing = UserProfile(
        id = "uid",
        firstName = "Иван",
        lastName = "Иванов",
        phone = "+79001234567",
        username = "ivan_i",
        bio = "Привет",
        birthDate = BirthDate(1990, 5, 7),
    )
    private lateinit var repository: FakeUserProfileRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeUserProfileRepository(existing)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(mode: EditProfileMode = EditProfileMode.EDIT) = EditProfileViewModel(mode, repository)

    @Test
    fun `fields are filled from the current profile`() {
        val state = viewModel().state.value

        assertThat(state.firstName).isEqualTo("Иван")
        assertThat(state.lastName).isEqualTo("Иванов")
        assertThat(state.username).isEqualTo("ivan_i")
        assertThat(state.bio).isEqualTo("Привет")
        assertThat(state.birthDate).isEqualTo(BirthDate(1990, 5, 7))
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `an unknown profile is loaded first`() {
        repository = FakeUserProfileRepository(null).apply { profileAfterRefresh = existing }

        val state = viewModel().state.value

        assertThat(repository.refreshCalls).isEqualTo(1)
        assertThat(state.firstName).isEqualTo("Иван")
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `load failure is shown and saving stays disabled`() {
        repository = FakeUserProfileRepository(null).apply {
            refreshResult = Result.Error(DataError.Network.NO_INTERNET)
        }

        val state = viewModel().state.value

        assertThat(state.error).isNotNull()
        assertThat(state.canSave).isFalse()
    }

    @Test
    fun `username input is normalized to allowed characters`() {
        val viewModel = viewModel()

        viewModel.onAction(EditProfileAction.OnUsernameChange(" @Ivan-Ivanov!_9 "))

        assertThat(viewModel.state.value.username).isEqualTo("ivanivanov_9")
    }

    @Test
    fun `too short username blocks saving but an empty one does not`() {
        val viewModel = viewModel()

        viewModel.onAction(EditProfileAction.OnUsernameChange("abc"))
        assertThat(viewModel.state.value.canSave).isFalse()

        viewModel.onAction(EditProfileAction.OnUsernameChange(""))
        assertThat(viewModel.state.value.canSave).isTrue()
    }

    @Test
    fun `bio and names are limited`() {
        val viewModel = viewModel()

        viewModel.onAction(EditProfileAction.OnBioChange("а".repeat(500)))
        viewModel.onAction(EditProfileAction.OnFirstNameChange("б".repeat(500)))

        assertThat(viewModel.state.value.bio.length).isEqualTo(140)
        assertThat(viewModel.state.value.firstName.length).isEqualTo(64)
    }

    @Test
    fun `blank first name blocks saving`() {
        val viewModel = viewModel()

        viewModel.onAction(EditProfileAction.OnFirstNameChange("   "))

        assertThat(viewModel.state.value.canSave).isFalse()
    }

    @Test
    fun `save sends the trimmed values and closes the screen`() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(EditProfileAction.OnFirstNameChange("  Пётр "))
        viewModel.onAction(EditProfileAction.OnBioChange("  О себе  "))
        viewModel.onAction(EditProfileAction.OnBirthDateChange(null))

        viewModel.events.test {
            viewModel.onAction(EditProfileAction.OnSaveClick)

            assertThat(awaitItem()).isEqualTo(EditProfileEvent.Close)
        }
        assertThat(repository.updates).isEqualTo(
            listOf(ProfileUpdate("Пётр", "Иванов", "ivan_i", "О себе", null)),
        )
    }

    @Test
    fun `empty username is saved as no username`() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(EditProfileAction.OnUsernameChange(""))

        viewModel.events.test {
            viewModel.onAction(EditProfileAction.OnSaveClick)
            awaitItem()
        }
        assertThat(repository.updates.single().username).isNull()
    }

    @Test
    fun `taken username is reported on the username field`() = runTest {
        repository.updateResult = Result.Error(ProfileError.USERNAME_TAKEN)
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(EditProfileAction.OnSaveClick)

            expectNoEvents()
        }
        assertThat(viewModel.state.value.usernameError).isNotNull()
        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.isSaving).isFalse()

        viewModel.onAction(EditProfileAction.OnUsernameChange("other_name"))
        assertThat(viewModel.state.value.usernameError).isNull()
    }

    @Test
    fun `other save failures are reported as a general error`() = runTest {
        repository.updateResult = Result.Error(ProfileError.NO_INTERNET)
        val viewModel = viewModel()

        viewModel.onAction(EditProfileAction.OnSaveClick)

        assertThat(viewModel.state.value.error).isNotNull()
        assertThat(viewModel.state.value.usernameError).isNull()
    }

    @Test
    fun `skip closes the screen without saving`() = runTest {
        val viewModel = viewModel(EditProfileMode.SETUP)

        viewModel.events.test {
            viewModel.onAction(EditProfileAction.OnSkipClick)

            assertThat(awaitItem()).isEqualTo(EditProfileEvent.Close)
        }
        assertThat(repository.updates).hasSize(0)
    }
}
