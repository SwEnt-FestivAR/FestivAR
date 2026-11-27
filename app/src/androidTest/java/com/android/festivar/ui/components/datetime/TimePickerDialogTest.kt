// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components.datetime

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimePickerDialogTest {
  @get:Rule val composeTestRule = createComposeRule()

  private var pickedTime: LocalTime? = null
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

  /** Breaks if OK does not confirm the 09:00 default when no initial time is given. */
  @Test
  fun timeDialog_withoutInitialTime_proposesNineOClock() {
    showTimeDialog(initial = null)
    composeTestRule.onNodeWithText("OK").performClick()
    assertEquals(LocalTime.of(9, 0), pickedTime)
  }

  /** Breaks if OK changes the initial time, minutes included. */
  @Test
  fun timeDialog_confirmsTheInitialTime() {
    showTimeDialog(initial = LocalTime.of(14, 35))
    composeTestRule.onNodeWithText("OK").performClick()
    assertEquals(LocalTime.of(14, 35), pickedTime)
  }

  /** Breaks if Cancel on the time dialog does not only dismiss it, or calls onConfirm. */
  @Test
  fun timeDialog_cancelOnlyDismisses() {
    showTimeDialog(initial = LocalTime.of(14, 35))
    composeTestRule.onNodeWithText("Cancel").performClick()
    assertEquals(1, dismissals)
    assertNull(pickedTime)
  }
}
