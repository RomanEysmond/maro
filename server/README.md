# Maro push server

A small Ktor server that sends the push notifications, because Cloud Functions are not available on the Firebase
Spark plan. The app calls it after a message reached Firestore; the server checks the sender and sends a
**data-only** FCM message (`type`, `chatId`, `messageId` — never the text) to the other participant's devices.

It is a separate Gradle build (it shares the app's version catalog), so Android Studio's sync does not load it.

## Endpoints

- `GET /health` → `ok`
- `POST /v1/notify` with `Authorization: Bearer <Firebase ID token>` and `{"chatId": "...", "messageId": "..."}`
  - `200 {"devices": n}` — pushed to `n` devices; `401` bad token; `400` bad body;
    `403` the caller is not a participant or not the author; `404` no such chat or message.

## Running locally

1. Get a service account key: Firebase Console → Project settings → Service accounts → Generate new private key.
   Keep the JSON file **outside the repository** and never commit it: it gives full admin access to the project.
2. From the repository root:

   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
   ./gradlew -p server run
   ```

   The server listens on port 8080 (`PORT` overrides it). The debug build of the app reaches it from the emulator
   at `http://10.0.2.2:8080` (`PUSH_SERVER_URL` in `app/build.gradle.kts`); the release build has no server yet.

Tests: `./gradlew -p server test`.

## What it reads and writes

The Admin SDK bypasses Security Rules, so the server checks everything itself before acting. It reads
`chats/{chatId}` (participants), `chats/{chatId}/messages/{messageId}` (sender) and `users/{uid}/devices/*`
(FCM tokens), and deletes a device whose token FCM reports as no longer registered. It logs ids only.
