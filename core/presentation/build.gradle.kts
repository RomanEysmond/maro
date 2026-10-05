plugins {
    id("maro.kmp.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:domain"))
        implementation(libs.lifecycle.runtime.compose)
        implementation(libs.kotlinx.coroutines.core)
        // SavedDraft: a screen's input kept in SavedStateHandle as JSON.
        api(libs.lifecycle.viewmodel.savedstate)
        implementation(libs.kotlinx.serialization.json)
        // LocalDate is part of the date formatting API the features use.
        api(libs.kotlinx.datetime)
    }
}
