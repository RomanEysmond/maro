package com.maro.feature.auth.presentation.registration

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
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
class RegistrationViewModelTest {

    private lateinit var viewModel: RegistrationViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = RegistrationViewModel()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `phone number starts empty because the plus-seven prefix is shown by the UI`() {
        assertThat(viewModel.state.value.phoneNumber).isEqualTo("")
    }

    @Test
    fun `continue is disabled until name and all ten digits are entered`() {
        assertThat(viewModel.state.value.isContinueEnabled).isFalse()

        viewModel.onAction(RegistrationAction.OnFirstNameChange("Иван"))
        assertThat(viewModel.state.value.isContinueEnabled).isFalse()

        viewModel.onAction(RegistrationAction.OnPhoneNumberChange("900123456"))
        assertThat(viewModel.state.value.isContinueEnabled).isFalse()

        viewModel.onAction(RegistrationAction.OnPhoneNumberChange("9001234567"))
        assertThat(viewModel.state.value.isContinueEnabled).isTrue()
    }

    @Test
    fun `blank first name keeps continue disabled`() {
        viewModel.onAction(RegistrationAction.OnFirstNameChange("   "))
        viewModel.onAction(RegistrationAction.OnPhoneNumberChange("9001234567"))

        assertThat(viewModel.state.value.isContinueEnabled).isFalse()
    }

    @Test
    fun `phone input keeps only digits and at most ten of them`() {
        viewModel.onAction(RegistrationAction.OnPhoneNumberChange("900abc12"))
        assertThat(viewModel.state.value.phoneNumber).isEqualTo("90012")

        viewModel.onAction(RegistrationAction.OnPhoneNumberChange("90012345678901"))
        assertThat(viewModel.state.value.phoneNumber).isEqualTo("9001234567")
    }

    @Test
    fun `pasted number with a country code is normalized`() {
        viewModel.onAction(RegistrationAction.OnPhoneNumberChange("+7 (900) 123-45-67"))
        assertThat(viewModel.state.value.phoneNumber).isEqualTo("9001234567")

        viewModel.onAction(RegistrationAction.OnPhoneNumberChange("89001234567"))
        assertThat(viewModel.state.value.phoneNumber).isEqualTo("9001234567")
    }

    @Test
    fun `full phone number is in international format`() {
        viewModel.onAction(RegistrationAction.OnPhoneNumberChange("9001234567"))

        assertThat(viewModel.state.value.fullPhoneNumber).isEqualTo("+79001234567")
    }

    @Test
    fun `continue emits NavigateNext only when the form is valid`() = runTest {
        viewModel.events.test {
            viewModel.onAction(RegistrationAction.OnContinueClick)
            expectNoEvents()

            viewModel.onAction(RegistrationAction.OnFirstNameChange("Иван"))
            viewModel.onAction(RegistrationAction.OnPhoneNumberChange("9001234567"))
            viewModel.onAction(RegistrationAction.OnContinueClick)

            assertThat(awaitItem()).isEqualTo(RegistrationEvent.NavigateNext)
        }
    }

    @Test
    fun `back click emits NavigateBack`() = runTest {
        viewModel.events.test {
            viewModel.onAction(RegistrationAction.OnBackClick)

            assertThat(awaitItem()).isEqualTo(RegistrationEvent.NavigateBack)
        }
    }
}
