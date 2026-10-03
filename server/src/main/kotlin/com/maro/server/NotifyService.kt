package com.maro.server

/** A device of a user that can receive pushes: `users/{uid}/devices/{id}` in Firestore. */
data class Device(val id: String, val token: String)

/** What the service needs from the database; Firestore through the Admin SDK in production. */
interface ChatStore {
    /** Participant uids of the chat, or `null` when there is no such chat. */
    suspend fun participants(chatId: String): List<String>?

    /** Uid of the message's sender, or `null` when there is no such message. */
    suspend fun messageSender(chatId: String, messageId: String): String?

    suspend fun devices(uid: String): List<Device>

    suspend fun removeDevice(uid: String, deviceId: String)
}

enum class PushOutcome {
    DELIVERED,

    /** The token is dead (app uninstalled, token rotated): the device should be forgotten. */
    TOKEN_GONE,

    /** Anything else; the device is kept. */
    FAILED,
}

/** Sends one data message to several devices; FCM in production. The outcomes line up with [tokens]. */
interface PushSender {
    suspend fun send(tokens: List<String>, data: Map<String, String>): List<PushOutcome>
}

sealed interface NotifyResult {
    data class Sent(val devices: Int) : NotifyResult
    data object ChatNotFound : NotifyResult
    data object MessageNotFound : NotifyResult

    /** The caller is not a participant of the chat or not the author of the message. */
    data object Forbidden : NotifyResult
}

/**
 * Wakes up the other participants of a chat after a message was sent. The push carries ids only, never the text:
 * the app fetches the message itself, so message content never goes through Google's push service.
 */
class NotifyService(
    private val store: ChatStore,
    private val sender: PushSender,
) {
    suspend fun notify(callerUid: String, chatId: String, messageId: String): NotifyResult {
        val participants = store.participants(chatId) ?: return NotifyResult.ChatNotFound
        if (callerUid !in participants) return NotifyResult.Forbidden
        val author = store.messageSender(chatId, messageId) ?: return NotifyResult.MessageNotFound
        // Only the author may announce a message: nobody can make the server spam a chat with someone else's messages.
        if (author != callerUid) return NotifyResult.Forbidden

        val data = mapOf("type" to TYPE_MESSAGE, "chatId" to chatId, "messageId" to messageId)
        var delivered = 0
        for (recipient in participants.filter { it != callerUid }.distinct()) {
            val devices = store.devices(recipient)
            if (devices.isEmpty()) continue
            val outcomes = sender.send(devices.map { it.token }, data)
            devices.zip(outcomes).forEach { (device, outcome) ->
                when (outcome) {
                    PushOutcome.DELIVERED -> delivered++
                    PushOutcome.TOKEN_GONE -> store.removeDevice(recipient, device.id)
                    PushOutcome.FAILED -> Unit
                }
            }
        }
        return NotifyResult.Sent(delivered)
    }

    companion object {
        const val TYPE_MESSAGE = "message"

        /** Chat ids (`uid1_uid2`) and message ids (UUID) only: nothing that could reach another Firestore path. */
        private val ID = Regex("^[A-Za-z0-9_-]{1,128}$")

        fun isValidId(id: String): Boolean = ID.matches(id)
    }
}
