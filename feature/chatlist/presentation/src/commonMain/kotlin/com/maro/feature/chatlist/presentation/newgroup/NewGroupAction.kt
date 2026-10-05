package com.maro.feature.chatlist.presentation.newgroup

sealed interface NewGroupAction {
    data class OnTitleChange(val value: String) : NewGroupAction
    data class OnQueryChange(val value: String) : NewGroupAction
    data object OnAddClick : NewGroupAction
    data class OnRemoveClick(val userId: String) : NewGroupAction
    data object OnCreateClick : NewGroupAction
    data object OnBackClick : NewGroupAction
}
