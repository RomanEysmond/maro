package com.maro.core.database.message

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * Which part of a chat's history Room holds. The server messages cached for a chat always form one unbroken
 * range, from the oldest cursor to the newest one: new messages are only fetched after the newest cursor and
 * history only before the oldest, so there can never be a gap in between.
 *
 * A cursor is the `(createdAt, id)` pair of a server message; `createdAt` is kept in microseconds, the precision
 * of a Firestore timestamp, so a cursor never skips a message written in the same millisecond.
 *
 * No row means the chat was never synced. A row with `null` cursors means the chat was empty on the server.
 */
@Entity(tableName = "message_sync")
data class MessageSyncEntity(
    @PrimaryKey val chatId: String,
    val newestAtMicros: Long?,
    val newestId: String?,
    val oldestAtMicros: Long?,
    val oldestId: String?,
    /** The oldest cursor is the first message of the chat: there is no older history to load. */
    val reachedStart: Boolean,
)
