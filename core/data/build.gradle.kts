plugins {
    id("maro.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
        }
        // Firebase is Android-only for now; iOS gets its own implementations of the same interfaces later.
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            implementation(libs.koin.core)
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
