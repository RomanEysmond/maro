# Release build

*Русская версия: [RELEASE.ru.md](RELEASE.ru.md).*

The release build is shrunk by R8 (`isMinifyEnabled`, `isShrinkResources`) and signed with the key named in
`local.properties`. That file is not in git (the repository is public): the key, its passwords and the push server
address never go into the repository.

## 1. Create the signing key (once)

Android Studio: **Build → Generate Signed App Bundle / APK → Android App Bundle → Next → Create new…**. Or in a
terminal (`keytool` comes with the JDK; it asks for the passwords itself):

```bash
keytool -genkeypair -v -keystore maro-release.jks -alias maro -keyalg RSA -keysize 4096 -validity 10000
```

- Keep the `.jks` file **outside the repository** and make a **backup** (a second disk, a password manager): without
  the key, an app distributed as APK can never be updated (people would have to uninstall it, losing their data), and
  on Google Play you would have to ask support to reset the upload key.
- Create it before the first build that anybody else installs. Until then the release build is signed with the debug
  key (Gradle says so: `maro: no release key in local.properties…`), which is fine for testing on your own devices only.

## 2. Point the build at it

Add to `local.properties` in the repository root:

```properties
maro.signing.storeFile=/absolute/path/to/maro-release.jks
maro.signing.storePassword=...
maro.signing.keyAlias=maro
maro.signing.keyPassword=...
```

All four are needed; with any of them missing the debug key is used.

## 3. Register the key with Firebase

Phone sign-in checks the app's signing certificate. Without this step SMS sign-in fails in the release build.

1. `./gradlew :app:signingReport` — copy **SHA-1** and **SHA-256** of the `release` variant.
2. Firebase Console → Project settings → Your apps → Android `com.maro` → **Add fingerprint** (both).
3. Google Play only: after the first upload, Play Console → *Test and release → App integrity → App signing* shows the
   **app signing key** Google signs the installed app with. Add its SHA-1 and SHA-256 to Firebase as well.

`google-services.json` does not need to be downloaded again.

## 4. Build

```bash
./gradlew :app:assembleRelease
```

→ `app/build/outputs/apk/release/app-release.apk`: an APK to install directly (`adb install`, or send the file).

```bash
./gradlew :app:bundleRelease
```

→ `app/build/outputs/bundle/release/app-release.aab`: for Google Play (Play App Signing; your key is the upload key).

Before each published build raise `versionCode` (and `versionName`) in `app/build.gradle.kts`.

## Push notifications

The release build sends pushes only when `maro.pushServerUrl=https://...` is in `local.properties` (the hosted
server, see [server/README.md](../server/README.md)). Without it messages still arrive, through sync, just without
notifications.

## If R8 breaks something

Symptoms: a crash only in release (`ClassNotFoundException`, `SerializationException`, a missing constructor). The
libraries in use (Firebase, Room, kotlinx.serialization, Ktor, Koin, WorkManager) ship their own R8 rules, so
`app/proguard-rules.pro` is empty. Add a `-keep` rule there for the class in the stack trace, and decode the trace with
`app/build/outputs/mapping/release/mapping.txt` (keep this file for every published build: Play Console accepts it
for readable crash reports).
