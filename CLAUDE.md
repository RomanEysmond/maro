# Maro — контекст проекта

Мессенджер (в духе Telegram/WhatsApp) на Kotlin Multiplatform + Compose Multiplatform. Сейчас собирается только Android,
структура готова под iOS. Репозиторий публичный: **секреты, ключи, тестовые номера и пути на машине разработчика сюда не пишем**.

- Репозиторий: `github.com/RomanEysmond/maro`, пакет/applicationId: `com.maro`.
- Язык общения с владельцем проекта: русский.
- Firebase-проект создан (тариф **Spark**, без Blaze). `app/google-services.json` лежит на диске, но **в git не попадает** (`.gitignore`).

## Правила работы
- Каждый этап в отдельной ветке `stage-N-<name>`, в конце пул-реквест в `main`. Коммитить и пушить только по просьбе владельца;
  пуш он делает сам из Android Studio (у Claude нет доступа к GitHub). Сообщения коммитов: `Stage N: <кратко>`.
- Сначала план и вопросы, потом код. Работать строго по этапам; неясное уточнять, допущения называть явно.
- Новые зависимости и смену архитектуры согласовывать.
- Следовать личным скиллам владельца: `android-module-structure`, `android-di-koin`, `android-navigation`,
  `android-presentation-mvi`, `android-error-handling`, `android-testing`, `android-compose-ui`, `android-data-layer`.
- После этапа проверять: `assembleDebug`, `testDebugUnitTest`, запуск на эмуляторе (через `adb`), только потом отчёт.
- Перед повышением AGP/Gradle проверять совместимость с установленной Android Studio (нужна 2025.1.3+ для AGP 8.13).

## Архитектурные решения
- **Offline-first:** Room — единственный источник данных для UI. Поток: Remote → Repository → Room → Flow → ViewModel → UI.
  Синхронизация по курсору + догоняющая синхронизация при старте и восстановлении сети; WebSocket/Firestore-слушатели
  только ускоряют, надёжность даёт курсор. Оптимистичная отправка (SENDING → SENT → DELIVERED → READ / FAILED), ретраи через
  WorkManager (за интерфейсом, реализация в `androidMain`). Идемпотентность: `client_id` (UUID) = id документа на сервере.
- **Статусы прочтения:** `last_read_message_id` у участника чата, а не флаг на каждом сообщении. «Печатает…» и онлайн — эфемерные
  данные, не в Room.
- **UI:** Compose Multiplatform, Material 3, MVI (`State` / `Action` / `Event` / `ViewModel`), пары `<Screen>Root` + `<Screen>Screen`.
- **DI:** Koin (`viewModelOf`, `singleOf`), модули `<feature><Layer>Module`, собираются только в `:app` (`MaroApplication`).
- **Навигация:** type-safe маршруты (`@Serializable`, `<Screen>Route`), граф каждой фичи `NavGraphBuilder.<feature>Graph(...)`,
  переходы между фичами только колбэками, связываются в `:app/navigation/NavigationRoot`.
- **Ошибки:** `Result<D, E : Error>` (`Result.Success` / `Result.Error`) в `:core:domain`, `DataError`, `EmptyResult`;
  ожидаемые сбои не бросаем; в UI через `UiText` (`DynamicString` / `Resource` — вариант под KMP) и `*.toUiText()`.
- **Backend за интерфейсами** (`*RemoteDataSource`, `PushRegistrar` и т.п.), чтобы позже заменить Firebase на свой сервер
  (Ktor + PostgreSQL + S3 + WebSocket) без переписывания UI, ViewModel, Room и Repository.
- **Firebase (этап 1 бэкенда):** Auth (Phone), Firestore — только как транспорт (слушатели пишут в Room; собственный
  офлайн-кэш Firestore отключить), Security Rules обязательны. **Cloud Functions и Firebase Storage недоступны на Spark.**
- **Пуши:** только data-сообщения без текста (`chatId`/`messageId`). Отправка — отдельным мини-сервером (Ktor + Firebase Admin SDK),
  пока нет Blaze; до него сообщения доставляются слушателями + догоняющей синхронизацией.
- **E2E-шифрование в v1 не делаем**, но не закрываем путь к нему: тело сообщения — отдельное поле, сервер его не разбирает,
  интерфейс `MessageCipher` (пока no-op), интерфейс `SecureStorage` (Keystore), поля `type` и `schemaVersion`,
  медиа как непрозрачные файлы. Если делать — только готовая библиотека (libsignal), не самописная криптография.
- **KMP:** всё, кроме `:app`, — KMP-библиотеки (`commonMain`, таргеты `androidTarget` + `iosArm64` + `iosSimulatorArm64`).
  Платформенное (WorkManager, FCM, Firebase SDK, Keystore) — за интерфейсами в `androidMain`. iOS-приложения (`iosApp`, сборка
  фреймворка, iOS-реализации Firebase/пушей) пока нет: это отдельный этап, когда появится Mac и Xcode.

## Структура проекта
```
:app                      Application + Koin, MainActivity, NavigationRoot (единственный Android-only модуль)
build-logic               convention plugins: maro.android.application, maro.kmp.library, maro.kmp.compose, maro.kmp.feature
:core:domain              Result, Error, DataError, профиль пользователя (без Compose и Android)
:core:data                Firebase-объекты (androidMain), Task.await(); позже Ktor-клиент, safeCall
:core:database            Room 3 (KMP): MaroDatabase, ChatDao/ChatEntity; источник данных для UI (этап 3)
:core:presentation        UiText, ObserveAsEvents, DataError.toUiText() (+ строки ошибок)
:core:design-system       MaroTheme (светлая/тёмная), InitialsAvatar, общие ресурсы (logo.png)
:feature:auth:{domain,data,presentation}
:feature:chatlist:{domain,data,presentation}
:feature:chat:{domain,data,presentation}
:feature:profile:{domain,data,presentation}
```
Правила зависимостей: `presentation` → `domain` своей фичи + `core:*`; `data` → `domain` своей фичи + `core:domain`/`core:data`/`core:database`;
`domain` → только `core:domain`; фичи друг от друга не зависят (общее выносится в `core:domain` / `core:presentation`);
`:app` знает всё. Модули `domain`/`data` у `feature:chat` и `feature:profile:domain` пока пустые заготовки.
Пакеты модулей: `com.maro.<путь модуля>`; пакет сгенерированного `Res` у каждого модуля уникален
(`com.maro.<путь>.generated.resources`, дефис → подчёркивание, например `com.maro.core.design_system...`).

## Что уже реализовано
- **Этап 0** (в `main`): пакет `com.maro`, Gradle Kotlin DSL + Version Catalog, Material 3, чистка зависимостей.
- **Этап 1** (ветка `stage-1-skeleton`, коммит `2614c1f`, проверен на эмуляторе): модули, build-logic, Koin, навигация, MVI регистрации.
- Экраны: Welcome; Registration (MVI: имя, фамилия, телефон = 10 цифр после постоянного префикса «+7», нормализация вставки
  `+7…`/`8…`, кнопка закреплена над клавиатурой); список чатов (заглушка «Место в разработке» + боковое меню Профиль/Настройки/Справка);
  Chat (заглушка); Profile / Settings / Help (+3 пустых подэкрана) — статичный UI с демо-данными «Иван Иванов».
- Навигация: `AuthGraphRoute` (Welcome → Registration) → `ChatListGraphRoute`; `ProfileRoute`, `SettingsRoute`, `HelpRoute`
  (+ `HelpChangedPhoneRoute`, `HelpHideLastSessionRoute`, `HelpCreateGroupChatRoute`), `ChatRoute(chatId)`.
  Стартовый маршрут выбирается по `isLoggedIn` (с этапа 2 — реальная сессия Firebase). После регистрации экран
  входа убирается из back stack.
- Koin: зарегистрирован только `authPresentationModule` (`RegistrationViewModel`).
- Тесты: 11 (`ResultTest` — 3, `RegistrationViewModelTest` — 8), все зелёные.
- **Этап 2** (ветка `stage-2-phone-auth`, не закоммичен; сборка и 50 юнит-тестов зелёные; на эмуляторе проверены: SMS-вход (тестовый номер Firebase), экран «О себе», профиль, редактирование и смена @username, сохранение сессии после перезапуска, выход, повторный вход существующего пользователя —
  для SMS на +7 в Firebase включён регион «Россия»): `PhoneAuthenticator` / `SessionRepository` в `feature:auth:domain`;
  Firebase-реализации в `feature:auth:data/androidMain` (`ActivityProvider` даёт Activity для `verifyPhoneNumber`; Firestore с
  memory-кэшем); экран `VerifyCode` (6 цифр, автоотправка, таймер повтора 60 с);
  имя/телефон едут в `VerifyCodeRoute`. Повторный вход: существующий `users/{uid}` побеждает, форма нужна только новым.
  `isLoggedIn` — `StateFlow` из Firebase Auth (старт без сплэша), выход через колбэк `onLogout` в `profileGraph` → `MainViewModel`
  в `:app`; при `false` навигация сбрасывает back stack на `AuthGraphRoute`. `firestore.rules` в корне — **опубликовать вручную**
  в консоли. Префикс «+7» показывается постоянно (`PhonePrefixTransformation`).
- **Этап 2, профиль:** `UserProfile` / `UserProfileRepository` (+ `BirthDate`, `ProfileUpdate`, `ProfileRules`, `ProfileError`) живут в
  `:core:domain` (профиль нужен и auth, и profile, а фичи друг от друга не зависят); реализация на Firestore — в
  `feature:profile:data` (`profileDataModule`), общие `FirebaseAuth`/`FirebaseFirestore` и `Task.await()` — в `:core:data`
  (`firebaseCoreModule`). Репозиторий — singleton с кэшем `profile: StateFlow`, экраны на него подписаны. `ensureProfile` возвращает
  `isNew`: новый пользователь после кода попадает на `ProfileSetupRoute` («Расскажите о себе»: bio, @username, дата рождения,
  «Пропустить»), существующий — сразу в чаты. Редактирование (`EditProfileRoute`, те же поля + имя/фамилия) открывается пунктом «Учётная запись» в
  `ProfileScreen` (отдельной кнопки нет), который показывает реальные данные. Аватар — заглушка с инициалами (`InitialsAvatar`), фото ждёт этапа 8 (Storage
  на Spark недоступен). Уникальность @username: документ `usernames/{name}` → uid, claim и смена в одной транзакции; правила это
  проверяют (`isUsernameClaimed`). Правила `firestore.rules` опубликованы владельцем в консоли (при изменении — публиковать заново вручную).
- Firebase `auth`/`firestore` подключены в `feature:auth:data`, `messaging` — в `:app` (пока не используется); плагин `google-services` в `:app`.
- **Этап 3** (ветка `stage-3-chatlist`, не закоммичен; сборка и 60 юнит-тестов зелёные; на эмуляторе проверены: пустой список
  («Пока нет чатов»), ошибочное состояние офлайн («Нет подключения к интернету» + «Повторить»), сессия и Room переживают
  перезапуск приложения; правила Firestore для `chats` опубликованы, после этого список открывается без ошибок): `:core:database` — Room 3.0 (`androidx.room3`, новые координаты, KMP-first, пришёл на смену
  `androidx.room` 2.x) + KSP; `MaroDatabase` (`@ConstructedBy`-конструктор генерируется компилятором, не пишется руками),
  `ChatEntity`/`ChatDao` (`replaceAll` = upsert + удаление лишнего, курсора не нужно — Firestore-листенер шлёт весь
  актуальный список целиком, не дельты). `Chat`/`ChatParticipant`/`LastMessage`/`ChatRepository` в `feature:chatlist:domain`
  (данные чата, а не только сообщений; группы — только поле `type`, сама логика групп на этапе 6). `feature:chatlist:data`:
  `FirestoreChatRemoteDataSource` (запрос без `orderBy` — сортировка в Room, чтобы не заводить составной индекс), имя/фамилия/
  `@username` собеседника денормализованы в `participantInfo` на самом документе чата (список никогда не читает чужой
  `users/{uid}`). `DefaultChatRepository.chats` — `StateFlow` (не просто `Flow`): `sync()` в вьюмодели проверяет
  `repository.chats.value` синхронно, а не своё же `state.chats` — иначе гонка с фоновым слушателем показывает ошибку
  поверх уже пришедших данных (нашёл и тем же способом починил аналогичный баг и в `ProfileViewModel`, этап 2). Запрос
  `fetchChats()` идёт с `Source.SERVER`: без этого офлайн-провал Firestore молча откатывается на пустой memory-кэш и
  выглядит как «чатов нет», а не как ошибка. `firestore.rules`: правила для `chats/{chatId}` опубликованы владельцем. До публикации
  любое обращение к `chats` возвращало PERMISSION_DENIED (проверено на эмуляторе). Тап по чату ведёт в
  `ChatRoute(chatId)`, но создавать чаты пока негде — это этап 4 (первое сообщение создаёт чат).

## Тулчейн (выбран из-за требований свежих AndroidX-библиотек)
AGP 8.13.2, Gradle 8.14.5, Kotlin 2.3.0, JDK 17, compileSdk 36 / targetSdk 35 / minSdk 26, Compose Multiplatform 1.9.3
(Material 3 стабилен только в линии 1.9.x; в 1.10+ он alpha), material3 1.9.0, material-icons-extended 1.7.3,
lifecycle 2.9.6, navigation 2.9.2, Koin 4.2.2, coroutines 1.10.2, serialization 1.9.0, Firebase BOM 34.19.0,
Room 3.0.2 (`androidx.room3`), KSP 2.3.10, `androidx.sqlite:sqlite-bundled` 2.7.1.
Convention plugins в `build-logic` — обычные Kotlin-классы (не `kotlin-dsl`): встроенный Kotlin Gradle 8.x не читает метаданные
Kotlin 2.3. Версии SDK и библиотек — только в `gradle/libs.versions.toml`. Нужна Android Studio 2025.1.3+ (у владельца Quail 4).

Сборка и проверка:
```bash
./gradlew assembleDebug testDebugUnitTest
```
На Windows с кириллицей в имени профиля тестовый воркер Gradle не стартует — нужен ASCII-путь для Gradle user home
(переменная `GRADLE_USER_HOME` или поле «Gradle user home» в настройках IDE).

## План этапов
1. ✅ Каркас.
2. ✅ SMS-вход через Firebase Auth (Phone) + сохранение сессии + профиль.
3. ✅ Список чатов: Room + Flow, `:core:database`, пустые/ошибочные состояния, Security Rules для чатов.
4. **Личные сообщения (текст)** (следующий): оптимистичная отправка, статусы, ретраи (WorkManager).
5. Курсорная синхронизация, Paging 3 + RemoteMediator; 5б — пуши через мини-сервер (без Cloud Functions).
6. Группы, `last_read_message_id`, «печатает…».
7. Полировка: SQLCipher/Keystore, локализация RU/EN, ktlint/detekt, a11y, R8 для release, `POST_NOTIFICATIONS`, SavedStateHandle там, где нужно.
8. Медиа (последним; хранилище выбираем к тому времени — Firebase Storage требует Blaze).

### Этап 2: задел и открытые вопросы
- `PhoneAuthenticator` (интерфейс в `feature:auth:domain`, реализация Firebase в `feature:auth:data/androidMain`; для
  `verifyPhoneNumber` нужен Activity), экран ввода SMS-кода, повторная отправка с таймером, ошибки через `UiText`.
- Сессия: `isLoggedIn` из Firebase Auth (сессию он хранит сам) → реальный старт `NavigationRoot`; «Выйти» в профиле/настройках →
  `signOut` и возврат на экран входа с очисткой back stack. Экран входа показывается только вне сессии.
- Профиль пользователя (имя/фамилия) в Firestore `users/{uid}`; первые Security Rules (сейчас открыты любому авторизованному!).
- Решить в начале этапа: показывать ли «+7» постоянно (сейчас Material 3 показывает префикс только при фокусе поля);
  ввод только по SMS или ещё и имя на отдельном шаге; поведение при ошибках/лимитах SMS.
- Для входа нужны SHA-1/SHA-256 debug-ключа в Firebase (добавлены), тестовые номера Firebase для разработки.

## Известные хвосты
- **Этап 3:** правила для `chats` опубликованы; без них (при смене `firestore.rules` их нужно публиковать заново) список чатов
  получает PERMISSION_DENIED. Заполненное состояние списка (реальные чаты) на устройстве
  не проверялось — создавать чат пока негде (это этап 4); можно добавить документ в `chats/` вручную в консоли (форму
  смотреть в `firestore.rules`) для визуальной проверки, но это не обязательно. Занятое @username (нужен второй
  пользователь) и отказ правил на чужих документах — тоже не проверялись на реальном Firestore, только юнит-тестами.
  `MaroDatabase`/`ChatDao` в `:core:database` не собирались и не запускались на iOS (нет Mac) — код написан по
  официальному образцу Room KMP, но не проверен.
- Этап 2: для SMS на +7 в консоли Firebase (Authentication → Settings → SMS region policy) должна быть разрешена Россия, иначе ошибка 17006 «SMS unable to be sent until this region enabled». Если после верного SMS-кода не удалось сохранить профиль и приложение убито, при следующем запуске пользователь уже «залогинен» без `users/{uid}` (профиль нигде пока не читается). Автоподстановка SMS, пришедшего после `onCodeSent`, игнорируется — код вводится вручную.
- `com.android.library` + Kotlin Multiplatform помечен устаревшим (несовместим с AGP 9) — позже перейти на
  `com.android.kotlin.multiplatform.library`.
- ktlint/detekt не настроены (detekt может не поддерживать Kotlin 2.3); `lintDebug` для KMP-модулей не гонялся.
- Ограничение Firebase API-ключа в Google Cloud Console не настроено (необязательно; ключ не секрет, защиту дают Security Rules).
- Отсутствуют: iOS-приложение, Ktor, WorkManager, DataStore.
