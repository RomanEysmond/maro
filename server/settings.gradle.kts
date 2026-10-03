// The push mini-server: a separate Gradle build next to the app, so Android Studio's sync and the app build never see
// Netty or the Firebase Admin SDK. It shares the app's version catalog. Run from the repository root:
//   ./gradlew -p server run     (needs GOOGLE_APPLICATION_CREDENTIALS, see server/README.md)
//   ./gradlew -p server test
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "maro-server"
