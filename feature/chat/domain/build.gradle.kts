plugins {
    id("maro.kmp.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:domain"))
        api(libs.kotlinx.coroutines.core)
        // PagingData is part of MessageRepository's API; paging-common is plain Kotlin (no Android, no Compose).
        api(libs.androidx.paging.common)
    }
}
