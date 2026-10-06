package com.maro.feature.auth.presentation.registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maro.core.designsystem.theme.MaroTheme
import com.maro.core.presentation.util.ObserveAsEvents
import com.maro.core.presentation.util.rememberTextFieldValue
import com.maro.feature.auth.presentation.generated.resources.Res
import com.maro.feature.auth.presentation.generated.resources.registration_back
import com.maro.feature.auth.presentation.generated.resources.registration_continue
import com.maro.feature.auth.presentation.generated.resources.registration_first_name
import com.maro.feature.auth.presentation.generated.resources.registration_last_name
import com.maro.feature.auth.presentation.generated.resources.registration_phone
import com.maro.feature.auth.presentation.generated.resources.registration_phone_subtitle
import com.maro.feature.auth.presentation.generated.resources.registration_phone_title
import com.maro.feature.auth.presentation.generated.resources.registration_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RegistrationRoot(
    onNavigateBack: () -> Unit,
    onNavigateToVerifyCode: (RegistrationEvent.NavigateToVerifyCode) -> Unit,
    onAuthenticated: (isNewUser: Boolean) -> Unit,
    viewModel: RegistrationViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            RegistrationEvent.NavigateBack -> onNavigateBack()
            is RegistrationEvent.NavigateToVerifyCode -> onNavigateToVerifyCode(event)
            is RegistrationEvent.Authenticated -> onAuthenticated(event.isNewUser)
        }
    }

    RegistrationScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(state: RegistrationState, onAction: (RegistrationAction) -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.registration_title)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(RegistrationAction.OnBackClick) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.registration_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        // The content scrolls and the button stays pinned above the keyboard (edge-to-edge needs imePadding).
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Иконка приложения
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Text(
                    text = stringResource(Res.string.registration_phone_title),
                    style = MaterialTheme.typography.headlineSmall,
                )

                Text(
                    text = stringResource(Res.string.registration_phone_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Поле для имени
                val firstName = rememberTextFieldValue(state.firstName)
                OutlinedTextField(
                    value = firstName.value,
                    onValueChange = {
                        firstName.value = it
                        onAction(RegistrationAction.OnFirstNameChange(it.text))
                    },
                    label = { Text(stringResource(Res.string.registration_first_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                )

                // Поле для фамилии (необязательное)
                val lastName = rememberTextFieldValue(state.lastName)
                OutlinedTextField(
                    value = lastName.value,
                    onValueChange = {
                        lastName.value = it
                        onAction(RegistrationAction.OnLastNameChange(it.text))
                    },
                    label = { Text(stringResource(Res.string.registration_last_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                    leadingIcon = {
                        Icon(Icons.Default.PersonOutline, contentDescription = null)
                    },
                )

                // Поле для номера телефона
                val phoneNumber = rememberTextFieldValue(state.phoneNumber)
                OutlinedTextField(
                    value = phoneNumber.value,
                    onValueChange = {
                        phoneNumber.value = it
                        onAction(RegistrationAction.OnPhoneNumberChange(it.text))
                    },
                    label = { Text(stringResource(Res.string.registration_phone)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone),
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    },
                    // The "+7" prefix is always visible; the state keeps only the ten national digits.
                    visualTransformation = PhonePrefixTransformation,
                )

                state.error?.let { error ->
                    Text(
                        text = error.asString(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // Кнопка продолжения
            TextButton(
                onClick = { onAction(RegistrationAction.OnContinueClick) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                enabled = state.isContinueEnabled && !state.isLoading,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.registration_continue), modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Preview
@Composable
private fun RegistrationScreenPreview() {
    MaroTheme {
        RegistrationScreen(
            state = RegistrationState(firstName = "Иван", phoneNumber = "9001234567"),
            onAction = {},
        )
    }
}
