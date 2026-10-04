package com.maro.feature.chat.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.maro.feature.chat.domain.Message
import com.maro.feature.chat.domain.MessageStatus
import kotlin.test.Test
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class DaySeparatorTest {

    private fun message(id: String, epochMillis: Long) =
        Message(id, "chat", "them", id, epochMillis, MessageStatus.SENT, isOutgoing = false)

    // 2026-10-01T10:00Z, 2026-10-01T22:00Z and 2026-10-02T09:00Z.
    private val morning = message("a", 1_790_848_800_000L)
    private val evening = message("b", 1_790_892_000_000L)
    private val nextDay = message("c", 1_790_931_600_000L)

    @Test
    fun `no heading between two messages of the same day`() {
        assertThat(daySeparatorBetween(newer = evening, older = morning, TimeZone.UTC)).isNull()
    }

    @Test
    fun `a heading for the newer day where the day changes`() {
        assertThat(daySeparatorBetween(newer = nextDay, older = evening, TimeZone.UTC))
            .isEqualTo(ChatItem.DaySeparator(LocalDate(2026, 10, 2)))
    }

    @Test
    fun `the oldest message of the chat gets its heading, nothing goes below the newest`() {
        assertThat(daySeparatorBetween(newer = morning, older = null, TimeZone.UTC))
            .isEqualTo(ChatItem.DaySeparator(LocalDate(2026, 10, 1)))
        assertThat(daySeparatorBetween(newer = null, older = nextDay, TimeZone.UTC)).isNull()
    }

    @Test
    fun `days follow the device time zone`() {
        // 22:00Z on Oct 1 is already 01:00 on Oct 2 in Moscow, the same day as 09:00Z on Oct 2.
        assertThat(daySeparatorBetween(newer = nextDay, older = evening, TimeZone.of("Europe/Moscow"))).isNull()
    }
}
