# Сервер пушей Maro

*English version: [README.md](README.md).*

Небольшой сервер на Ktor, который рассылает пуш-уведомления: на тарифе Firebase Spark нет Cloud Functions. Приложение
вызывает его после того, как сообщение попало в Firestore; сервер проверяет отправителя и шлёт устройствам остальных
участников **data-only** FCM-сообщение (`type`, `chatId`, `messageId` — текст никогда).

Это отдельная Gradle-сборка (с общим каталогом версий приложения), поэтому Android Studio её не синхронизирует.

## Эндпоинты

- `GET /health` → `ok`
- `POST /v1/notify` с `Authorization: Bearer <Firebase ID-токен>` и `{"chatId": "...", "messageId": "..."}`
  - `200 {"devices": n}` — отправлено на `n` устройств; `401` — плохой токен; `400` — плохое тело запроса;
    `403` — вызывающий не участник чата или не автор сообщения; `404` — нет такого чата или сообщения.

## Локальный запуск

1. Получите ключ сервисного аккаунта: Firebase Console → Project settings → Service accounts → Generate new private key.
   Храните JSON-файл **вне репозитория** и никогда не коммитьте: он даёт полный админский доступ к проекту.
2. Из корня репозитория:

   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
   ./gradlew -p server run
   ```

   Сервер слушает порт 8080 (переопределяется `PORT`). Debug-сборка приложения видит его из эмулятора по адресу
   `http://10.0.2.2:8080` (`PUSH_SERVER_URL` в `app/build.gradle.kts`).

Тесты: `./gradlew -p server test`.

## Ключ

Ключа сервисного аккаунта нет ни в репозитории, ни в Docker-образе — сервер берёт его из окружения:

- `FIREBASE_CREDENTIALS_JSON` — **содержимое** файла ключа. Для хостингов, где секреты задаются только переменными.
- иначе Application Default Credentials: `GOOGLE_APPLICATION_CREDENTIALS` = путь к файлу ключа, или собственный
  сервисный аккаунт хостинга (Google Cloud Run — файл ключа не нужен вовсе).

В логе пишется, какой способ использован (`credentials: ...`), но никогда не сам ключ.

## Развёртывание

### Docker-образ

Из корня репозитория (сервер использует общий каталог версий и Gradle wrapper, поэтому контекст сборки — корень;
`.dockerignore` пропускает только `gradlew`, `gradle/` и `server/` и никогда — `*.json`):

```bash
docker build -f server/Dockerfile -t maro-push .
docker run --rm -p 8080:8080 -e FIREBASE_CREDENTIALS_JSON="$(cat /path/to/service-account.json)" maro-push
```

или с ключом в виде файла только для чтения:

```bash
docker run --rm -p 8080:8080 -v /path/to/service-account.json:/secrets/key.json:ro -e GOOGLE_APPLICATION_CREDENTIALS=/secrets/key.json maro-push
```

Образ работает не от root и слушает `PORT` (по умолчанию 8080); куча JVM подстраивается под лимит памяти контейнера
(`JAVA_OPTS=-XX:MaxRAMPercentage=75`), хватает 256–512 МБ.

### Что нужно от любого хостинга

- **HTTPS** с действительным сертификатом: release-сборка приложения обращается к серверу только по `https://`.
- Ключ: `FIREBASE_CREDENTIALS_JSON` как секретная переменная или секретный файл + `GOOGLE_APPLICATION_CREDENTIALS`.
- `PORT`, если хостинг задаёт свой (Render и Cloud Run задают; сервер его подхватывает).
- Проверка здоровья: `GET /health` → `200 ok`.
- Правильные часы машины: по ним проверяются Firebase ID-токены (при расхождении в часы каждый запрос отклоняется,
  в логе `ID token rejected: INVALID_ID_TOKEN`). На хостингах время синхронизировано.

### Варианты

- **Render (бесплатный Web Service).** New → Web Service → этот репозиторий, runtime Docker, Dockerfile path
  `server/Dockerfile`, Docker build context `.`; Environment → секрет `FIREBASE_CREDENTIALS_JSON`; health check path
  `/health`. HTTPS уже включён. Бесплатный тариф засыпает примерно через 15 минут без запросов: первый пуш после
  этого придёт с задержкой до минуты (само сообщение — без задержки, оно приходит через синхронизацию).
- **VPS (≈4–5 €/мес).** Установить Docker, собрать или скопировать образ, запустить с `--restart unless-stopped` на
  `127.0.0.1:8080`, а перед ним поставить [Caddy](https://caddyserver.com) для HTTPS (`your.domain { reverse_proxy 127.0.0.1:8080 }` —
  сертификат он получит сам). Нужно доменное имя, указывающее на сервер.
- **Google Cloud Run.** Собрать образ (Cloud Build или `docker push` в Artifact Registry) и развернуть в проекте
  Firebase от имени сервисного аккаунта Admin SDK (`firebase-adminsdk-…@<project>.iam.gserviceaccount.com`) — тогда
  файл ключа не нужен вовсе. Есть бесплатный лимит, но нужен платёжный аккаунт (карта).

### Как указать адрес приложению

Добавьте адрес в `local.properties` (не попадает в git — репозиторий публичный):

```properties
maro.pushServerUrl=https://your-push-server.example.com
```

Release-сборка берёт его как `BuildConfig.PUSH_SERVER_URL` (сборка падает, если адрес не `https://`). Без него
в release-сборке нет пушей; сообщения всё равно приходят через синхронизацию. Debug-сборка всегда использует
локальный сервер.

## Что сервер читает и пишет

Admin SDK обходит Security Rules, поэтому сервер сам всё проверяет перед действием. Он читает `chats/{chatId}`
(участники), `chats/{chatId}/messages/{messageId}` (отправитель) и `users/{uid}/devices/*` (FCM-токены) и удаляет
устройство, токен которого FCM считает больше не зарегистрированным. В лог пишутся только id.
