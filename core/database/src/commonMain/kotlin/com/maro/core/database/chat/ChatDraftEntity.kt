package com.maro.core.database.chat

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * The unsent text typed in a chat's input, kept until it is sent or erased (also across restarts). A table of its
 * own, not a column of `chats`: `ChatDao.replaceAll` rewrites chat rows from the server, which knows nothing of drafts.
 */
@Entity(tableName = "chat_drafts")
data class ChatDraftEntity(@PrimaryKey val chatId: String, val text: String, val updatedAt: Long)
