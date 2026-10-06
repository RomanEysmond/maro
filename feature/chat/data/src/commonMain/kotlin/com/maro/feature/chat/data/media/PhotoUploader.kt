package com.maro.feature.chat.data.media

import com.maro.core.database.message.MessageDao
import com.maro.core.database.message.MessageEntity
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result

/**
 * The photo side of sending: the picked picture onto the device, then into the storage before its message may go
 * out (a message must never point at a file that is not there).
 */
class PhotoUploader(
    private val media: ChatMediaRemoteDataSource,
    private val files: ImageFiles,
    private val messageDao: MessageDao,
) {
    /** The picture shrunk into the app's own storage; `null` when [source] is not a readable picture. */
    suspend fun prepare(source: String, messageId: String): PreparedImage? = files.prepare(source, messageId)

    /** Uploads the photo of [message] once: a retry after a failed send does not upload it again. */
    suspend fun upload(message: MessageEntity): EmptyResult<DataError.Network> {
        if (message.type != MessageEntity.TYPE_IMAGE) return Result.Success(Unit)
        // The local copy is gone (app data cleared): the photo cannot be sent any more.
        val local = messageDao.getLocalMedia(message.id) ?: return Result.Error(DataError.Network.NOT_FOUND)
        if (local.uploaded) return Result.Success(Unit)
        val bytes = files.read(local.path) ?: return Result.Error(DataError.Network.NOT_FOUND)
        return media.upload(message.chatId, message.id, bytes).also { result ->
            if (result is Result.Success) messageDao.markMediaUploaded(message.id)
        }
    }
}
