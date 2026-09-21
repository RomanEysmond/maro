package com.maro.core.domain.profile

/** A calendar date without time zone. Kept as plain numbers to avoid a date-time dependency. */
data class BirthDate(val year: Int, val month: Int, val day: Int) {

    /** "yyyy-MM-dd": the storage format. */
    fun toIsoString(): String = "${year.toString().padStart(4, '0')}-${pad2(month)}-${pad2(day)}"

    /** "dd.MM.yyyy": the display format. */
    fun toDisplayString(): String = "${pad2(day)}.${pad2(month)}.${year.toString().padStart(4, '0')}"

    /** Midnight UTC of this date, the format Material's date picker works with. */
    fun toEpochMillis(): Long = daysFromCivil(year, month, day) * MILLIS_PER_DAY

    companion object {
        private const val MILLIS_PER_DAY = 86_400_000L

        fun parse(iso: String): BirthDate? {
            val parts = iso.split('-')
            if (parts.size != 3) return null
            val year = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull() ?: return null
            val day = parts[2].toIntOrNull() ?: return null
            if (month !in 1..12 || day !in 1..daysInMonth(year, month)) return null
            return BirthDate(year, month, day)
        }

        /** [millis] is midnight UTC of the date, as returned by the date picker. */
        fun fromEpochMillis(millis: Long): BirthDate {
            val (year, month, day) = civilFromDays(millis.floorDiv(MILLIS_PER_DAY))
            return BirthDate(year, month, day)
        }

        private fun pad2(value: Int) = value.toString().padStart(2, '0')

        private fun isLeap(year: Int) = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

        private fun daysInMonth(year: Int, month: Int): Int = when (month) {
            2 -> if (isLeap(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }

        // Days since 1970-01-01 <-> civil date (H. Hinnant's algorithms).
        private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
            val y = if (month <= 2) year - 1 else year
            val era = y.floorDiv(400)
            val yearOfEra = y - era * 400
            val monthShifted = if (month > 2) month - 3 else month + 9
            val dayOfYear = (153 * monthShifted + 2) / 5 + day - 1
            val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
            return era.toLong() * 146_097 + dayOfEra - 719_468
        }

        private fun civilFromDays(days: Long): Triple<Int, Int, Int> {
            val z = days + 719_468
            val era = z.floorDiv(146_097L)
            val dayOfEra = (z - era * 146_097).toInt()
            val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
            val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
            val monthShifted = (5 * dayOfYear + 2) / 153
            val day = dayOfYear - (153 * monthShifted + 2) / 5 + 1
            val month = if (monthShifted < 10) monthShifted + 3 else monthShifted - 9
            val year = (yearOfEra + era * 400).toInt() + if (month <= 2) 1 else 0
            return Triple(year, month, day)
        }
    }
}
