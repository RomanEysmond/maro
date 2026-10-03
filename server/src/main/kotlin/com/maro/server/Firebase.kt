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

/**
 * The Admin SDK bypasses Security Rules, so everything it reads or writes is checked by [NotifyService] first.
 * Credentials come from GOOGLE_APPLICATION_CREDENTIALS (a service account key file that never goes into git).
 */
class FirebaseBackend private constructor(app: FirebaseApp) {
    val tokenVerifier: TokenVerifier = FirebaseTokenVerifier(FirebaseAuth.getInstance(app))
    val chatStore: ChatStore = FirestoreChatStore(FirestoreClient.getFirestore(app))
    val pushSender: PushSender = FcmPushSender(FirebaseMessaging.getInstance(app))

    companion object {
        fun create(): FirebaseBackend {
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
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
        null
    } catch (e: IllegalArgumentException) {
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
