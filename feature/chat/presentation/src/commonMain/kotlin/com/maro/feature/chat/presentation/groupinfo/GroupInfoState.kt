package com.maro.feature.chat.presentation.groupinfo

import com.maro.core.presentation.util.UiText
import com.maro.feature.chat.domain.ChatHeader

data class GroupInfoState(
    val header: ChatHeader? = null,
    /** The @username to add, without the "@". */
    val addQuery: String = "",
    val isAdding: Boolean = false,
    /** Non-null while the rename dialog is open: what is typed in it. */
    val renameDraft: String? = null,
    val isLeaveConfirmVisible: Boolean = false,
    /** A rename or leave is on its way to the server. */
    val isBusy: Boolean = false,
    val error: UiText? = null,
) {
    val canManage: Boolean
        get() = header?.canManage == true

    val canAdd: Boolean
        get() = canManage && addQuery.isNotEmpty() && !isAdding && !isBusy
}
