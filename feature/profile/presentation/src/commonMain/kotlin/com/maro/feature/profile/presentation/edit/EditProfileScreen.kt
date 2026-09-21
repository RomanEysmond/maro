package com.maro.feature.profile.presentation.edit

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maro.core.designsystem.component.InitialsAvatar
import com.maro.core.designsystem.theme.MaroTheme
import com.maro.core.domain.profile.BirthDate
import com.maro.core.domain.profile.ProfileRules
import com.maro.core.presentation.util.ObserveAsEvents
import com.maro.feature.profile.presentation.generated.resources.Res
import com.maro.feature.profile.presentation.generated.resources.edit_back
import com.maro.feature.profile.presentation.generated.resources.edit_bio
import com.maro.feature.profile.presentation.generated.resources.edit_bio_counter
import com.maro.feature.profile.presentation.generated.resources.edit_birth_date
import com.maro.feature.profile.presentation.generated.resources.edit_birth_date_clear
import com.maro.feature.profile.presentation.generated.resources.edit_date_cancel
import com.maro.feature.profile.presentation.generated.resources.edit_date_ok
import com.maro.feature.profile.presentation.generated.resources.edit_first_name
import com.maro.feature.profile.presentation.generated.resources.edit_last_name
import com.maro.feature.profile.presentation.generated.resources.edit_save
import com.maro.feature.profile.presentation.generated.resources.edit_skip
import com.maro.feature.profile.presentation.generated.resources.edit_subtitle_setup
import com.maro.feature.profile.presentation.generated.resources.edit_title_edit
import com.maro.feature.profile.presentation.generated.resources.edit_title_setup
import com.maro.feature.profile.presentation.generated.resources.edit_username
import com.maro.feature.profile.presentation.generated.resources.edit_username_hint
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun EditProfileRoot(
    viewModel: EditProfileViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            EditProfileEvent.Close -> onClose()
        }
    }

    EditProfileScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    state: EditProfileState,
    onAction: (EditProfileAction) -> Unit,
) {
    val isSetup = state.mode == EditProfileMode.SETUP

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(if (isSetup) Res.string.edit_title_setup else Res.string.edit_title_edit),
                    )
                },
                navigationIcon = {
                    if (!isSetup) {
                        IconButton(onClick = { onAction(EditProfileAction.OnBackClick) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.edit_back),
                            )
                        }
                    }
                },
                actions = {
                    if (isSetup) {
                        TextButton(onClick = { onAction(EditProfileAction.OnSkipClick) }) {
                            Text(stringResource(Res.string.edit_skip))
                        }
                    }
                },
            )
        },
    ) { padding ->
        // The form scrolls and the button stays pinned above the keyboard (edge-to-edge needs imePadding).
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
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                InitialsAvatar(
                    initials = initialsOf(state.firstName, state.lastName),
                    size = 96.dp,
                )

                if (isSetup) {
                    Text(
                        text = stringResource(Res.string.edit_subtitle_setup),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    OutlinedTextField(
                        value = state.firstName,
                        onValueChange = { onAction(EditProfileAction.OnFirstNameChange(it)) },
                        label = { Text(stringResource(Res.string.edit_first_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.lastName,
                        onValueChange = { onAction(EditProfileAction.OnLastNameChange(it)) },
                        label = { Text(stringResource(Res.string.edit_last_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                OutlinedTextField(
                    value = state.username,
                    onValueChange = { onAction(EditProfileAction.OnUsernameChange(it)) },
                    label = { Text(stringResource(Res.string.edit_username)) },
                    prefix = { Text("@") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !state.isUsernameValid || state.usernameError != null,
                    supportingText = {
                        val takenError = state.usernameError
                        if (takenError != null) {
                            Text(takenError.asString())
                        } else {
                            Text(stringResource(Res.string.edit_username_hint))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                )

                OutlinedTextField(
                    value = state.bio,
                    onValueChange = { onAction(EditProfileAction.OnBioChange(it)) },
                    label = { Text(stringResource(Res.string.edit_bio)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    supportingText = {
                        Text(
                            text = stringResource(
                                Res.string.edit_bio_counter,
                                state.bio.length,
                                ProfileRules.MAX_BIO_LENGTH,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                        )
                    },
                )

                BirthDateField(
                    birthDate = state.birthDate,
                    onBirthDateChange = { onAction(EditProfileAction.OnBirthDateChange(it)) },
                )

                state.error?.let { error ->
                    Text(
                        text = error.asString(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Button(
                onClick = { onAction(EditProfileAction.OnSaveClick) },
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.edit_save), modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDateField(
    birthDate: BirthDate?,
    onBirthDateChange: (BirthDate?) -> Unit,
) {
    var isPickerOpen by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    // A read-only text field does not report clicks by itself: open the picker when the press is released.
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) isPickerOpen = true
        }
    }

    OutlinedTextField(
        value = birthDate?.toDisplayString().orEmpty(),
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(Res.string.edit_birth_date)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        interactionSource = interactionSource,
        trailingIcon = {
            if (birthDate != null) {
                IconButton(onClick = { onBirthDateChange(null) }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(Res.string.edit_birth_date_clear),
                    )
                }
            } else {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
            }
        },
    )

    if (isPickerOpen) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = birthDate?.toEpochMillis(),
            selectableDates = remember { PastDates(Clock.System.now().toEpochMilliseconds()) },
        )
        DatePickerDialog(
            onDismissRequest = { isPickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { onBirthDateChange(BirthDate.fromEpochMillis(it)) }
                        isPickerOpen = false
                    },
                ) { Text(stringResource(Res.string.edit_date_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { isPickerOpen = false }) { Text(stringResource(Res.string.edit_date_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Nobody is born in the future. */
@OptIn(ExperimentalMaterial3Api::class)
private class PastDates(private val nowMillis: Long) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= nowMillis
}

private fun initialsOf(firstName: String, lastName: String): String =
    listOf(firstName, lastName)
        .mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

@Preview
@Composable
private fun EditProfileScreenPreview() {
    MaroTheme {
        EditProfileScreen(
            state = EditProfileState(
                mode = EditProfileMode.EDIT,
                firstName = "Иван",
                lastName = "Иванов",
                username = "ivan_ivanov",
                bio = "Люблю горы",
                birthDate = BirthDate(1990, 5, 7),
            ),
            onAction = {},
        )
    }
}
