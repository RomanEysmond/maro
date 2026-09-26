plugins {
    id("maro.kmp.feature")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":feature:chat:domain"))
        implementation(libs.androidx.paging.compose)
    }
}
