package com.maro.core.database

import android.content.Context
import androidx.room3.Room

/** The Room compiler generates the `actual` for [MaroDatabaseConstructor] from the `@ConstructedBy` annotation. */
fun buildDatabase(context: Context): MaroDatabase =
    Room.databaseBuilder<MaroDatabase>(
        context = context.applicationContext,
        name = context.getDatabasePath(DATABASE_FILE_NAME).absolutePath,
    ).withDefaults()
