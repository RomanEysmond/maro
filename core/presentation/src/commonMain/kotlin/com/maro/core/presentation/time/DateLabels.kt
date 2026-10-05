package com.maro.core.presentation.time

import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/*
 * What a date or time should say, decided without string resources so it can be unit-tested; the composables in
 * DateTexts.kt turn these into text. All of it works in the device's time zone.
 */

fun Long.toLocalDateTime(timeZone: TimeZone): LocalDateTime =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone)

/** The heading above the messages of one day in a conversation. */
sealed interface DayLabel {
    data object Today : DayLabel
    data object Yesterday : DayLabel

    /** [year] is `null` within the current year. */
    data class Date(val day: Int, val month: Int, val year: Int?) : DayLabel
}

fun dayLabel(date: LocalDate, today: LocalDate): DayLabel = when (date) {
    today -> DayLabel.Today
    today.minus(DatePeriod(days = 1)) -> DayLabel.Yesterday
    else -> DayLabel.Date(date.day, date.month.ordinal + 1, date.year.takeIf { it != today.year })
}

/** The time next to a chat in the chat list: the clock today, the weekday this week, the date before that. */
sealed interface ShortTimeLabel {
    data class Time(val hour: Int, val minute: Int) : ShortTimeLabel
    data class Weekday(val dayOfWeek: DayOfWeek) : ShortTimeLabel

    /** [year] is `null` within the current year. */
    data class Date(val day: Int, val month: Int, val year: Int?) : ShortTimeLabel
}

fun shortTimeLabel(at: LocalDateTime, today: LocalDate): ShortTimeLabel {
    val date = at.date
    return when {
        date == today -> ShortTimeLabel.Time(at.hour, at.minute)
        date > today.minus(DatePeriod(days = WEEK_DAYS)) && date < today -> ShortTimeLabel.Weekday(date.dayOfWeek)
        else -> ShortTimeLabel.Date(date.day, date.month.ordinal + 1, date.year.takeIf { it != today.year })
    }
}

/** "09:05": the time of a single message. */
fun formatClock(at: LocalDateTime): String = "${at.hour.twoDigits()}:${at.minute.twoDigits()}"

internal fun Int.twoDigits(): String = toString().padStart(2, '0')

private const val WEEK_DAYS = 7
