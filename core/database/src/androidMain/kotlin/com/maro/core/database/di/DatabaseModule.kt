package com.maro.core.database.di

import com.maro.core.database.MaroDatabase
import com.maro.core.database.buildDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single { buildDatabase(androidContext()) }
    single { get<MaroDatabase>().chatDao() }
    single { get<MaroDatabase>().messageDao() }
}
