// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components.datetime

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** `pickDays` on the real Dates field and range picker, in the language of the device. */
@RunWith(AndroidJUnit4::class)
class DateRangePickerTestUtilsTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val today = LocalDate.now()

  private var range by mutableStateOf(DateTimeRange())

  /** The field is in a scrolling column, like in the forms: `pickDays` scrolls to it first. */
  private fun showDatesField() {
    composeTestRule.setContent {
      AppTheme {
        Column(Modifier.verticalScroll(rememberScrollState())) {
          FestivarDatesAndTimes(
              range = range,
              onRangeChange = { range = it },
              allowPast = true,
              today = today,
          )
        }
      }
    }
  }

  /**
   * Breaks if the helper cannot scroll the calendar to a day that is far from the month it opens
   * on: here a year before today and a year after it.
   */
  @Test
  fun pickDays_fromOneYearAgoToInOneYear_savesThoseTwoDays() {
    val start = today.minusYears(1)
    val end = today.plusYears(1)
    showDatesField()

    composeTestRule.pickDays(DatesAndTimesTestTags.DATES_FIELD, start, end)

    composeTestRule.runOnIdle {
      assertEquals(start, range.startDate)
      assertEquals(end, range.endDate)
    }
  }
}
