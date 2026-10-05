package com.maro.core.database.message

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** Room's copy of a message. The primary key is the `client_id` (UUID), which is also the Firestore document id. */
@Entity(tableName = "messages", indices = [Index(value = ["chatId", "createdAt"])])
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val text: String,
    /** Epoch millis: the local time while [status] is SENDING, replaced by the server time once synced. */
    val createdAt: Long,
    /** "SENDING" | "SENT" | "FAILED". */
    val status: String,
    /** "text" | "system" (a group event: created, added, left, renamed; [text] is empty then). */
    @ColumnInfo(defaultValue = "text") val type: String = TYPE_TEXT,
    /** System messages only: "created" | "added" | "left" | "renamed". */
    val eventKind: String? = null,
    /** System messages only: the users the event is about, comma-separated ids. */
    val eventTargets: String? = null,
    /** System messages only: the group title the event set ("created", "renamed"). */
    val eventTitle: String? = null,
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_SYSTEM = "system"
    }
}
