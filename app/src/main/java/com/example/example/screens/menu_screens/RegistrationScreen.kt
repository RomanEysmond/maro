package com.example.example.screens.menu_screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(navController: NavController, onNextButtonClickedToSkip: () -> Unit) {
    var phoneNumber by remember { mutableStateOf(TextFieldValue("+7")) }
    var firstName by remember { mutableStateOf(TextFieldValue()) }
    var lastName by remember { mutableStateOf(TextFieldValue()) }
    var isButtonEnabled by remember {
        mutableStateOf(false)
    }

    // Проверяем условия для активации кнопки
    LaunchedEffect(phoneNumber, firstName, lastName) {
        isButtonEnabled = phoneNumber.text.length >= 11 && firstName.text.isNotBlank()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Регистрация") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Иконка приложения
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Регистрация",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Введите ваш номер телефона",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Мы отправим SMS с кодом подтверждения",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Поле для имени
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("Имя") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text
                ),
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = "Имя")
                }
            )

            // Поле для фамилии (необязательное)
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Фамилия (необязательно)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text
                ),
                leadingIcon = {
                    Icon(Icons.Default.PersonOutline, contentDescription = "Фамилия")
                }
            )

            // Поле для номера телефона
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { newValue ->
                    if (newValue.text.length <= 12 && newValue.text.matches(Regex("^\\+?\\d*$"))) {
                        phoneNumber = newValue
                    }
                },
                label = { Text("Номер телефона") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Phone
                ),
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = "Телефон")
                },
                prefix = {
                    Text(text = "+7", color = MaterialTheme.colorScheme.onSurface)
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Кнопка продолжения
            androidx.compose.material.TextButton(
                onClick = {
                        onNextButtonClickedToSkip()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                enabled = isButtonEnabled
            ) {
                Text("Продолжить", modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}