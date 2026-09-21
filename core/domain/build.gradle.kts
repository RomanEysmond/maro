plugins {
    id("maro.kmp.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        // StateFlow is part of the public API of the profile repository
        api(libs.kotlinx.coroutines.core)
    }
}
