import java.util.Properties

plugins {
    id("maro.android.application")
    alias(libs.plugins.google.services)
}

// Machine-local settings: local.properties is not in git (the repository is public).
val localProperties: Properties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

fun localProperty(name: String): String? = localProperties.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }

// The hosted push server for release builds: `maro.pushServerUrl=https://...`. Without it release builds have no
// pushes; messages still arrive through sync.
val releasePushServerUrl: String = localProperty("maro.pushServerUrl").orEmpty()
require(releasePushServerUrl.isEmpty() || releasePushServerUrl.startsWith("https://")) {
    "maro.pushServerUrl must be an https:// address"
}

// The release key: `maro.signing.storeFile` (path to the keystore, outside the repository), `maro.signing.storePassword`,
// `maro.signing.keyAlias`, `maro.signing.keyPassword`. Without them a release build is signed with the debug key: good
// for checking R8 on a device, never for distribution (see docs/RELEASE.md).
val releaseSigning: Map<String, String>? = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .associateWith { localProperty("maro.signing.$it") }
    .takeIf { values -> values.values.all { it != null } }
    ?.mapValues { it.value!! }

android {
    namespace = "com.maro"

    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = file(releaseSigning.getValue("storeFile"))
                storePassword = releaseSigning.getValue("storePassword")
                keyAlias = releaseSigning.getValue("keyAlias")
                keyPassword = releaseSigning.getValue("keyPassword")
            }
        }
    }

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
            signingConfig = if (releaseSigning != null) {
                signingConfigs.getByName("release")
            } else {
                logger.warn("maro: no release key in local.properties, the release build is signed with the debug key")
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
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
