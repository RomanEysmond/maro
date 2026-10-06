package com.maro.feature.chat.presentation.image

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.maro.feature.chat.domain.MessageImage
import com.maro.feature.chat.presentation.generated.resources.Res
import com.maro.feature.chat.presentation.generated.resources.chat_photo
import org.jetbrains.compose.resources.stringResource

/**
 * A photo in the conversation. It has its final shape from the start (the size travels with the message), so the
 * list does not jump when the picture arrives. The sender sees their own copy at once, before any upload.
 */
@Composable
fun ChatPicture(image: MessageImage, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    // Very tall or wide pictures are cropped to a sane bubble; the full one is a tap away.
    val ratio = (image.width.toFloat() / image.height.coerceAtLeast(1)).coerceIn(MIN_RATIO, MAX_RATIO)
    AsyncImage(
        model = image.localPath ?: image.source,
        contentDescription = stringResource(Res.string.chat_photo),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .width(240.dp)
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    )
}

private const val MIN_RATIO = 0.6f
private const val MAX_RATIO = 1.8f
