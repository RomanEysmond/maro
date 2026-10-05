package com.maro.server

import com.google.api.core.ApiFuture
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.cloud.FirestoreClient
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

// Ids and error codes only: never message text or push tokens.
private val log = LoggerFactory.getLogger("com.maro.server.firebase")

/** Checks a Firebase ID token and returns the uid it was issued to, or `null` when it is not valid. */
fun interface TokenVerifier {
    suspend fun verify(idToken: String): String?
}

/** Where the service account comes from: never from the image or the repository, only from the environment. */
sealed interface CredentialsSource {
    /** The key file's content in [CREDENTIALS_JSON_ENV]: for hostings that take secrets as variables, not files. */
    data class Json(val json: String) : CredentialsSource

    /** Application Default Credentials: GOOGLE_APPLICATION_CREDENTIALS (a key file) or the hosting's own account. */
    data object ApplicationDefault : CredentialsSource

    companion object {
        const val CREDENTIALS_JSON_ENV = "FIREBASE_CREDENTIALS_JSON"

        fun from(env: (String) -> String?): CredentialsSource =
            env(CREDENTIALS_JSON_ENV)?.takeIf { it.isNotBlank() }?.let(::Json) ?: ApplicationDefault
    }
}

/**
 * The Admin SDK bypasses Security Rules, so everything it reads or writes is checked by [NotifyService] first.
 * Credentials: see [CredentialsSource]; the key never goes into git or the Docker image.
 */
class FirebaseBackend private constructor(app: FirebaseApp) {
    val tokenVerifier: TokenVerifier = FirebaseTokenVerifier(FirebaseAuth.getInstance(app))
    val chatStore: ChatStore = FirestoreChatStore(FirestoreClient.getFirestore(app))
    val pushSender: PushSender = FcmPushSender(FirebaseMessaging.getInstance(app))

    companion object {
        fun create(source: CredentialsSource = CredentialsSource.from(System::getenv)): FirebaseBackend {
            val credentials = when (source) {
                is CredentialsSource.Json -> GoogleCredentials.fromStream(source.json.byteInputStream())
                CredentialsSource.ApplicationDefault -> GoogleCredentials.getApplicationDefault()
            }
            // Which source, never the key itself.
            log.info("credentials: {}", if (source is CredentialsSource.Json) CredentialsSource.CREDENTIALS_JSON_ENV else "default")
            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build()
            return FirebaseBackend(FirebaseApp.initializeApp(options))
        }
    }
}

/** The Admin SDK is blocking; its futures are awaited on the IO dispatcher. */
private suspend fun <T> ApiFuture<T>.await(): T = withContext(Dispatchers.IO) { get() }

private class FirebaseTokenVerifier(private val auth: FirebaseAuth) : TokenVerifier {
    override suspend fun verify(idToken: String): String? = try {
        withContext(Dispatchers.IO) { auth.verifyIdToken(idToken).uid }
    } catch (e: FirebaseAuthException) {
        log.warn("ID token rejected: {} {}", e.authErrorCode, e.message)
        null
    } catch (e: IllegalArgumentException) {
        log.warn("ID token malformed")
        null
    }
}

private class FirestoreChatStore(private val firestore: Firestore) : ChatStore {
    override suspend fun participants(chatId: String): List<String>? {
        val chat = firestore.collection("chats").document(chatId).get().await()
        if (!chat.exists()) return null
        return (chat.get("participants") as? List<*>)?.filterIsInstance<String>()
    }

    override suspend fun messageSender(chatId: String, messageId: String): String? {
        val message = firestore.collection("chats").document(chatId)
            .collection("messages").document(messageId).get().await()
        return if (message.exists()) message.getString("senderId") else null
    }

    override suspend fun devices(uid: String): List<Device> {
        val devices = firestore.collection("users").document(uid).collection("devices").get().await()
            .documents.mapNotNull { doc -> doc.getString("token")?.let { Device(doc.id, it) } }
        log.info("devices uid={} count={}", uid, devices.size)
        return devices
    }

    override suspend fun removeDevice(uid: String, deviceId: String) {
        firestore.collection("users").document(uid).collection("devices").document(deviceId).delete().await()
    }
}

private class FcmPushSender(private val messaging: FirebaseMessaging) : PushSender {
    override suspend fun send(tokens: List<String>, data: Map<String, String>): List<PushOutcome> {
        val messages = tokens.map { token ->
            Message.builder()
                .setToken(token)
                .putAllData(data)
                // High priority: a data message has to wake the app up (Doze included) to fetch and show the message.
                .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
                .build()
        }
        val response = withContext(Dispatchers.IO) { messaging.sendEach(messages) }
        return response.responses.map { result ->
            if (!result.isSuccessful) log.warn("fcm failed code={}", result.exception?.messagingErrorCode)
            when {
                result.isSuccessful -> PushOutcome.DELIVERED
                result.exception?.messagingErrorCode in GONE -> PushOutcome.TOKEN_GONE
                else -> PushOutcome.FAILED
            }
        }
    }

    private companion object {
        val GONE = setOf(MessagingErrorCode.UNREGISTERED, MessagingErrorCode.SENDER_ID_MISMATCH)
    }
}
