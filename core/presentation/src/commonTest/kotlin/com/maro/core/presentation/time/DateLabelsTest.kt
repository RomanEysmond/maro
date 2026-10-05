package com.maro.core.presentation.time

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone

class DateLabelsTest {

    // A Thursday.
    private val today = LocalDate(2026, 10, 1)

    @Test
    fun `day headers say today, yesterday, then the date with the year only for other years`() {
        assertThat(dayLabel(today, today)).isEqualTo(DayLabel.Today)
        assertThat(dayLabel(LocalDate(2026, 9, 30), today)).isEqualTo(DayLabel.Yesterday)
        assertThat(dayLabel(LocalDate(2026, 9, 12), today)).isEqualTo(DayLabel.Date(12, 9, null))
        assertThat(dayLabel(LocalDate(2025, 12, 31), today)).isEqualTo(DayLabel.Date(31, 12, 2025))
    }

    @Test
    fun `the chat list shows the clock today, the weekday this week and the date before that`() {
        assertThat(shortTimeLabel(LocalDateTime(2026, 10, 1, 9, 5), today)).isEqualTo(ShortTimeLabel.Time(9, 5))
        assertThat(shortTimeLabel(LocalDateTime(2026, 9, 28, 23, 0), today))
            .isEqualTo(ShortTimeLabel.Weekday(DayOfWeek.MONDAY))
        // Exactly a week ago is no longer "this week".
        assertThat(shortTimeLabel(LocalDateTime(2026, 9, 24, 12, 0), today)).isEqualTo(ShortTimeLabel.Date(24, 9, null))
        assertThat(shortTimeLabel(LocalDateTime(2025, 3, 2, 12, 0), today)).isEqualTo(ShortTimeLabel.Date(2, 3, 2025))
    }

    @Test
    fun `a message time is zero padded and follows the time zone`() {
        val epochMillis = 1_790_920_800_000L // 2026-10-02T06:00:00Z

        assertThat(formatClock(epochMillis.toLocalDateTime(TimeZone.UTC))).isEqualTo("06:00")
        assertThat(formatClock(epochMillis.toLocalDateTime(TimeZone.of("Europe/Moscow")))).isEqualTo("09:00")
    }
}
