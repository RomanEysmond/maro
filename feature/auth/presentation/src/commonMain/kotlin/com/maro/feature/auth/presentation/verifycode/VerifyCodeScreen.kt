package com.maro.feature.auth.presentation.verifycode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maro.core.designsystem.theme.MaroTheme
import com.maro.core.presentation.util.ObserveAsEvents
import com.maro.feature.auth.presentation.generated.resources.Res
import com.maro.feature.auth.presentation.generated.resources.verify_back
import com.maro.feature.auth.presentation.generated.resources.verify_code_label
import com.maro.feature.auth.presentation.generated.resources.verify_confirm
import com.maro.feature.auth.presentation.generated.resources.verify_resend
import com.maro.feature.auth.presentation.generated.resources.verify_resend_timer
import com.maro.feature.auth.presentation.generated.resources.verify_subtitle
import com.maro.feature.auth.presentation.generated.resources.verify_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun VerifyCodeRoot(
    viewModel: VerifyCodeViewModel,
    onNavigateBack: () -> Unit,
    onAuthenticated: (isNewUser: Boolean) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            VerifyCodeEvent.NavigateBack -> onNavigateBack()
            is VerifyCodeEvent.Authenticated -> onAuthenticated(event.isNewUser)
        }
    }

    VerifyCodeScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyCodeScreen(
    state: VerifyCodeState,
    onAction: (VerifyCodeAction) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.verify_title)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(VerifyCodeAction.OnBackClick) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.verify_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.verify_subtitle, state.phoneNumber),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )

            OutlinedTextField(
                value = state.code,
                onValueChange = { onAction(VerifyCodeAction.OnCodeChange(it)) },
                label = { Text(stringResource(Res.string.verify_code_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                isError = state.error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
            )

            state.error?.let { error ->
                Text(
                    text = error.asString(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }

            Button(
                onClick = { onAction(VerifyCodeAction.OnConfirmClick) },
                enabled = state.isConfirmEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.verify_confirm), modifier = Modifier.padding(vertical = 4.dp))
                }
            }

            TextButton(
                onClick = { onAction(VerifyCodeAction.OnResendClick) },
                enabled = state.canResend,
            ) {
                Text(
                    if (state.resendSecondsLeft > 0) {
                        stringResource(Res.string.verify_resend_timer, state.resendSecondsLeft)
                    } else {
                        stringResource(Res.string.verify_resend)
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun VerifyCodeScreenPreview() {
    MaroTheme {
        VerifyCodeScreen(
            state = VerifyCodeState(phoneNumber = "+79001234567", code = "123", resendSecondsLeft = 42),
            onAction = {},
        )
    }
}
