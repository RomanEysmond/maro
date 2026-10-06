package com.maro.server

/**
 * Signed, short-lived URLs of the object storage (any S3-compatible one: Backblaze B2 now, R2 or a Russian provider
 * later — only the configuration changes). The app never gets the storage keys: it asks the server for a URL per file.
 */
interface MediaStore {
    /** Where the client PUTs the file to; valid for a few minutes. */
    fun uploadUrl(key: String): String

    /** Where the client GETs the file from; valid for a few minutes. */
    fun downloadUrl(key: String): String
}

sealed interface MediaResult {
    data class Url(val key: String, val url: String) : MediaResult
    data object ChatNotFound : MediaResult

    /** The caller is not a participant of the chat, or the file is not one of the chat's. */
    data object Forbidden : MediaResult

    /** The storage is not configured on this server. */
    data object Unavailable : MediaResult
}

/**
 * Media of a chat lives under `chats/{chatId}/{messageId}`: the message that carries a file names it by its own id,
 * so a key alone says which chat it belongs to and only that chat's participants get a URL for it. The file is
 * opaque to the server (one day it may be encrypted end to end).
 */
class MediaService(
    private val store: ChatStore,
    private val media: MediaStore?,
) {
    suspend fun uploadUrl(uid: String, chatId: String, messageId: String): MediaResult =
        checked(uid, chatId) { media ->
            val key = keyFor(chatId, messageId)
            MediaResult.Url(key, media.uploadUrl(key))
        }

    suspend fun downloadUrl(uid: String, chatId: String, key: String): MediaResult {
        val messageId = key.removePrefix(prefixOf(chatId))
        if (!key.startsWith(prefixOf(chatId)) || !NotifyService.isValidId(messageId)) return MediaResult.Forbidden
        return checked(uid, chatId) { media -> MediaResult.Url(key, media.downloadUrl(key)) }
    }

    private suspend fun checked(uid: String, chatId: String, block: (MediaStore) -> MediaResult): MediaResult {
        val media = media ?: return MediaResult.Unavailable
        val participants = store.participants(chatId) ?: return MediaResult.ChatNotFound
        if (uid !in participants) return MediaResult.Forbidden
        return block(media)
    }

    companion object {
        fun keyFor(chatId: String, messageId: String): String = prefixOf(chatId) + messageId

        private fun prefixOf(chatId: String): String = "chats/$chatId/"
    }
}
