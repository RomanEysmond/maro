package com.maro.feature.chat.data.media

/** A picture ready to go: the app's own JPEG and its size in pixels. */
data class PreparedImage(val path: String, val width: Int, val height: Int)

/**
 * The device side of photos: the picked picture, shrunk to a sensible size, kept in the app's own storage (the picker's
 * URI may stop working later). Platform code: Android in androidMain.
 */
interface ImageFiles {
    /** [source] is what the picker returned; `null` when it cannot be read as a picture. */
    suspend fun prepare(source: String, messageId: String): PreparedImage?

    /** The bytes to upload; `null` when the file is gone. */
    suspend fun read(path: String): ByteArray?

    /** Sign-out: the previous user's pictures leave the device. */
    suspend fun deleteAll()

    companion object {
        /** The longer side, in pixels: sharp on a phone screen, a few hundred kilobytes as JPEG. */
        const val MAX_SIDE = 1600
        const val JPEG_QUALITY = 85
    }
}
