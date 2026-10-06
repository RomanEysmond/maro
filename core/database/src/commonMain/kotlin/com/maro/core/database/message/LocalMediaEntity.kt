package com.maro.core.database.message

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * The device's own copy of a file the user sent (the compressed photo), keyed by message. A table of its own: the
 * sync rewrites message rows from the server, which knows nothing of local files, and would lose these.
 */
@Entity(tableName = "local_media")
data class LocalMediaEntity(
    @PrimaryKey val messageId: String,
    /** Absolute path in the app's own storage. */
    val path: String,
    /** The file is in the storage: the message can go out (and a retry does not upload it again). */
    val uploaded: Boolean = false,
)
