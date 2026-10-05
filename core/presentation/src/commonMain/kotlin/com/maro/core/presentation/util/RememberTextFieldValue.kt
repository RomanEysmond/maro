package com.maro.core.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * The field's value for a text the ViewModel owns. A text field fed a plain String puts the cursor at the start of
 * any text it did not type itself, so typing into a restored draft or a pre-filled name would go in front of it. Here
 * text that comes from outside (a restored draft, the stored profile, a cleared field) gets the cursor at its end;
 * the user's own edits keep their cursor and selection.
 *
 * Usage: `val name = rememberTextFieldValue(state.name)`, then `value = name.value` and
 * `onValueChange = { name.value = it; onAction(OnNameChange(it.text)) }`.
 */
@Composable
fun rememberTextFieldValue(text: String): MutableState<TextFieldValue> {
    val state = remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    if (state.value.text != text) {
        state.value = TextFieldValue(text, TextRange(text.length))
    }
    return state
}
