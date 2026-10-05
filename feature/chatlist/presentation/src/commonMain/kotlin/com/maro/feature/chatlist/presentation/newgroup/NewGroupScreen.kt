package com.maro.feature.chatlist.presentation.newgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maro.core.designsystem.component.InitialsAvatar
import com.maro.core.designsystem.theme.MaroTheme
import com.maro.core.presentation.util.ObserveAsEvents
import com.maro.core.presentation.util.rememberTextFieldValue
import com.maro.feature.chatlist.domain.FoundUser
import com.maro.feature.chatlist.presentation.generated.resources.Res
import com.maro.feature.chatlist.presentation.generated.resources.new_chat_back
import com.maro.feature.chatlist.presentation.generated.resources.new_group_add
import com.maro.feature.chatlist.presentation.generated.resources.new_group_create
import com.maro.feature.chatlist.presentation.generated.resources.new_group_members
import com.maro.feature.chatlist.presentation.generated.resources.new_group_remove
import com.maro.feature.chatlist.presentation.generated.resources.new_group_title
import com.maro.feature.chatlist.presentation.generated.resources.new_group_title_label
import com.maro.feature.chatlist.presentation.generated.resources.new_group_username
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NewGroupRoot(
    onNavigateBack: () -> Unit,
    onOpenChat: (chatId: String) -> Unit,
    viewModel: NewGroupViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            NewGroupEvent.NavigateBack -> onNavigateBack()
            is NewGroupEvent.NavigateToChat -> onOpenChat(event.chatId)
        }
    }

    NewGroupScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupScreen(
    state: NewGroupState,
    onAction: (NewGroupAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.new_group_title)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(NewGroupAction.OnBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.new_chat_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val title = rememberTextFieldValue(state.title)
            OutlinedTextField(
                value = title.value,
                onValueChange = { title.value = it; onAction(NewGroupAction.OnTitleChange(it.text)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.new_group_title_label)) },
                singleLine = true,
            )
            val query = rememberTextFieldValue(state.query)
            OutlinedTextField(
                value = query.value,
                onValueChange = { query.value = it; onAction(NewGroupAction.OnQueryChange(it.text)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.new_group_username)) },
                prefix = { Text("@") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAction(NewGroupAction.OnAddClick) }),
                trailingIcon = {
                    if (state.isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { onAction(NewGroupAction.OnAddClick) }, enabled = state.canAdd) {
                            Icon(Icons.Default.PersonAdd, contentDescription = stringResource(Res.string.new_group_add))
                        }
                    }
                },
            )
            state.error?.let { error ->
                Text(text = error.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            if (state.members.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.new_group_members, state.members.size + 1),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.members, key = { it.id }) { member ->
                    MemberRow(member = member, onRemoveClick = { onAction(NewGroupAction.OnRemoveClick(member.id)) })
                }
            }
            Button(
                onClick = { onAction(NewGroupAction.OnCreateClick) },
                enabled = state.canCreate,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.new_group_create))
                }
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: FoundUser,
    onRemoveClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialsAvatar(initials = member.initials, size = 40.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = member.fullName, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "@${member.username}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onRemoveClick) {
            Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.new_group_remove))
        }
    }
}

@Preview
@Composable
private fun NewGroupScreenPreview() {
    MaroTheme {
        NewGroupScreen(
            state = NewGroupState(
                title = "Поход в горы",
                members = listOf(FoundUser("a", "Анна", "Петрова", "anna_p"), FoundUser("b", "Пётр", "", "petr")),
            ),
            onAction = {},
        )
    }
}
