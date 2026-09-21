package com.maro.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * `maro.kmp.library`: a Kotlin Multiplatform library with an Android target and iOS targets.
 * The iOS targets are declared now so the code stays multiplatform-clean; they can only be built on a Mac
 * (on other hosts Kotlin skips them, see `kotlin.native.ignoreDisabledTargets` in gradle.properties).
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")

            val catalog = libs
            configureExt<LibraryExtension> {
                namespace = androidNamespace()
                compileSdk = catalog.intVersion("compileSdk")

                defaultConfig {
                    minSdk = catalog.intVersion("minSdk")
                }
                compileOptions {
                    sourceCompatibility = JAVA_VERSION
                    targetCompatibility = JAVA_VERSION
                }
            }

            configureExt<KotlinMultiplatformExtension> {
                androidTarget {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_17)
                    }
                }
                iosArm64()
                iosSimulatorArm64()

                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }

                sourceSets.getByName("commonTest").dependencies {
                    implementation(catalog.library("kotlin-test"))
                    implementation(catalog.library("kotlinx-coroutines-test"))
                    implementation(catalog.library("turbine"))
                    implementation(catalog.library("assertk"))
                }
            }
        }
    }
}
