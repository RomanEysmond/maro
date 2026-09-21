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
fun RegistrationScreen(
    state: RegistrationState,
    onAction: (RegistrationAction) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Регистрация") },
                navigationIcon = {
                    IconButton(onClick = { onAction(RegistrationAction.OnBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
                    contentDescription = "Регистрация",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Text(
                    text = "Введите ваш номер телефона",
                    style = MaterialTheme.typography.headlineSmall,
                )

                Text(
                    text = "Мы отправим SMS с кодом подтверждения",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Поле для имени
                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = { onAction(RegistrationAction.OnFirstNameChange(it)) },
                    label = { Text("Имя") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = "Имя")
                    },
                )

                // Поле для фамилии (необязательное)
                OutlinedTextField(
                    value = state.lastName,
                    onValueChange = { onAction(RegistrationAction.OnLastNameChange(it)) },
                    label = { Text("Фамилия (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                    leadingIcon = {
                        Icon(Icons.Default.PersonOutline, contentDescription = "Фамилия")
                    },
                )

                // Поле для номера телефона
                OutlinedTextField(
                    value = state.phoneNumber,
                    onValueChange = { onAction(RegistrationAction.OnPhoneNumberChange(it)) },
                    label = { Text("Номер телефона") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone),
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = "Телефон")
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
                    Text("Продолжить", modifier = Modifier.padding(vertical = 8.dp))
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
