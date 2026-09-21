import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-gradle-plugin`
    // Plain Kotlin (not `kotlin-dsl`): Gradle's embedded Kotlin cannot read the metadata of
    // Kotlin 2.3 Gradle plugins, so the convention plugins are compiled with the project's Kotlin.
    alias(libs.plugins.kotlin.jvm)
}

group = "com.maro.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "maro.android.application"
            implementationClass = "com.maro.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("kmpLibrary") {
            id = "maro.kmp.library"
            implementationClass = "com.maro.buildlogic.KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "maro.kmp.compose"
            implementationClass = "com.maro.buildlogic.KmpComposeConventionPlugin"
        }
        register("kmpFeature") {
            id = "maro.kmp.feature"
            implementationClass = "com.maro.buildlogic.KmpFeatureConventionPlugin"
        }
    }
}
