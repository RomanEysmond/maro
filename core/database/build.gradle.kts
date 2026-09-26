plugins {
    id("maro.kmp.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            api(libs.androidx.room.runtime)
            // MessageDao hands out a PagingSource; paging-common is pinned to the version the UI uses.
            api(libs.androidx.room.paging)
            api(libs.androidx.paging.common)
            implementation(libs.androidx.sqlite.bundled)
        }
        androidMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.koin.android)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}
