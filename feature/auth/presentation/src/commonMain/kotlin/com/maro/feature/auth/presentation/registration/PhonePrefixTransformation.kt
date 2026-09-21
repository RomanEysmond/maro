package com.maro.feature.auth.presentation.registration

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/** Shows "+7 " in front of the typed digits without making it part of the edited value. */
internal object PhonePrefixTransformation : VisualTransformation {
    private const val PREFIX = RegistrationState.COUNTRY_PREFIX + " "

    override fun filter(text: AnnotatedString): TransformedText {
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + PREFIX.length
            override fun transformedToOriginal(offset: Int): Int = (offset - PREFIX.length).coerceAtLeast(0)
        }
        return TransformedText(AnnotatedString(PREFIX) + text, offsetMapping)
    }
}
