package com.maro.core.presentation.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * A string that is either dynamic or comes from a (localizable) string resource.
 * Use plain `String` for values that are always dynamic (names, formatted dates, amounts).
 */
sealed interface UiText {
    data class DynamicString(val value: String) : UiText

    class Resource(
        val resource: StringResource,
        val args: Array<Any> = emptyArray(),
    ) : UiText

    @Composable
    fun asString(): String = when (this) {
        is DynamicString -> value
        is Resource -> stringResource(resource, *args)
    }

    suspend fun asStringAsync(): String = when (this) {
        is DynamicString -> value
        is Resource -> getString(resource, *args)
    }
}
