plugins {
    id("maro.kmp.feature")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":feature:chat:domain"))
        implementation(libs.androidx.paging.compose)
        // Photos in the conversation (AsyncImage) and the loader component for chat pictures.
        implementation(libs.coil.compose)
    }
    sourceSets.androidMain.dependencies {
        // The system Photo Picker (rememberLauncherForActivityResult).
        implementation(libs.androidx.activity.compose)
    }
}
