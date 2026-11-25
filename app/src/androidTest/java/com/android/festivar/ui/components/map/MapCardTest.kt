// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.map.geometry.EPFL_LOCATION
import com.android.festivar.model.map.geometry.GeoPosition
import com.android.festivar.ui.components.FestivarMapTestTags
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * There is no API key in instrumented tests: the map shows no tiles, and no test looks at them.
 *
 * `gesturesEnabled = false` on the map is not covered by a test, since the map does not expose it:
 * it is checked by hand, on a device.
 */
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
   * Breaks if the touch layer lets a tap reach what is under it, so a tap on a marker of the map
   * moves the camera again.
   */
  @Test
  fun touchBlocker_keepsTheTapsFromWhatIsUnderIt() {
    var clicked = false
    composeTestRule.setContent {
      Box(Modifier.size(100.dp)) {
        // In a Box, a child written later is drawn on top of the ones before it, and receives the
        // touches first: the blocker covers the clickable box, as it covers the map in MapCard.
        Box(Modifier.matchParentSize().testTag("under").clickable { clicked = true })
        TouchBlocker(Modifier.matchParentSize())
      }
    }

    // A click is not sent to the node it finds: it is injected at the center of its bounds and goes
    // through the usual hit testing, so it reaches whatever is on top there, the blocker.
    composeTestRule.onNodeWithTag("under").performClick()

    assertFalse(clicked)
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

  /** Breaks if the map keeps drawing the old content when the content given to the card changes. */
  @Test
  fun card_whenItsContentChanges_composesTheNewContent() {
    var label by mutableStateOf("first")
    var seen = ""
    composeTestRule.setContent {
      AppTheme {
        // Read here, not inside the lambda, so the card gets a new lambda when the label changes.
        val current = label
        MapCard(listOf(EPFL_LOCATION), Modifier.fillMaxWidth().height(156.dp)) {
          SideEffect { seen = current }
        }
      }
    }
    // The map composes its content only once it is ready.
    composeTestRule.waitUntil(timeoutMillis = 5_000) { seen == "first" }

    composeTestRule.runOnIdle { label = "second" }

    composeTestRule.waitUntil(timeoutMillis = 5_000) { seen == "second" }
  }

  /** Breaks if the card disappears or crashes when the shape of the theme changes under it. */
  @Test
  fun card_whenTheThemeShapesChange_staysDisplayed() {
    var shapes by mutableStateOf(Shapes(large = RoundedCornerShape(24.dp)))
    composeTestRule.setContent {
      MaterialTheme(shapes = shapes) {
        MapCard(listOf(EPFL_LOCATION), Modifier.fillMaxWidth().height(156.dp))
      }
    }

    composeTestRule.runOnIdle { shapes = Shapes(large = RoundedCornerShape(4.dp)) }

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).assertIsDisplayed()
  }

  /**
   * Breaks if the card disappears or crashes when its parent recomposes with the same arguments.
   */
  @Test
  fun card_whenItsParentRecomposesWithTheSameArguments_staysDisplayed() {
    val locations = listOf(EPFL_LOCATION)
    val modifier = Modifier.fillMaxWidth().height(156.dp)
    var tick by mutableIntStateOf(0)
    composeTestRule.setContent {
      AppTheme {
        Column {
          Text(tick.toString())
          MapCard(locations, modifier)
        }
      }
    }

    composeTestRule.runOnIdle { tick++ }

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).assertIsDisplayed()
  }

  /** Breaks if the preview of the card, with its marker, fails to compose or to display. */
  @Test
  fun preview_isDisplayed() {
    composeTestRule.setContent { MapCardPreview() }

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).assertIsDisplayed()
  }
}
