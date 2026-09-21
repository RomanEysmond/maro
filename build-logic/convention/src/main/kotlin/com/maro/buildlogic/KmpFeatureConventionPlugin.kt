package com.maro.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * `maro.kmp.feature`: the `presentation` module of a feature — Compose UI, MVI ViewModels,
 * type-safe navigation, Koin, and the shared `core` modules. Features never depend on each other.
 */
class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("maro.kmp.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            val catalog = libs
            configureExt<KotlinMultiplatformExtension> {
                sourceSets.getByName("commonMain").dependencies {
                    implementation(project(":core:domain"))
                    implementation(project(":core:presentation"))
                    implementation(project(":core:design-system"))

                    // Exposed in the public API of a feature: its nav graph and its Koin module.
                    api(catalog.library("navigation-compose"))
                    api(catalog.library("koin-core"))

                    implementation(catalog.library("koin-compose"))
                    implementation(catalog.library("koin-compose-viewmodel"))
                    implementation(catalog.library("lifecycle-viewmodel-compose"))
                    implementation(catalog.library("lifecycle-runtime-compose"))
                    implementation(catalog.library("kotlinx-serialization-json"))
                    implementation(catalog.library("kotlinx-coroutines-core"))
                }
            }
        }
    }
}
