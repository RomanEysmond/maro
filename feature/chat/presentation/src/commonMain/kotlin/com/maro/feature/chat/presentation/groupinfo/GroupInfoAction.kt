package com.maro.feature.chat.presentation.groupinfo

sealed interface GroupInfoAction {
    data object OnBackClick : GroupInfoAction

    data object OnRenameClick : GroupInfoAction
    data class OnRenameChange(val value: String) : GroupInfoAction
    data object OnRenameConfirm : GroupInfoAction
    data object OnRenameDismiss : GroupInfoAction

    data class OnAddQueryChange(val value: String) : GroupInfoAction
    data object OnAddClick : GroupInfoAction

    data object OnLeaveClick : GroupInfoAction
    data object OnLeaveConfirm : GroupInfoAction
    data object OnLeaveDismiss : GroupInfoAction
}
