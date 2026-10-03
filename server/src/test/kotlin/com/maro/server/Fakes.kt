package com.maro.server

class FakeChatStore : ChatStore {
    val chats = mutableMapOf<String, List<String>>()

    /** (chatId, messageId) -> sender uid. */
    val messages = mutableMapOf<Pair<String, String>, String>()
    val devices = mutableMapOf<String, MutableList<Device>>()

    override suspend fun participants(chatId: String): List<String>? = chats[chatId]

    override suspend fun messageSender(chatId: String, messageId: String): String? = messages[chatId to messageId]

    override suspend fun devices(uid: String): List<Device> = devices[uid].orEmpty()

    override suspend fun removeDevice(uid: String, deviceId: String) {
        devices[uid]?.removeAll { it.id == deviceId }
    }
}

class FakePushSender : PushSender {
    /** Outcome per token; DELIVERED when not listed. */
    val outcomes = mutableMapOf<String, PushOutcome>()
    val sent = mutableListOf<Pair<String, Map<String, String>>>()

    override suspend fun send(tokens: List<String>, data: Map<String, String>): List<PushOutcome> {
        tokens.forEach { sent += it to data }
        return tokens.map { outcomes[it] ?: PushOutcome.DELIVERED }
    }
}
