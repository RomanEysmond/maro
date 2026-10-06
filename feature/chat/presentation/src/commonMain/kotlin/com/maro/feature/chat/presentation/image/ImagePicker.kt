package com.maro.feature.chat.presentation.image

import androidx.compose.runtime.Composable

/**
 * Opens the platform's photo picker; [onPicked] gets what it returned (on Android a content URI), nothing when the
 * user backs out. Returns the function that opens it.
 */
@Composable
expect fun rememberImagePicker(onPicked: (source: String) -> Unit): () -> Unit
