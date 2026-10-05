package com.maro.feature.chat.presentation.groupinfo

sealed interface GroupInfoEvent {
    data object NavigateBack : GroupInfoEvent

    /** The user is no longer in the group: neither this screen nor the conversation makes sense any more. */
    data object LeftGroup : GroupInfoEvent
}
