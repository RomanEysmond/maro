package com.maro.core.database.di

import com.maro.core.database.MaroDatabase
import com.maro.core.database.buildDatabase
import com.maro.core.domain.session.SignOutCleaner
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val databaseModule = module {
    single { buildDatabase(androidContext()) }
    single { get<MaroDatabase>().chatDao() }
    single { get<MaroDatabase>().messageDao() }
    // Sign-out: chats, messages, members, drafts and sync cursors of the previous user go. Named: every layer adds
    // its own SignOutCleaner, and the app collects them all with getAll().
    single<SignOutCleaner>(named("database")) { SignOutCleaner { get<MaroDatabase>().sessionDao().clearAll() } }
}
