package com.maro.feature.chatlist.presentation.newchat

import com.maro.core.presentation.util.UiText
import com.maro.feature.chatlist.domain.FoundUser

data class NewChatState(
    /** What was typed: lowercase latin letters, digits and "_", without the "@". */
    val query: String = "",
    val isSearching: Boolean = false,
    val foundUser: FoundUser? = null,
    val isStarting: Boolean = false,
    val error: UiText? = null,
) {
    val canSearch: Boolean
        get() = query.isNotEmpty() && !isSearching && !isStarting
}
