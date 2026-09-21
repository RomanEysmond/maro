plugins {
    id("maro.kmp.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:domain"))
        implementation(project(":core:data"))
        implementation(project(":feature:chat:domain"))
    }
}
