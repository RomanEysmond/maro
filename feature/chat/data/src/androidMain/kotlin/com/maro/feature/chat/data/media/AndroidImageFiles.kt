package com.maro.feature.chat.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Pictures in `files/media/` of the app: private, removed with the app and on sign-out. */
internal class AndroidImageFiles(private val context: Context) : ImageFiles {

    private val directory: File
        get() = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    override suspend fun prepare(source: String, messageId: String): PreparedImage? = withContext(Dispatchers.IO) {
        val uri = Uri.parse(source)
        val bitmap = decodeScaled(uri) ?: return@withContext null
        val upright = bitmap.rotated(orientationOf(uri))
        val file = File(directory, "$messageId.jpg")
        try {
            file.outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, ImageFiles.JPEG_QUALITY, it) }
            PreparedImage(file.absolutePath, upright.width, upright.height)
        } catch (e: IOException) {
            file.delete()
            null
        } finally {
            upright.recycle()
        }
    }

    override suspend fun read(path: String): ByteArray? = withContext(Dispatchers.IO) {
        File(path).takeIf { it.isFile }?.readBytes()
    }

    override suspend fun deleteAll() {
        withContext(Dispatchers.IO) { File(context.filesDir, DIRECTORY).deleteRecursively() }
    }

    /** Decoded at most twice as big as needed (cheap subsampling), then scaled to exactly [ImageFiles.MAX_SIDE]. */
    private fun decodeScaled(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(uri) { BitmapFactory.decodeStream(it, null, bounds) }
        val longer = max(bounds.outWidth, bounds.outHeight)
        if (longer <= 0) return null

        var sample = 1
        while (longer / (sample * 2) >= ImageFiles.MAX_SIDE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = open(uri) { BitmapFactory.decodeStream(it, null, options) } ?: return null

        val scale = ImageFiles.MAX_SIDE.toFloat() / max(decoded.width, decoded.height)
        if (scale >= 1f) return decoded
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).roundToInt(),
            (decoded.height * scale).roundToInt(),
            true,
        )
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    /** Camera photos are often stored sideways with an EXIF note; the JPEG we send is upright instead. */
    private fun orientationOf(uri: Uri): Int =
        open(uri) { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            ?: ExifInterface.ORIENTATION_NORMAL

    private fun Bitmap.rotated(orientation: Int): Bitmap {
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> QUARTER_TURN
            ExifInterface.ORIENTATION_ROTATE_180 -> HALF_TURN
            ExifInterface.ORIENTATION_ROTATE_270 -> THREE_QUARTER_TURN
            else -> return this
        }
        val rotated = Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees) }, true)
        if (rotated !== this) recycle()
        return rotated
    }

    private fun <T> open(uri: Uri, block: (java.io.InputStream) -> T): T? = try {
        context.contentResolver.openInputStream(uri)?.use(block)
    } catch (e: IOException) {
        null
    } catch (e: SecurityException) {
        // The picker's grant is gone (very old URI): nothing to read.
        null
    }

    private companion object {
        const val DIRECTORY = "media"
        const val QUARTER_TURN = 90f
        const val HALF_TURN = 180f
        const val THREE_QUARTER_TURN = 270f
    }
}
