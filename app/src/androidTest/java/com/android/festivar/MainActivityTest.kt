// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun createsMainActivity() {
    assertEquals("com.android.festivar", composeTestRule.activity.packageName)
  }
}
