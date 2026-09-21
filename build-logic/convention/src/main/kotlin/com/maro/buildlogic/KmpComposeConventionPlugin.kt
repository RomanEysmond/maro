package com.maro.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** `maro.kmp.compose`: a `maro.kmp.library` with Compose Multiplatform (Material 3 + resources). */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("maro.kmp.library")
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            val catalog = libs
            configureExt<KotlinMultiplatformExtension> {
                sourceSets.getByName("commonMain").dependencies {
                    // `api`: Compose types appear in the public signatures of these modules.
                    api(catalog.library("compose-runtime"))
                    api(catalog.library("compose-foundation"))
                    api(catalog.library("compose-ui"))
                    api(catalog.library("compose-material3"))
                    api(catalog.library("compose-material-icons-extended"))
                    api(catalog.library("compose-components-resources"))
                    api(catalog.library("compose-components-ui-tooling-preview"))
                }
            }
            dependencies.add("debugImplementation", catalog.library("compose-ui-tooling"))

            // Every module has its own generated `Res` class; the package must be unique per module.
            val resPackage = "${androidNamespace()}.generated.resources"
            configureExt<ComposeExtension> {
                (this as ExtensionAware).extensions.configure(
                    ResourcesExtension::class.java,
                ) { resources ->
                    resources.packageOfResClass = resPackage
                    resources.publicResClass = true
                }
            }
        }
    }
}
