// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class JoinCodeTest {
  private val start = ZonedDateTime.of(2026, 10, 17, 10, 0, 0, 0, ZoneId.of("Europe/Zurich"))

  private fun eventWithId(id: String) =
      Event(eventId = id, title = "Fete", startDate = start, endDate = start.plusHours(2))

  @Test
  fun anIdShorterThanSixCharactersGivesAShorterCode() {
    assertEquals("AB1", joinCodeOf(eventWithId("ab1")))
  }

  @Test
  fun punctuationInTheIdIsSkippedBeforeTakingSixCharacters() {
    assertEquals("A1B2C3", joinCodeOf(eventWithId("a1-b2_c3-d4")))
  }

  @Test
  fun aLowercaseIdGivesAnUppercaseCodeOfSixCharacters() {
    val code = joinCodeOf(eventWithId("fete2026xyz"))

    assertEquals("FETE20", code)
    assertEquals(JOIN_CODE_LENGTH, code.length)
  }
}
