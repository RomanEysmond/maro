package com.maro.core.domain.profile

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

class ProfileModelTest {

    @Test
    fun `birth date survives a round trip through epoch millis`() {
        val dates = listOf(
            BirthDate(1970, 1, 1),
            BirthDate(1999, 12, 31),
            BirthDate(2000, 2, 29),
            BirthDate(1955, 7, 4),
            BirthDate(2024, 3, 1),
        )
        dates.forEach { date ->
            assertThat(BirthDate.fromEpochMillis(date.toEpochMillis())).isEqualTo(date)
        }
    }

    @Test
    fun `epoch start is the first of January 1970`() {
        assertThat(BirthDate(1970, 1, 1).toEpochMillis()).isEqualTo(0L)
        assertThat(BirthDate.fromEpochMillis(86_400_000L)).isEqualTo(BirthDate(1970, 1, 2))
    }

    @Test
    fun `birth date is formatted for storage and for display`() {
        val date = BirthDate(1990, 5, 7)

        assertThat(date.toIsoString()).isEqualTo("1990-05-07")
        assertThat(date.toDisplayString()).isEqualTo("07.05.1990")
    }

    @Test
    fun `parsing accepts valid dates and rejects nonsense`() {
        assertThat(BirthDate.parse("1990-05-07")).isEqualTo(BirthDate(1990, 5, 7))
        assertThat(BirthDate.parse("2000-02-29")).isEqualTo(BirthDate(2000, 2, 29))
        assertThat(BirthDate.parse("1900-02-29")).isNull()
        assertThat(BirthDate.parse("1990-13-01")).isNull()
        assertThat(BirthDate.parse("1990-05")).isNull()
        assertThat(BirthDate.parse("abc")).isNull()
    }

    @Test
    fun `username must be five to thirty two lowercase latin letters digits or underscores starting with a letter`() {
        assertThat(ProfileRules.isValidUsername("ivan_1")).isTrue()
        assertThat(ProfileRules.isValidUsername("abcde")).isTrue()
        assertThat(ProfileRules.isValidUsername("abcd")).isFalse()
        assertThat(ProfileRules.isValidUsername("1ivan")).isFalse()
        assertThat(ProfileRules.isValidUsername("Ivan_1")).isFalse()
        assertThat(ProfileRules.isValidUsername("иван_12")).isFalse()
        assertThat(ProfileRules.isValidUsername("a".repeat(32))).isTrue()
        assertThat(ProfileRules.isValidUsername("a".repeat(33))).isFalse()
    }

    @Test
    fun `username normalization trims drops the at sign and lowercases`() {
        assertThat(ProfileRules.normalizeUsername("  @Ivan_Ivanov ")).isEqualTo("ivan_ivanov")
    }

    @Test
    fun `initials and full name skip blank parts`() {
        val profile = UserProfile("id", "иван", "Иванов", "+79001234567")

        assertThat(profile.initials).isEqualTo("ИИ")
        assertThat(profile.fullName).isEqualTo("иван Иванов")
        assertThat(profile.copy(lastName = "").initials).isEqualTo("И")
        assertThat(profile.copy(lastName = "").fullName).isEqualTo("иван")
    }
}
