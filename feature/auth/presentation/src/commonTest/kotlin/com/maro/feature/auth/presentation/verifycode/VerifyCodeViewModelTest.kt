package com.maro.feature.auth.presentation.verifycode

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.Result
import com.maro.feature.auth.domain.AuthError
import com.maro.feature.auth.domain.SendCodeOutcome
import com.maro.feature.auth.presentation.VerifyCodeRoute
import com.maro.feature.auth.presentation.fakes.FakePhoneAuthenticator
import com.maro.feature.auth.presentation.fakes.FakeUserProfileRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class VerifyCodeViewModelTest {

    private lateinit var authenticator: FakePhoneAuthenticator
    private lateinit var profiles: FakeUserProfileRepository
    private lateinit var viewModel: VerifyCodeViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        authenticator = FakePhoneAuthenticator()
        profiles = FakeUserProfileRepository()
        viewModel = VerifyCodeViewModel(
            route = VerifyCodeRoute(firstName = "Иван", lastName = "Иванов", phone = "+79001234567"),
            phoneAuthenticator = authenticator,
            userProfileRepository = profiles,
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `code keeps only digits and is not submitted while incomplete`() {
        viewModel.onAction(VerifyCodeAction.OnCodeChange("12a34"))

        assertThat(viewModel.state.value.code).isEqualTo("1234")
        assertThat(authenticator.verifiedCodes).hasSize(0)
    }

    @Test
    fun `entering the sixth digit verifies the code and emits Authenticated`() = runTest {
        viewModel.events.test {
            viewModel.onAction(VerifyCodeAction.OnCodeChange("123456"))

            assertThat(awaitItem()).isEqualTo(VerifyCodeEvent.Authenticated(isNewUser = true))
        }
        assertThat(authenticator.verifiedCodes).isEqualTo(listOf("123456"))
        assertThat(profiles.calls).isEqualTo(1)
    }

    @Test
    fun `wrong code shows an error and does not touch the profile`() = runTest {
        authenticator.verifyResult = Result.Error(AuthError.INVALID_CODE)

        viewModel.events.test {
            viewModel.onAction(VerifyCodeAction.OnCodeChange("123456"))

            expectNoEvents()
        }
        assertThat(viewModel.state.value.error).isNotNull()
        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(profiles.calls).isEqualTo(0)
    }

    @Test
    fun `editing the code clears the error`() {
        authenticator.verifyResult = Result.Error(AuthError.INVALID_CODE)
        viewModel.onAction(VerifyCodeAction.OnCodeChange("123456"))

        viewModel.onAction(VerifyCodeAction.OnCodeChange("12345"))

        assertThat(viewModel.state.value.error).isNull()
    }

    @Test
    fun `profile failure after a correct code is retried without verifying the code again`() = runTest {
        profiles.result = Result.Error(DataError.Network.NO_INTERNET)
        viewModel.onAction(VerifyCodeAction.OnCodeChange("123456"))
        assertThat(viewModel.state.value.error).isNotNull()

        profiles.result = null
        viewModel.events.test {
            viewModel.onAction(VerifyCodeAction.OnConfirmClick)

            assertThat(awaitItem()).isEqualTo(VerifyCodeEvent.Authenticated(isNewUser = true))
        }
        assertThat(authenticator.verifiedCodes).hasSize(1)
        assertThat(profiles.calls).isEqualTo(2)
    }

    @Test
    fun `resend is blocked until the timer runs out`() = runTest {
        assertThat(viewModel.state.value.canResend).isFalse()
        viewModel.onAction(VerifyCodeAction.OnResendClick)
        assertThat(authenticator.resentTo).hasSize(0)

        testScheduler.advanceTimeBy(VerifyCodeState.RESEND_DELAY_SECONDS * 1_000L + 1)
        assertThat(viewModel.state.value.resendSecondsLeft).isEqualTo(0)
        assertThat(viewModel.state.value.canResend).isTrue()

        viewModel.onAction(VerifyCodeAction.OnResendClick)

        assertThat(authenticator.resentTo).isEqualTo(listOf("+79001234567"))
        assertThat(viewModel.state.value.resendSecondsLeft).isEqualTo(VerifyCodeState.RESEND_DELAY_SECONDS)
    }

    @Test
    fun `instant verification on resend goes straight to the profile`() = runTest {
        authenticator.resendResult = Result.Success(SendCodeOutcome.AutoVerified)
        testScheduler.advanceTimeBy(VerifyCodeState.RESEND_DELAY_SECONDS * 1_000L + 1)

        viewModel.events.test {
            viewModel.onAction(VerifyCodeAction.OnResendClick)

            assertThat(awaitItem()).isEqualTo(VerifyCodeEvent.Authenticated(isNewUser = true))
        }
        assertThat(authenticator.verifiedCodes).hasSize(0)
    }
}
