package com.maro.buildlogic

import com.android.build.api.dsl.Lint
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider

internal val JAVA_VERSION = JavaVersion.VERSION_17

internal val Project.libs: VersionCatalog
    get() = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow { IllegalStateException("Library '$alias' is missing in libs.versions.toml") }

internal fun VersionCatalog.intVersion(alias: String): Int =
    findVersion(alias).orElseThrow { IllegalStateException("Version '$alias' is missing in libs.versions.toml") }
        .requiredVersion.toInt()

/** Kotlin-DSL-like `configure<T> { ... }` for plain (non `kotlin-dsl`) plugin code. */
internal inline fun <reified T : Any> Project.configureExt(noinline block: T.() -> Unit) {
    extensions.configure(T::class.java, Action { it.block() })
}

/**
 * `:feature:auth:presentation` -> `com.maro.feature.auth.presentation`.
 * Used as the Android namespace and as the base for the generated Compose `Res` class package,
 * so it is unique per module.
 */
internal fun Project.androidNamespace(): String =
    "com.maro." + path.removePrefix(":").split(":").joinToString(".") { it.replace('-', '_') }

/** Android lint for every module (`lintDebug`): errors fail the build, warnings are reported. */
internal fun Lint.configureMaroLint() {
    abortOnError = true
    // Library, AGP and target SDK versions are raised deliberately, in a stage of their own: the toolchain has to
    // stay compatible with the owner's Android Studio (see CLAUDE.md).
    disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
    // Themed icons (Android 13+) need a monochrome version of the logo, which does not exist yet.
    disable += "MonochromeLauncherIcon"
}
