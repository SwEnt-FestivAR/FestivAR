// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components.datetime

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatesAndTimesEndTimeTest {
  @get:Rule val composeTestRule = createComposeRule()

  /**
   * Breaks if the End time dialog opens on its 09:00 default when only a start time is set, instead
   * of on the start time: confirming it unchanged must give the start time.
   */
  @Test
  fun endTimeDialog_withOnlyAStartTime_opensOnTheStartTime() {
    val startTime = LocalTime.of(14, 35)
    var range = DateTimeRange(startDate = LocalDate.of(2026, 3, 28), startTime = startTime)
    composeTestRule.setContent {
      AppTheme {
        FestivarDatesAndTimes(
            range = range,
            onRangeChange = { range = it },
            allowPast = true,
            today = LocalDate.of(2026, 3, 28),
        )
      }
    }

    composeTestRule.onNodeWithTag(DatesAndTimesTestTags.END_TIME_FIELD).performClick()
    composeTestRule.onNodeWithText("OK").performClick()

    composeTestRule.runOnIdle { assertEquals(startTime, range.endTime) }
  }
}
