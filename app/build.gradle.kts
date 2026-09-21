plugins {
    id("maro.android.application")
    alias(libs.plugins.google.services)
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

    buildTypes {
        release {
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
    implementation(project(":core:design-system"))

    // Features: :app is the only place that knows all of them and wires them together
    implementation(project(":feature:auth:presentation"))
    implementation(project(":feature:chatlist:presentation"))
    implementation(project(":feature:chat:presentation"))
    implementation(project(":feature:profile:presentation"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.android)

    // XML theme parent for Theme.Maro
    implementation(libs.google.material)

    // Firebase (wired up in stage 2)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
