plugins {
    id("maro.kmp.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:domain"))
    }
}
