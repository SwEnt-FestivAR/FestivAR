// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.map.geometry.EPFL_LOCATION
import com.android.festivar.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** There is no API key in instrumented tests: the map shows no tiles, and no test looks at them. */
@RunWith(AndroidJUnit4::class)
class FestivarMapTest {
  @get:Rule val composeTestRule = createComposeRule()

  /** Breaks if the map is not on screen when it is given EPFL, as in S6. */
  @Test
  fun map_withEpfl_isDisplayed() {
    composeTestRule.setContent {
      AppTheme {
        Box(Modifier.fillMaxSize()) { FestivarMap(listOf(EPFL_LOCATION), Modifier.fillMaxSize()) }
      }
    }

    composeTestRule.onNodeWithTag(FestivarMapTestTags.MAP).assertIsDisplayed()
  }

  /** Breaks if an empty list of locations crashes the map or hides it. */
  @Test
  fun map_withoutLocation_isDisplayed() {
    composeTestRule.setContent { AppTheme { FestivarMap(emptyList(), Modifier.fillMaxSize()) } }

    composeTestRule.onNodeWithTag(FestivarMapTestTags.MAP).assertIsDisplayed()
  }

  /** Breaks if the dark style makes the map fail to display. */
  @Test
  fun map_inDarkTheme_isDisplayed() {
    composeTestRule.setContent {
      AppTheme(darkTheme = true) {
        FestivarMap(listOf(EPFL_LOCATION), Modifier.fillMaxSize(), darkTheme = true)
      }
    }

    composeTestRule.onNodeWithTag(FestivarMapTestTags.MAP).assertIsDisplayed()
  }
}
