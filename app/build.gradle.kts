import java.util.Properties

plugins {
    id("maro.android.application")
    alias(libs.plugins.google.services)
}

// The hosted push server for release builds: `maro.pushServerUrl=https://...` in local.properties (not in git, the
// repository is public). Without it release builds have no pushes; messages still arrive through sync.
val releasePushServerUrl: String = rootProject.file("local.properties")
    .takeIf { it.exists() }
    ?.let { file -> Properties().apply { file.inputStream().use { load(it) } } }
    ?.getProperty("maro.pushServerUrl")
    ?.trim()
    .orEmpty()
require(releasePushServerUrl.isEmpty() || releasePushServerUrl.startsWith("https://")) {
    "maro.pushServerUrl must be an https:// address"
}

android {
    namespace = "com.maro"

    defaultConfig {
        applicationId = "com.maro"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            // The push server on the development machine, as the emulator sees it (see server/README.md).
            buildConfigField("String", "PUSH_SERVER_URL", "\"http://10.0.2.2:8080\"")
        }
        release {
            buildConfigField("String", "PUSH_SERVER_URL", "\"$releasePushServerUrl\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    // Shared
    implementation(project(":core:domain"))
    implementation(project(":core:design-system"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))

    // Features: :app is the only place that knows all of them and wires them together
    implementation(project(":feature:auth:domain"))
    implementation(project(":feature:auth:data"))
    implementation(project(":feature:auth:presentation"))
    implementation(project(":feature:chatlist:data"))
    implementation(project(":feature:chatlist:presentation"))
    implementation(project(":feature:chat:domain"))
    implementation(project(":feature:chat:data"))
    implementation(project(":feature:chat:presentation"))
    implementation(project(":feature:profile:data"))
    implementation(project(":feature:profile:presentation"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    // XML theme parent for Theme.Maro
    implementation(libs.google.material)

    // Firebase Auth and Firestore live in :feature:auth:data; messaging comes with the push stage
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
