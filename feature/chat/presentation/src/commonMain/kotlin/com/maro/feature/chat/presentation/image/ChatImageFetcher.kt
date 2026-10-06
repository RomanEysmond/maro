package com.maro.feature.chat.presentation.image

import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.Result
import com.maro.feature.chat.domain.ChatImage
import com.maro.feature.chat.domain.ChatImageUrls

/** Lets the app's image loader show [ChatImage]s: `components { addChatImages(urls) }` where the loader is built. */
fun ComponentRegistry.Builder.addChatImages(urls: ChatImageUrls): ComponentRegistry.Builder = apply {
    add(ChatImageKeyer())
    add(ChatImageFetcher.Factory(urls))
}

/** The storage key is the picture's identity: the signed URL is different on every request. */
internal class ChatImageKeyer : Keyer<ChatImage> {
    override fun key(data: ChatImage, options: Options): String = data.key
}

/** Thrown into the image loader when the server would not give a URL; the image then shows its error state. */
class ChatImageUnavailableException(val error: DataError.Network) : Exception("No URL for the picture: $error")

/**
 * A picture already in the disk cache is shown without asking anyone; otherwise our server signs a short-lived URL
 * and the loader's own network fetcher downloads it, caching it under the storage key (not the URL).
 */
internal class ChatImageFetcher(
    private val image: ChatImage,
    private val options: Options,
    private val imageLoader: ImageLoader,
    private val urls: ChatImageUrls,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val diskCache = imageLoader.diskCache
        diskCache?.openSnapshot(image.key)?.let { snapshot ->
            return SourceFetchResult(
                source = ImageSource(
                    file = snapshot.data,
                    fileSystem = diskCache.fileSystem,
                    diskCacheKey = image.key,
                    closeable = snapshot,
                ),
                mimeType = null,
                dataSource = DataSource.DISK,
            )
        }

        val url = when (val result = urls.downloadUrl(image)) {
            is Result.Success -> result.data
            is Result.Error -> throw ChatImageUnavailableException(result.error)
        }
        val withKey = options.copy(diskCacheKey = image.key)
        val data = imageLoader.components.map(url, withKey)
        val (network, _) = imageLoader.components.newFetcher(data, withKey, imageLoader) ?: return null
        return network.fetch()
    }

    class Factory(private val urls: ChatImageUrls) : Fetcher.Factory<ChatImage> {
        override fun create(data: ChatImage, options: Options, imageLoader: ImageLoader): Fetcher =
            ChatImageFetcher(data, options, imageLoader, urls)
    }
}
