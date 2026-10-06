package com.maro.feature.chat.presentation.image

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.maro.feature.chat.domain.ChatImage
import com.maro.feature.chat.presentation.generated.resources.Res
import com.maro.feature.chat.presentation.generated.resources.chat_image_close
import com.maro.feature.chat.presentation.generated.resources.chat_photo
import org.jetbrains.compose.resources.stringResource

/**
 * A photo on its own, the whole screen, pinch to zoom (up to [MAX_ZOOM]) and drag around when zoomed. Pure UI:
 * the picture comes from the same loader and cache as in the conversation.
 */
@Composable
fun ImageViewerScreen(image: ChatImage, localPath: String?, onBackClick: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
        // Back at full size, the picture snaps back to the middle.
        offset = if (scale == 1f) Offset.Zero else offset + pan
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = localPath ?: image,
            contentDescription = stringResource(Res.string.chat_photo),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .transformable(transform),
        )
        IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.TopStart).safeDrawingPadding()) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.chat_image_close),
                tint = Color.White,
            )
        }
    }
}

private const val MAX_ZOOM = 5f
