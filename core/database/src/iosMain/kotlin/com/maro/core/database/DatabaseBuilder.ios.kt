package com.maro.core.database

import androidx.room3.Room
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** The Room compiler generates the `actual` for [MaroDatabaseConstructor] from the `@ConstructedBy` annotation. */
@OptIn(ExperimentalForeignApi::class)
fun buildDatabase(): MaroDatabase {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    val path = requireNotNull(documentDirectory?.path) + "/$DATABASE_FILE_NAME"
    return Room.databaseBuilder<MaroDatabase>(name = path).withDefaults()
}
