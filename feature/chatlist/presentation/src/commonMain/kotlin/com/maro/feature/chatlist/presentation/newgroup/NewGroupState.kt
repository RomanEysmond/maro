package com.maro.feature.chatlist.presentation.newgroup

import com.maro.core.domain.chat.GroupRules
import com.maro.core.presentation.util.UiText
import com.maro.feature.chatlist.domain.FoundUser

data class NewGroupState(
    val title: String = "",
    /** The @username being looked up, without the "@". */
    val query: String = "",
    /** People added so far, in the order they were added; the user is added by the group itself. */
    val members: List<FoundUser> = emptyList(),
    val isSearching: Boolean = false,
    val isCreating: Boolean = false,
    val error: UiText? = null,
) {
    val canAdd: Boolean
        get() = query.isNotEmpty() && !isSearching && !isCreating && members.size + 1 < GroupRules.MAX_MEMBERS

    val canCreate: Boolean
        get() = GroupRules.isValidTitle(title) && members.isNotEmpty() && !isCreating
}
