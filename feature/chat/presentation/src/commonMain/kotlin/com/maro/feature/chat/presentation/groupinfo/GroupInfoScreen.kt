package com.maro.feature.chat.presentation.groupinfo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.maro.feature.chat.domain.ChatHeader
import com.maro.feature.chat.domain.ChatMember
import com.maro.feature.chat.presentation.generated.resources.Res
import com.maro.feature.chat.presentation.generated.resources.chat_back
import com.maro.feature.chat.presentation.generated.resources.chat_members
import com.maro.feature.chat.presentation.generated.resources.group_info_add
import com.maro.feature.chat.presentation.generated.resources.group_info_add_hint
import com.maro.feature.chat.presentation.generated.resources.group_info_cancel
import com.maro.feature.chat.presentation.generated.resources.group_info_creator
import com.maro.feature.chat.presentation.generated.resources.group_info_leave
import com.maro.feature.chat.presentation.generated.resources.group_info_leave_confirm
import com.maro.feature.chat.presentation.generated.resources.group_info_leave_text
import com.maro.feature.chat.presentation.generated.resources.group_info_leave_text_creator
import com.maro.feature.chat.presentation.generated.resources.group_info_rename
import com.maro.feature.chat.presentation.generated.resources.group_info_rename_title
import com.maro.feature.chat.presentation.generated.resources.group_info_save
import com.maro.feature.chat.presentation.generated.resources.group_info_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun GroupInfoRoot(
    viewModel: GroupInfoViewModel,
    onNavigateBack: () -> Unit,
    onLeftGroup: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            GroupInfoEvent.NavigateBack -> onNavigateBack()
            GroupInfoEvent.LeftGroup -> onLeftGroup()
        }
    }

    GroupInfoScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    state: GroupInfoState,
    onAction: (GroupInfoAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.group_info_title)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(GroupInfoAction.OnBackClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.chat_back))
                    }
                },
            )
        },
    ) { padding ->
        val header = state.header
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (header != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    InitialsAvatar(initials = header.initials, size = 64.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = header.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = pluralStringResource(Res.plurals.chat_members, header.memberCount, header.memberCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    if (state.canManage) {
                        IconButton(onClick = { onAction(GroupInfoAction.OnRenameClick) }, enabled = !state.isBusy) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.group_info_rename))
                        }
                    }
                }
            }

            if (state.canManage) {
                OutlinedTextField(
                    value = state.addQuery,
                    onValueChange = { onAction(GroupInfoAction.OnAddQueryChange(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.group_info_add_hint)) },
                    prefix = { Text("@") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAction(GroupInfoAction.OnAddClick) }),
                    trailingIcon = {
                        if (state.isAdding) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { onAction(GroupInfoAction.OnAddClick) }, enabled = state.canAdd) {
                                Icon(Icons.Default.PersonAdd, contentDescription = stringResource(Res.string.group_info_add))
                            }
                        }
                    },
                )
            }

            state.error?.let { error ->
                Text(text = error.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(header?.members.orEmpty().filter { it.isMember }, key = { it.id }) { member ->
                    MemberRow(member = member, isCreator = member.id == header?.createdBy)
                }
            }

            HorizontalDivider()
            TextButton(
                onClick = { onAction(GroupInfoAction.OnLeaveClick) },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.group_info_leave), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    state.renameDraft?.let { draft ->
        AlertDialog(
            onDismissRequest = { onAction(GroupInfoAction.OnRenameDismiss) },
            title = { Text(stringResource(Res.string.group_info_rename_title)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { onAction(GroupInfoAction.OnRenameChange(it)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = { onAction(GroupInfoAction.OnRenameConfirm) }, enabled = draft.isNotBlank() && !state.isBusy) {
                    Text(stringResource(Res.string.group_info_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(GroupInfoAction.OnRenameDismiss) }) {
                    Text(stringResource(Res.string.group_info_cancel))
                }
            },
        )
    }

    if (state.isLeaveConfirmVisible) {
        AlertDialog(
            onDismissRequest = { onAction(GroupInfoAction.OnLeaveDismiss) },
            title = { Text(stringResource(Res.string.group_info_leave)) },
            text = {
                Text(
                    stringResource(
                        if (state.canManage) Res.string.group_info_leave_text_creator else Res.string.group_info_leave_text,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { onAction(GroupInfoAction.OnLeaveConfirm) }) {
                    Text(stringResource(Res.string.group_info_leave_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(GroupInfoAction.OnLeaveDismiss) }) {
                    Text(stringResource(Res.string.group_info_cancel))
                }
            },
        )
    }
}

@Composable
private fun MemberRow(member: ChatMember, isCreator: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        InitialsAvatar(
            initials = listOf(member.firstName, member.lastName)
                .mapNotNull { it.trim().firstOrNull()?.uppercaseChar() }.joinToString("").ifEmpty { "?" },
            size = 40.dp,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = member.fullName, style = MaterialTheme.typography.titleSmall)
            member.username?.let { username ->
                Text(text = "@$username", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (isCreator) {
            Text(
                text = stringResource(Res.string.group_info_creator),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Preview
@Composable
private fun GroupInfoScreenPreview() {
    MaroTheme {
        GroupInfoScreen(
            state = GroupInfoState(
                header = ChatHeader(
                    title = "Поход в горы",
                    initials = "ПВ",
                    isGroup = true,
                    members = listOf(
                        ChatMember("a", "Анна", "Петрова", "anna_p"),
                        ChatMember("b", "Иван", "Иванов", "ivan_i"),
                    ),
                    canManage = true,
                    createdBy = "a",
                ),
            ),
            onAction = {},
        )
    }
}
