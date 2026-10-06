# Maro push server

*Русская версия: [README.ru.md](README.ru.md).*

A small Ktor server that sends the push notifications, because Cloud Functions are not available on the Firebase
Spark plan. The app calls it after a message reached Firestore; the server checks the sender and sends a
**data-only** FCM message (`type`, `chatId`, `messageId` — never the text) to the other participant's devices.

It is a separate Gradle build (it shares the app's version catalog), so Android Studio's sync does not load it.

## Endpoints

- `GET /health` → `ok`
- `POST /v1/notify` with `Authorization: Bearer <Firebase ID token>` and `{"chatId": "...", "messageId": "..."}`
  - `200 {"devices": n}` — pushed to `n` devices; `401` bad token; `400` bad body;
    `403` the caller is not a participant or not the author; `404` no such chat or message.

- `POST /v1/media/upload` with the ID token and `{"chatId": "...", "messageId": "..."}`
  - `200 {"key": "chats/{chatId}/{messageId}", "url": "..."}` — PUT the file to `url` within 15 minutes;
    `403` not a participant; `404` no such chat; `503` media storage not configured.
- `POST /v1/media/download` with the ID token and `{"chatId": "...", "key": "chats/{chatId}/..."}`
  - `200 {"key": ..., "url": ...}` — GET the file from `url` within 15 minutes; `403` not a participant or a key of
    another chat.

## Running locally

1. Get a service account key: Firebase Console → Project settings → Service accounts → Generate new private key.
   Keep the JSON file **outside the repository** and never commit it: it gives full admin access to the project.
2. From the repository root:

   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
   ./gradlew -p server run
   ```

   The server listens on port 8080 (`PORT` overrides it). The debug build of the app reaches it from the emulator
   at `http://10.0.2.2:8080` (`PUSH_SERVER_URL` in `app/build.gradle.kts`).

Tests: `./gradlew -p server test`.

## Credentials

The service account key is never in the repository or in the Docker image; the server takes it from the environment:

- `FIREBASE_CREDENTIALS_JSON` — the **content** of the key file. For hostings that take secrets as variables only.
- otherwise Application Default Credentials: `GOOGLE_APPLICATION_CREDENTIALS` = path to the key file, or the hosting's
  own service account (Google Cloud Run — no key file at all).

The log says which one was used (`credentials: ...`), never the key itself.

## Media storage (photos)

Photos live in any S3-compatible object storage; the server only signs short-lived URLs, the files never pass
through it. Without these variables the server still sends pushes and answers media requests with `503`.

| Variable | Example |
|---|---|
| `S3_ENDPOINT` | `https://s3.eu-central-003.backblazeb2.com` |
| `S3_REGION` | `eu-central-003` |
| `S3_BUCKET` | `maro-media` |
| `S3_ACCESS_KEY_ID` | the key's id |
| `S3_SECRET_ACCESS_KEY` | the key itself (a secret: never in git, never in the image) |

Backblaze B2 (the current choice, 10 GB free, no card): create a **private** bucket; *Application Keys → Add a New
Application Key* with access to that bucket only (read and write); the endpoint and region are shown on the bucket's
page (`s3.<region>.backblazeb2.com`). Moving to Cloudflare R2, a Russian provider or AWS later means changing these
variables, nothing else.

## Deploy

### Docker image

From the repository root (the server shares the app's version catalog and Gradle wrapper, so the build context is the
root; `.dockerignore` lets in only `gradlew`, `gradle/` and `server/`, and never `*.json`):

```bash
docker build -f server/Dockerfile -t maro-push .
docker run --rm -p 8080:8080 -e FIREBASE_CREDENTIALS_JSON="$(cat /path/to/service-account.json)" maro-push
```

or with the key as a read-only file:

```bash
docker run --rm -p 8080:8080 -v /path/to/service-account.json:/secrets/key.json:ro -e GOOGLE_APPLICATION_CREDENTIALS=/secrets/key.json maro-push
```

The image runs as a non-root user and listens on `PORT` (8080 by default); the JVM heap follows the container's memory
limit (`JAVA_OPTS=-XX:MaxRAMPercentage=75`), 256-512 MB is enough.

### What any hosting needs

- **HTTPS** with a valid certificate: the release build of the app talks to the server only over `https://`.
- The key: `FIREBASE_CREDENTIALS_JSON` as a secret variable, or a secret file + `GOOGLE_APPLICATION_CREDENTIALS`.
- `PORT`, if the hosting sets its own (Render, Cloud Run do; the server picks it up).
- Health check: `GET /health` → `200 ok`.
- The machine's clock must be correct: Firebase ID tokens are checked against it (a skew of hours rejects every
  request with `ID token rejected: INVALID_ID_TOKEN` in the log). Hostings keep it in sync.

### Options

- **Render (free web service).** New → Web Service → this repository, runtime Docker, Dockerfile path
  `server/Dockerfile`, Docker build context `.`; Environment → secret `FIREBASE_CREDENTIALS_JSON`; health check path
  `/health`. HTTPS is included. The free plan sleeps after ~15 minutes without requests: the first push after that is
  delayed by up to a minute (the message itself is not, it arrives through sync).
- **VPS (≈4-5 €/month).** Install Docker, build or copy the image, run it with `--restart unless-stopped` on
  `127.0.0.1:8080`, and put [Caddy](https://caddyserver.com) in front for HTTPS (`your.domain { reverse_proxy 127.0.0.1:8080 }` —
  it gets the certificate by itself). Needs a domain name pointing at the server.
- **Google Cloud Run.** Build the image (Cloud Build or `docker push` to Artifact Registry), deploy it in the Firebase project
  running as the project's Admin SDK service account (`firebase-adminsdk-…@<project>.iam.gserviceaccount.com`) —
  then no key file is needed at all. Has a free tier, but
  the billing account (a card) is required.

### Pointing the app at it

Add the address to `local.properties` (not in git — the repository is public):

```properties
maro.pushServerUrl=https://your-push-server.example.com
```

The release build picks it up as `BuildConfig.PUSH_SERVER_URL` (the build fails if it is not `https://`). Without it
the release build has no pushes; messages still arrive through sync. The debug build always uses the local server.

## What it reads and writes

The Admin SDK bypasses Security Rules, so the server checks everything itself before acting. It reads
`chats/{chatId}` (participants), `chats/{chatId}/messages/{messageId}` (sender) and `users/{uid}/devices/*`
(FCM tokens), and deletes a device whose token FCM reports as no longer registered. It logs ids only.
