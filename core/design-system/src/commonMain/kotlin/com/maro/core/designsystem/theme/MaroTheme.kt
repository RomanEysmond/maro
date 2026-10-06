package com.maro.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colors a messenger needs that Material 3 has no role for. */
@Immutable
data class MaroColors(
    /**
     * The "read" double tick. It has to stand out from the grey "sent / delivered" ticks on the user's own
     * bubbles in both themes; the theme's primary color is too close to those on a dark primary-container bubble.
     */
    val readReceipt: Color,
)

private val LightMaroColors = MaroColors(readReceipt = Color(0xFF1E88E5))
private val DarkMaroColors = MaroColors(readReceipt = Color(0xFF64B5F6))

private val LocalMaroColors = staticCompositionLocalOf { LightMaroColors }

/** App-wide Material 3 theme. Brand colors and typography will be customised here later. */
@Composable
fun MaroTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMaroColors provides if (darkTheme) DarkMaroColors else LightMaroColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
            content = content,
        )
    }
}

/** The extra colors of the current [MaroTheme]. */
object MaroTheme {
    val colors: MaroColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMaroColors.current
}
