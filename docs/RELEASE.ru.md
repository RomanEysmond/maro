# Release-сборка

*English version: [RELEASE.md](RELEASE.md).*

Release-сборка сжимается R8 (`isMinifyEnabled`, `isShrinkResources`) и подписывается ключом, указанным в
`local.properties`. Этот файл не попадает в git (репозиторий публичный): ключ, его пароли и адрес сервера пушей в
репозиторий никогда не попадают.

## 1. Создать ключ подписи (один раз)

Android Studio: **Build → Generate Signed App Bundle / APK → Android App Bundle → Next → Create new…**. Или в
терминале (`keytool` входит в JDK, пароли он спросит сам):

```bash
keytool -genkeypair -v -keystore maro-release.jks -alias maro -keyalg RSA -keysize 4096 -validity 10000
```

- Храните файл `.jks` **вне репозитория** и сделайте **резервную копию** (второй диск, менеджер паролей): без ключа
  приложение, которое раздаётся APK-файлом, нельзя обновить (людям придётся удалить его вместе с данными), а в
  Google Play придётся просить поддержку сбросить upload key.
- Создайте его до первой сборки, которую установит кто-то ещё. До тех пор release подписывается debug-ключом
  (Gradle об этом пишет: `maro: no release key in local.properties…`) — это годится только для проверки на своих
  устройствах.

## 2. Указать ключ сборке

Добавьте в `local.properties` в корне репозитория:

```properties
maro.signing.storeFile=/абсолютный/путь/к/maro-release.jks
maro.signing.storePassword=...
maro.signing.keyAlias=maro
maro.signing.keyPassword=...
```

Нужны все четыре строки; если какой-то нет, используется debug-ключ.

## 3. Добавить ключ в Firebase

Вход по телефону проверяет сертификат, которым подписано приложение. Без этого шага вход по SMS в release не работает.

1. `./gradlew :app:signingReport` — скопируйте **SHA-1** и **SHA-256** варианта `release`.
2. Firebase Console → Project settings → Your apps → Android `com.maro` → **Add fingerprint** (оба).
3. Только для Google Play: после первой загрузки в Play Console → *Test and release → App integrity → App signing*
   будет **app signing key**, которым Google подписывает устанавливаемое приложение. Его SHA-1 и SHA-256 тоже
   добавьте в Firebase.

Скачивать `google-services.json` заново не нужно.

## 4. Собрать

```bash
./gradlew :app:assembleRelease
```

→ `app/build/outputs/apk/release/app-release.apk`: APK для установки напрямую (`adb install` или отправить файлом).

```bash
./gradlew :app:bundleRelease
```

→ `app/build/outputs/bundle/release/app-release.aab`: для Google Play (Play App Signing; ваш ключ — upload key).

Перед каждой публикуемой сборкой увеличивайте `versionCode` (и `versionName`) в `app/build.gradle.kts`.

## Пуши

Release-сборка шлёт пуши, только если в `local.properties` есть `maro.pushServerUrl=https://...` (сервер на хостинге,
см. [server/README.ru.md](../server/README.ru.md)). Без него сообщения всё равно приходят — через синхронизацию, просто
без уведомлений.

## Если R8 что-то сломал

Признаки: падение только в release (`ClassNotFoundException`, `SerializationException`, нет конструктора). Все
используемые библиотеки (Firebase, Room, kotlinx.serialization, Ktor, Koin, WorkManager) приносят свои правила R8, поэтому
`app/proguard-rules.pro` пуст. Добавьте туда правило `-keep` для класса из стектрейса, а сам стектрейс расшифруйте по
`app/build/outputs/mapping/release/mapping.txt` (сохраняйте этот файл для каждой опубликованной сборки: Play Console
принимает его для читаемых отчётов о падениях).
