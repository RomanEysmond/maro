package com.maro.feature.chat.presentation

import com.maro.core.presentation.time.toLocalDateTime
import com.maro.feature.chat.domain.Message
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

/** One row of the conversation: a message, or the heading above the messages of a day. */
sealed interface ChatItem {
    val key: String

    data class MessageItem(val message: Message) : ChatItem {
        override val key: String get() = message.id
    }

    data class DaySeparator(val date: LocalDate) : ChatItem {
        override val key: String get() = "day-$date"
    }
}

/**
 * The list is newest first and drawn bottom-up, so a day's heading goes right after its oldest message: between
 * [newer] and [older] when they fall on different days, and after the very oldest message ([older] is `null` only
 * once the start of the chat is loaded).
 */
internal fun daySeparatorBetween(newer: Message?, older: Message?, timeZone: TimeZone): ChatItem.DaySeparator? {
    if (newer == null) return null
    val newerDate = newer.createdAt.toLocalDateTime(timeZone).date
    val olderDate = older?.createdAt?.toLocalDateTime(timeZone)?.date
    return if (newerDate != olderDate) ChatItem.DaySeparator(newerDate) else null
}
