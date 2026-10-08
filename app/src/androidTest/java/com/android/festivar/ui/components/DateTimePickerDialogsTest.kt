// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DateTimePickerDialogsTest {
  @get:Rule val composeTestRule = createComposeRule()

  private var pickedTime: LocalTime? = null
  private var pickedDate: LocalDate? = null
  private var dismissals = 0

  private fun showTimeDialog(initial: LocalTime?) {
    composeTestRule.setContent {
      AppTheme {
        FestivarTimePickerDialog(
            initial = initial,
            onConfirm = { pickedTime = it },
            onDismiss = { dismissals++ },
        )
      }
    }
  }

  private fun showDateDialog(initial: LocalDate?) {
    composeTestRule.setContent {
      AppTheme {
        FestivarDatePickerDialog(
            initial = initial,
            onConfirm = { pickedDate = it },
            onDismiss = { dismissals++ },
        )
      }
    }
  }

  // --- Time dialog ---------------------------------------------------------------------------

  @Test
  fun timeDialog_withoutInitialTime_proposesNineOClock() {
    showTimeDialog(initial = null)
    composeTestRule.onNodeWithText("OK").performClick()
    assertEquals(LocalTime.of(9, 0), pickedTime)
  }

  @Test
  fun timeDialog_confirmsTheInitialTime() {
    showTimeDialog(initial = LocalTime.of(14, 35))
    composeTestRule.onNodeWithText("OK").performClick()
    assertEquals(LocalTime.of(14, 35), pickedTime)
  }

  @Test
  fun timeDialog_cancelOnlyDismisses() {
    showTimeDialog(initial = LocalTime.of(14, 35))
    composeTestRule.onNodeWithText("Cancel").performClick()
    assertEquals(1, dismissals)
    assertNull(pickedTime)
  }

  // --- Date dialog ---------------------------------------------------------------------------

  @Test
  fun dateDialog_confirmsTheInitialDate() {
    showDateDialog(initial = LocalDate.of(2026, 10, 17))
    composeTestRule.onNodeWithText("OK").performClick()
    assertEquals(LocalDate.of(2026, 10, 17), pickedDate)
  }

  @Test
  fun dateDialog_cannotBeConfirmedBeforeADateIsSelected() {
    showDateDialog(initial = null)
    composeTestRule.onNodeWithText("OK").assertIsNotEnabled()
    assertNull(pickedDate)
  }

  @Test
  fun dateDialog_confirmsTheDayThatWasTapped() {
    showDateDialog(initial = null)

    composeTestRule.onNode(hasText("15") and hasClickAction()).performClick()
    composeTestRule.onNodeWithText("OK").performClick()

    assertEquals(LocalDate.now().withDayOfMonth(15), pickedDate)
  }

  @Test
  fun dateDialog_cancelOnlyDismisses() {
    showDateDialog(initial = LocalDate.of(2026, 10, 17))
    composeTestRule.onNodeWithText("Cancel").performClick()
    assertEquals(1, dismissals)
    assertNull(pickedDate)
  }
}
