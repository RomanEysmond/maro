package com.maro.feature.chat.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.maro.core.database.chat.ChatEntity
import com.maro.feature.chat.domain.ChatMember
import com.maro.feature.chat.domain.SystemEvent
import com.maro.feature.chat.domain.SystemEventKind
import kotlin.test.Test

class GroupMappersTest {

    @Test
    fun `a system message survives the trip through Room`() {
        val remote = serverMessage("s1", second = 1, senderId = "anna").copy(
            text = "",
            systemEvent = RemoteSystemEvent(kind = "added", targets = listOf("petr", "olga")),
        )

        val message = remote.toEntity().toDomain(currentUserId = "me")

        assertThat(message.systemEvent).isEqualTo(SystemEvent(SystemEventKind.ADDED, listOf("petr", "olga")))
    }

    @Test
    fun `an ordinary message has no system event and an unknown kind is dropped`() {
        assertThat(serverMessage("m1", second = 1).toEntity().toDomain("me").systemEvent).isNull()

        val unknown = serverMessage("s2", second = 1).copy(systemEvent = RemoteSystemEvent(kind = "exploded"))
        assertThat(unknown.toEntity().toDomain("me").systemEvent).isNull()
    }

    private fun chat(type: String, createdBy: String? = null) = ChatEntity(
        id = "c", type = type, otherUserId = if (type == "direct") "anna" else "", otherUserFirstName = "Анна",
        otherUserLastName = "Петрова", otherUserUsername = null, lastMessageText = null, lastMessageSenderId = null,
        lastMessageAt = null, updatedAt = 0L,
        title = if (type ==
            "group"
        ) {
            "Поход в горы"
        } else {
            null
        },
        createdBy = createdBy,
    )

    private val members = listOf(ChatMember("me", "Иван", "", null), ChatMember("anna", "Анна", "", null))

    @Test
    fun `a group's header has its title, members and who may manage it`() {
        val mine = chat("group", createdBy = "me").toHeader(members, currentUserId = "me")
        assertThat(mine.title).isEqualTo("Поход в горы")
        assertThat(mine.initials).isEqualTo("ПВ")
        assertThat(mine.isGroup).isTrue()
        assertThat(mine.memberCount).isEqualTo(2)
        assertThat(mine.canManage).isTrue()

        assertThat(chat("group", createdBy = "anna").toHeader(members, currentUserId = "me").canManage).isFalse()
    }

    @Test
    fun `a direct chat's header is the other person`() {
        val header = chat("direct").toHeader(members, currentUserId = "me")

        assertThat(header.title).isEqualTo("Анна Петрова")
        assertThat(header.initials).isEqualTo("АП")
        assertThat(header.isGroup).isFalse()
        assertThat(header.canManage).isFalse()
    }
}
