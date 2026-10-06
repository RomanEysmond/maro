package com.maro.buildlogic

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/**
 * `maro.code.quality`: ktlint (formatting, `ktlintCheck` / `ktlintFormat`) and detekt (static analysis, `detekt`)
 * for every module; applied by the other convention plugins. The code style lives in `.editorconfig`, the detekt
 * rules in `config/detekt/detekt.yml` (on top of detekt's defaults).
 */
class CodeQualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")
            pluginManager.apply("io.gitlab.arturbosch.detekt")

            val catalog = libs
            configureExt<KtlintExtension> {
                version.set(catalog.findVersion("ktlint").get().requiredVersion)
                android.set(true)
                // Generated sources (Room, Compose resources, BuildConfig) are not ours to format.
                filter {
                    it.exclude { element -> element.file.path.contains("${java.io.File.separator}build${java.io.File.separator}") }
                }
            }

            configureExt<DetektExtension> {
                buildUponDefaultConfig = true
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
                // Every source set of a KMP module (commonMain, androidMain, tests), not only src/main.
                source.setFrom(files("src"))
                parallel = true
            }

            // detekt 1.23.8 runs on the Kotlin it was built with; without this Gradle would hand it ours (newer)
            // and it refuses to start. Only detekt's own classpath is affected, not the app's.
            val detektKotlin = catalog.findVersion("detektKotlin").get().requiredVersion
            configurations.matching { it.name == "detekt" }.configureEach { configuration ->
                configuration.resolutionStrategy.eachDependency { details ->
                    if (details.requested.group == "org.jetbrains.kotlin") details.useVersion(detektKotlin)
                }
            }
        }
    }
}
