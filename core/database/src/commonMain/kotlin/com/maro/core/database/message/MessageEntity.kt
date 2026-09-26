package com.maro.core.database.message

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
)
