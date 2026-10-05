package com.maro.core.domain.chat

/** Limits of a group chat; `firestore.rules` checks the same numbers. */
object GroupRules {
    const val MAX_TITLE_LENGTH = 64

    /** Everyone, the creator included. */
    const val MAX_MEMBERS = 50

    fun isValidTitle(title: String): Boolean = title.trim().length in 1..MAX_TITLE_LENGTH
}
