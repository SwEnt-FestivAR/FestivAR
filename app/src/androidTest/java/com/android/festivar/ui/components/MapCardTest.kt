// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.map.geometry.EPFL_LOCATION
import com.android.festivar.model.map.geometry.GeoPosition
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** There is no API key in instrumented tests: the map shows no tiles, and no test looks at them. */
@RunWith(AndroidJUnit4::class)
class MapCardTest {
  @get:Rule val composeTestRule = createComposeRule()

  private fun setCard(locations: List<GeoPosition>, content: @Composable () -> Unit = {}) {
    composeTestRule.setContent {
      AppTheme { MapCard(locations, Modifier.fillMaxWidth().height(156.dp), content = content) }
    }
  }

  /** Breaks if the card, or the map it holds, is not on screen when given EPFL. */
  @Test
  fun card_withLocation_showsCardAndMap() {
    setCard(listOf(EPFL_LOCATION))

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FestivarMapTestTags.MAP).assertIsDisplayed()
  }

  /** Breaks if an empty list of locations crashes the card or hides it. */
  @Test
  fun card_withoutLocation_isDisplayed() {
    setCard(emptyList())

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).assertIsDisplayed()
  }

  /**
   * Breaks if the card no longer covers the map with its touch layer, so that a tap on a marker
   * moves the camera again.
   */
  @Test
  fun card_coversTheMapWithATouchLayer() {
    setCard(listOf(EPFL_LOCATION))

    composeTestRule.onNodeWithTag(MapCardTestTags.TOUCH_BLOCKER).assertIsDisplayed()
  }

  /**
   * Breaks if the touch layer swallows the taps, so a screen can no longer make the whole card
   * clickable.
   */
  @Test
  fun card_stillPassesTapsToAClickableParent() {
    var clicked = false
    composeTestRule.setContent {
      AppTheme {
        Box(Modifier.clickable { clicked = true }) {
          MapCard(listOf(EPFL_LOCATION), Modifier.fillMaxWidth().height(156.dp))
        }
      }
    }

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD, useUnmergedTree = true).performClick()

    assertTrue(clicked)
  }

  /** Breaks if the card does not hand its content to the map, so markers are never composed. */
  @Test
  fun card_composesTheContentItIsGiven() {
    var composed = false
    setCard(listOf(EPFL_LOCATION)) { SideEffect { composed = true } }

    // The map composes its content only once it is ready.
    composeTestRule.waitUntil(timeoutMillis = 5_000) { composed }
  }

  /** Breaks if the card needs a modifier from its caller to be composed. */
  @Test
  fun card_withoutModifier_isComposed() {
    composeTestRule.setContent { AppTheme { MapCard(listOf(EPFL_LOCATION)) } }

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).assertExists()
  }

  /** Breaks if a shape given by the screen makes the card fail to display. */
  @Test
  fun card_withCustomShape_isDisplayed() {
    composeTestRule.setContent {
      AppTheme {
        MapCard(
            listOf(EPFL_LOCATION),
            Modifier.fillMaxWidth().height(156.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        )
      }
    }

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).assertIsDisplayed()
  }

  /** Breaks if the card crashes or disappears when its locations change. */
  @Test
  fun card_whenItsLocationsChange_staysDisplayed() {
    var locations by mutableStateOf(listOf(EPFL_LOCATION))
    composeTestRule.setContent {
      AppTheme { MapCard(locations, Modifier.fillMaxWidth().height(156.dp)) }
    }

    composeTestRule.runOnIdle { locations = listOf(GeoPosition(46.0, 7.0), GeoPosition(47.0, 8.0)) }

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).assertIsDisplayed()
  }

  /** Breaks if the preview of the card, with its marker, fails to compose or to display. */
  @Test
  fun preview_isDisplayed() {
    composeTestRule.setContent { MapCardPreview() }

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).assertIsDisplayed()
  }
}
