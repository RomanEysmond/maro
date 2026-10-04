package com.maro.core.presentation.time

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.maro.core.presentation.generated.resources.Res
import com.maro.core.presentation.generated.resources.day_today
import com.maro.core.presentation.generated.resources.day_yesterday
import com.maro.core.presentation.generated.resources.month_genitive_1
import com.maro.core.presentation.generated.resources.month_genitive_10
import com.maro.core.presentation.generated.resources.month_genitive_11
import com.maro.core.presentation.generated.resources.month_genitive_12
import com.maro.core.presentation.generated.resources.month_genitive_2
import com.maro.core.presentation.generated.resources.month_genitive_3
import com.maro.core.presentation.generated.resources.month_genitive_4
import com.maro.core.presentation.generated.resources.month_genitive_5
import com.maro.core.presentation.generated.resources.month_genitive_6
import com.maro.core.presentation.generated.resources.month_genitive_7
import com.maro.core.presentation.generated.resources.month_genitive_8
import com.maro.core.presentation.generated.resources.month_genitive_9
import com.maro.core.presentation.generated.resources.weekday_short_1
import com.maro.core.presentation.generated.resources.weekday_short_2
import com.maro.core.presentation.generated.resources.weekday_short_3
import com.maro.core.presentation.generated.resources.weekday_short_4
import com.maro.core.presentation.generated.resources.weekday_short_5
import com.maro.core.presentation.generated.resources.weekday_short_6
import com.maro.core.presentation.generated.resources.weekday_short_7
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val monthsGenitive = listOf(
    Res.string.month_genitive_1, Res.string.month_genitive_2, Res.string.month_genitive_3,
    Res.string.month_genitive_4, Res.string.month_genitive_5, Res.string.month_genitive_6,
    Res.string.month_genitive_7, Res.string.month_genitive_8, Res.string.month_genitive_9,
    Res.string.month_genitive_10, Res.string.month_genitive_11, Res.string.month_genitive_12,
)

// Monday first, as DayOfWeek.ordinal.
private val weekdaysShort = listOf(
    Res.string.weekday_short_1, Res.string.weekday_short_2, Res.string.weekday_short_3, Res.string.weekday_short_4,
    Res.string.weekday_short_5, Res.string.weekday_short_6, Res.string.weekday_short_7,
)

@Composable
private fun today(timeZone: TimeZone): LocalDate = remember(timeZone) { Clock.System.todayIn(timeZone) }

/** "Сегодня", "Вчера", "12 сентября", "12 сентября 2025". */
@Composable
fun dayHeaderText(date: LocalDate, timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
    when (val label = dayLabel(date, today(timeZone))) {
        DayLabel.Today -> stringResource(Res.string.day_today)
        DayLabel.Yesterday -> stringResource(Res.string.day_yesterday)
        is DayLabel.Date -> dayMonth(label.day, label.month) + (label.year?.let { " $it" } ?: "")
    }

/** "09:05" today, "пн" this week, "12.09" this year, "12.09.25" before. */
@Composable
fun chatListTimeText(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val at = remember(epochMillis, timeZone) { epochMillis.toLocalDateTime(timeZone) }
    return when (val label = shortTimeLabel(at, today(timeZone))) {
        is ShortTimeLabel.Time -> "${label.hour.twoDigits()}:${label.minute.twoDigits()}"
        is ShortTimeLabel.Weekday -> stringResource(weekdaysShort[label.dayOfWeek.ordinal])
        is ShortTimeLabel.Date ->
            "${label.day.twoDigits()}.${label.month.twoDigits()}" + (label.year?.let { ".${(it % 100).twoDigits()}" } ?: "")
    }
}

/** "09:05" — the time of a single message. */
@Composable
fun messageTimeText(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
    remember(epochMillis, timeZone) { formatClock(epochMillis.toLocalDateTime(timeZone)) }

@Composable
private fun dayMonth(day: Int, month: Int): String = "$day ${stringResource(monthResource(month))}"

private fun monthResource(month: Int): StringResource = monthsGenitive[month - 1]
