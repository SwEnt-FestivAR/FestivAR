// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.voicenote

import com.android.festivar.ui.components.formatDate
import com.android.festivar.ui.components.formatDateWithYear
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceNoteFormatTest {
  @Test
  fun formatsSecondsWithTwoDigits() {
    assertEquals("0:14", formatVoiceDuration(Duration.ofSeconds(14)))
    assertEquals("0:03", formatVoiceDuration(Duration.ofSeconds(3)))
    assertEquals("0:00", formatVoiceDuration(Duration.ZERO))
  }

  @Test
  fun formatsMinutes() {
    assertEquals("1:05", formatVoiceDuration(Duration.ofSeconds(65)))
    assertEquals("12:00", formatVoiceDuration(Duration.ofMinutes(12)))
  }

  @Test
  fun addsHoursFromOneHourOn() {
    assertEquals("59:59", formatVoiceDuration(Duration.ofSeconds(3599)))
    assertEquals("1:00:00", formatVoiceDuration(Duration.ofHours(1)))
    assertEquals("1:01:01", formatVoiceDuration(Duration.ofSeconds(61 * 60 + 1)))
    assertEquals("12:05:09", formatVoiceDuration(Duration.ofSeconds(12 * 3600 + 5 * 60 + 9)))
  }

  @Test
  fun roundsSecondsDown() {
    assertEquals("0:01", formatVoiceDuration(Duration.ofMillis(1999)))
  }

  @Test
  fun negativeCountsAsZero() {
    assertEquals("0:00", formatVoiceDuration(Duration.ofSeconds(-5)))
  }

  private val recorded = ZonedDateTime.of(2026, 10, 17, 9, 12, 0, 0, ZoneOffset.UTC)

  private fun clockOn(date: LocalDate, zone: ZoneId = ZoneOffset.UTC) =
      Clock.fixed(Instant.parse("${date}T12:00:00Z"), zone)

  /** Breaks if a note of this year shows its year, or loses the date or the time. */
  @Test
  fun recordedAtHasNoYearInTheCurrentYear() {
    val today = LocalDate.of(2026, 11, 7)
    assertEquals(
        "${formatDate(recorded.toLocalDate())}, 09:12",
        formatRecordedAt(recorded, clockOn(today), "Today"),
    )
  }

  /** Breaks if a note of another year, before or after, does not show its year. */
  @Test
  fun recordedAtShowsTheYearInAnotherYear() {
    val expected = "${formatDateWithYear(recorded.toLocalDate())}, 09:12"
    assertEquals(expected, formatRecordedAt(recorded, clockOn(LocalDate.of(2027, 1, 2)), "Today"))
    assertEquals(expected, formatRecordedAt(recorded, clockOn(LocalDate.of(2025, 12, 31)), "Today"))
  }

  /** Breaks if a note recorded today shows its date, or loses its time. */
  @Test
  fun recordedAtSaysTodayForTheDayOfToday() {
    assertEquals(
        "Today, 09:12",
        formatRecordedAt(recorded, clockOn(LocalDate.of(2026, 10, 17)), "Today"),
    )
  }

  /** Breaks if the day before or after the note is mistaken for today. */
  @Test
  fun recordedAtOnlySaysTodayOnTheSameDay() {
    val expected = "${formatDate(recorded.toLocalDate())}, 09:12"
    assertEquals(expected, formatRecordedAt(recorded, clockOn(LocalDate.of(2026, 10, 18)), "Today"))
    assertEquals(expected, formatRecordedAt(recorded, clockOn(LocalDate.of(2026, 10, 16)), "Today"))
  }

  /**
   * Breaks if the date and the time are read in the zone of the note instead of the device zone.
   */
  @Test
  fun recordedAtUsesTheZoneOfTheDevice() {
    val lateUtc = ZonedDateTime.of(2026, 10, 17, 23, 30, 0, 0, ZoneOffset.UTC)
    val clock = clockOn(LocalDate.of(2026, 10, 18), ZoneId.of("Europe/Paris"))
    assertEquals("Today, 01:30", formatRecordedAt(lateUtc, clock, "Today"))
  }

  /** Breaks if the date format of the author line changes, e.g. loses the weekday or the time. */
  @Test
  fun recordedAtHasTheFormatOfTheDesign() {
    val saved = Locale.getDefault()
    Locale.setDefault(Locale.ENGLISH)
    try {
      val clock = clockOn(LocalDate.of(2026, 11, 7))
      assertEquals("Sat 17 Oct, 09:12", formatRecordedAt(recorded, clock, "Today"))
      assertEquals(
          "Sat 17 Oct 2026, 09:12",
          formatRecordedAt(recorded, clockOn(LocalDate.of(2027, 1, 2)), "Today"),
      )
    } finally {
      Locale.setDefault(saved)
    }
  }
}
