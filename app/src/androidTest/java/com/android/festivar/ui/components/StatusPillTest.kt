// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatusPillTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun showsLabelForEveryStyle() {
    composeTestRule.setContent {
      AppTheme { Column { PillStyle.entries.forEach { StatusPill(label = it.name, style = it) } } }
    }
    PillStyle.entries.forEach { composeTestRule.onNodeWithText(it.name).assertIsDisplayed() }
  }
}
