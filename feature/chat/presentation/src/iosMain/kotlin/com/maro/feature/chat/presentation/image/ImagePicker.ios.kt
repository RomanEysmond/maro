package com.maro.feature.chat.presentation.image

import androidx.compose.runtime.Composable

/** Not built yet: the iOS app is a stage of its own (PHPickerViewController, needs a Mac to try). */
@Composable
actual fun rememberImagePicker(onPicked: (source: String) -> Unit): () -> Unit = {}
