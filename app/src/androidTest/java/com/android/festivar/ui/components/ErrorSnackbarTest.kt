package com.android.festivar.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorSnackbarTest {
  @get:Rule val composeTestRule = createComposeRule()

  private fun setContent(onDismiss: () -> Unit, message: String) {
    composeTestRule.setContent { AppTheme() { ErrorSnackbar(onDismiss, message) } }
  }

  @Test
  fun displaysMessage() {
    val message = "Super cool test, sponsored by raid shadow legend !!!!!"

    setContent({}, message)
    composeTestRule.onNodeWithText(message).assertIsDisplayed()
  }

  @Test
  fun onDismissActivatesOnDismiss() {
    val message = "LDM ball cube ball big cube ball next Rest In Peace"
    var hasActivated = false
    fun onDissucced(): () -> Unit = { hasActivated = true }

    setContent(onDissucced(), message)
    composeTestRule.onNodeWithText(message).assertIsDisplayed() // bonus assert
    composeTestRule.onNodeWithText("OK").performClick()
    assertTrue(hasActivated)
  }
}
