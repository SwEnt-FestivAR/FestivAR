// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
class DateTimeFieldsTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val fieldTag = "field"
  private var pickedDate: LocalDate? = null
  private var pickedTime: LocalTime? = null

  private fun showDateField(value: LocalDate?, initialWhenEmpty: LocalDate? = null) {
    composeTestRule.setContent {
      AppTheme {
        FestivarDateField(
            value = value,
            label = "Start date",
            onValueChange = { pickedDate = it },
            modifier = Modifier.testTag(fieldTag),
            initialWhenEmpty = initialWhenEmpty,
        )
      }
    }
  }

  private fun showTimeField(value: LocalTime?, initialWhenEmpty: LocalTime? = null) {
    composeTestRule.setContent {
      AppTheme {
        FestivarTimeField(
            value = value,
            label = "Start time",
            onValueChange = { pickedTime = it },
            modifier = Modifier.testTag(fieldTag),
            initialWhenEmpty = initialWhenEmpty,
        )
      }
    }
  }

  // The field shows the date the way the forms format it.
  @Test
  fun dateField_showsTheFormattedDate() {
    showDateField(LocalDate.of(2026, 10, 17))

    composeTestRule.onNodeWithTag(fieldTag).assertTextContains("Sat 17 Oct")
  }

  // Tapping the field opens the dialog, and OK reports the date and closes the dialog.
  @Test
  fun dateField_tapThenOk_reportsTheDateAndClosesTheDialog() {
    showDateField(LocalDate.of(2026, 10, 17))

    composeTestRule.onNodeWithTag(fieldTag).performClick()
    composeTestRule.onNodeWithText("OK").assertIsDisplayed().performClick()

    assertEquals(LocalDate.of(2026, 10, 17), pickedDate)
    composeTestRule.onNodeWithText("OK").assertDoesNotExist()
  }

  // Cancel closes the dialog without reporting anything.
  @Test
  fun dateField_tapThenCancel_reportsNothing() {
    showDateField(LocalDate.of(2026, 10, 17))

    composeTestRule.onNodeWithTag(fieldTag).performClick()
    composeTestRule.onNodeWithText("Cancel").performClick()

    assertNull(pickedDate)
    composeTestRule.onNodeWithText("Cancel").assertDoesNotExist()
  }

  // With no value, the dialog opens on initialWhenEmpty (e.g. the start date for an end date).
  @Test
  fun dateField_withoutValue_opensOnTheInitialWhenEmptyDate() {
    showDateField(value = null, initialWhenEmpty = LocalDate.of(2026, 10, 20))

    composeTestRule.onNodeWithTag(fieldTag).performClick()
    composeTestRule.onNodeWithText("OK").performClick()

    assertEquals(LocalDate.of(2026, 10, 20), pickedDate)
  }

  // The field shows the time on a 24-hour clock.
  @Test
  fun timeField_showsTheFormattedTime() {
    showTimeField(LocalTime.of(9, 30))

    composeTestRule.onNodeWithTag(fieldTag).assertTextContains("09:30")
  }

  // Tapping the field opens the dialog, and OK reports the time and closes the dialog.
  @Test
  fun timeField_tapThenOk_reportsTheTimeAndClosesTheDialog() {
    showTimeField(LocalTime.of(14, 35))

    composeTestRule.onNodeWithTag(fieldTag).performClick()
    composeTestRule.onNodeWithText("OK").assertIsDisplayed().performClick()

    assertEquals(LocalTime.of(14, 35), pickedTime)
    composeTestRule.onNodeWithText("OK").assertDoesNotExist()
  }

  // Cancel closes the dialog without reporting anything.
  @Test
  fun timeField_tapThenCancel_reportsNothing() {
    showTimeField(LocalTime.of(14, 35))

    composeTestRule.onNodeWithTag(fieldTag).performClick()
    composeTestRule.onNodeWithText("Cancel").performClick()

    assertNull(pickedTime)
    composeTestRule.onNodeWithText("Cancel").assertDoesNotExist()
  }

  // With no value and no initialWhenEmpty, the dialog proposes 09:00.
  @Test
  fun timeField_withoutValue_proposesNineOClock() {
    showTimeField(value = null)

    composeTestRule.onNodeWithTag(fieldTag).performClick()
    composeTestRule.onNodeWithText("OK").performClick()

    assertEquals(LocalTime.of(9, 0), pickedTime)
  }
}
