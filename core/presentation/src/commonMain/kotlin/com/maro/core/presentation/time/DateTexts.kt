package com.maro.core.presentation.time

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.maro.core.presentation.generated.resources.Res
import com.maro.core.presentation.generated.resources.date_day_month
import com.maro.core.presentation.generated.resources.date_day_month_year
import com.maro.core.presentation.generated.resources.date_short
import com.maro.core.presentation.generated.resources.date_short_year
import com.maro.core.presentation.generated.resources.day_today
import com.maro.core.presentation.generated.resources.day_yesterday
import com.maro.core.presentation.generated.resources.month_1
import com.maro.core.presentation.generated.resources.month_10
import com.maro.core.presentation.generated.resources.month_11
import com.maro.core.presentation.generated.resources.month_12
import com.maro.core.presentation.generated.resources.month_2
import com.maro.core.presentation.generated.resources.month_3
import com.maro.core.presentation.generated.resources.month_4
import com.maro.core.presentation.generated.resources.month_5
import com.maro.core.presentation.generated.resources.month_6
import com.maro.core.presentation.generated.resources.month_7
import com.maro.core.presentation.generated.resources.month_8
import com.maro.core.presentation.generated.resources.month_9
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

// The month as it stands in a date: genitive in Russian ("12 сентября"), the plain name in English.
private val months = listOf(
    Res.string.month_1, Res.string.month_2, Res.string.month_3,
    Res.string.month_4, Res.string.month_5, Res.string.month_6,
    Res.string.month_7, Res.string.month_8, Res.string.month_9,
    Res.string.month_10, Res.string.month_11, Res.string.month_12,
)

// Monday first, as DayOfWeek.ordinal.
private val weekdaysShort = listOf(
    Res.string.weekday_short_1, Res.string.weekday_short_2, Res.string.weekday_short_3, Res.string.weekday_short_4,
    Res.string.weekday_short_5, Res.string.weekday_short_6, Res.string.weekday_short_7,
)

@Composable
private fun today(timeZone: TimeZone): LocalDate = remember(timeZone) { Clock.System.todayIn(timeZone) }

/** "Today", "Yesterday", "September 12", "September 12, 2025" (in Russian "12 сентября 2025"). */
@Composable
fun dayHeaderText(date: LocalDate, timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
    when (val label = dayLabel(date, today(timeZone))) {
        DayLabel.Today -> stringResource(Res.string.day_today)
        DayLabel.Yesterday -> stringResource(Res.string.day_yesterday)
        is DayLabel.Date -> {
            val month = stringResource(monthResource(label.month))
            label.year?.let { stringResource(Res.string.date_day_month_year, label.day, month, it) }
                ?: stringResource(Res.string.date_day_month, label.day, month)
        }
    }

/** "09:05" today, "Mon" this week, "09/12" this year, "09/12/25" before (in Russian "пн", "12.09", "12.09.25"). */
@Composable
fun chatListTimeText(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val at = remember(epochMillis, timeZone) { epochMillis.toLocalDateTime(timeZone) }
    return when (val label = shortTimeLabel(at, today(timeZone))) {
        is ShortTimeLabel.Time -> "${label.hour.twoDigits()}:${label.minute.twoDigits()}"
        is ShortTimeLabel.Weekday -> stringResource(weekdaysShort[label.dayOfWeek.ordinal])
        is ShortTimeLabel.Date -> {
            val day = label.day.twoDigits()
            val month = label.month.twoDigits()
            label.year?.let { stringResource(Res.string.date_short_year, day, month, (it % 100).twoDigits()) }
                ?: stringResource(Res.string.date_short, day, month)
        }
    }
}

/** "09:05" — the time of a single message. */
@Composable
fun messageTimeText(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
    remember(epochMillis, timeZone) { formatClock(epochMillis.toLocalDateTime(timeZone)) }

private fun monthResource(month: Int): StringResource = months[month - 1]
