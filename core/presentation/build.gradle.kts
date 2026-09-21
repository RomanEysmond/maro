plugins {
    id("maro.kmp.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:domain"))
        implementation(libs.lifecycle.runtime.compose)
        implementation(libs.kotlinx.coroutines.core)
    }
}
