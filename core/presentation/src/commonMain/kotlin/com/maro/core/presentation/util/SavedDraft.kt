package com.maro.core.presentation.util

import androidx.lifecycle.SavedStateHandle
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * What the user has typed on a screen, kept in [SavedStateHandle]: Android may kill the process while the app is in
 * the background (waiting for an SMS, switching apps), and the screen should come back as it was left.
 *
 * Stored as one JSON string, so a draft can be any `@Serializable` class without registering its fields one by one.
 * Drafts are small (a few form fields): the saved state of an activity is limited to a few hundred kilobytes.
 */
class SavedDraft<D>(
    private val handle: SavedStateHandle,
    private val serializer: KSerializer<D>,
    private val key: String = "draft",
) {
    /** The draft left before the process died, `null` on a fresh start (or after an incompatible app update). */
    fun restore(): D? =
        handle.get<String>(key)?.let { json -> runCatching { Json.decodeFromString(serializer, json) }.getOrNull() }

    fun save(draft: D) {
        handle[key] = Json.encodeToString(serializer, draft)
    }
}
