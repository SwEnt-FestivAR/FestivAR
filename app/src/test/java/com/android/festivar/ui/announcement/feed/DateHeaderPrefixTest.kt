// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.announcement.feed

import com.android.festivar.R
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DateHeaderPrefixTest {
  private val today = LocalDate.of(2026, 10, 17)

  // Breaks if today's or yesterday's header loses its "Today" or "Yesterday", or they are swapped.
  @Test
  fun todayAndYesterdayHaveTheirPrefix() {
    assertEquals(R.string.announcement_feed_today, dateHeaderPrefixRes(today, today))
    assertEquals(
        R.string.announcement_feed_yesterday,
        dateHeaderPrefixRes(today.minusDays(1), today),
    )
  }

  // Breaks if an older day, or a day after today (e.g. a clock set back), is called "Today" or
  // "Yesterday" instead of only showing its date.
  @Test
  fun otherDaysHaveNoPrefix() {
    assertNull(dateHeaderPrefixRes(today.minusDays(2), today))
    assertNull(dateHeaderPrefixRes(today.plusDays(1), today))
  }
}
