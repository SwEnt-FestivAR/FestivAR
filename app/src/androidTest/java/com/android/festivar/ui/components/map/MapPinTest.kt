// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.map.geometry.EPFL_LOCATION
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * There is no API key in instrumented tests: the map shows no tiles, and no test looks at them.
 *
 * A marker is drawn by Google Maps, not by Compose, so a UI test cannot see a pin on the map. The
 * tests of a map with a pin therefore assert the map only, and the pin is tested on its own, as a
 * [MapPinImage].
 */
@RunWith(AndroidJUnit4::class)
class MapPinTest {
  @get:Rule val composeTestRule = createComposeRule()

  /** Breaks if the map without any pin (S6) fails to display. */
  @Test
  fun map_withNoPin_isDisplayed() {
    composeTestRule.setContent { AppTheme { FestivarMap(emptyList(), Modifier.fillMaxSize()) } }

    composeTestRule.onNodeWithTag(FestivarMapTestTags.MAP).assertIsDisplayed()
  }

  /** Breaks if the map with the event pin fails to display. */
  @Test
  fun map_withEventPin_isDisplayed() {
    showMapWithPin(MapPinKind.EVENT)

    composeTestRule.onNodeWithTag(FestivarMapTestTags.MAP).assertIsDisplayed()
  }

  /** Breaks if the map with the task pin fails to display. */
  @Test
  fun map_withTaskPin_isDisplayed() {
    showMapWithPin(MapPinKind.TASK)

    composeTestRule.onNodeWithTag(FestivarMapTestTags.MAP).assertIsDisplayed()
  }

  /** Shows a map centred on EPFL with a pin of [kind] on it. */
  private fun showMapWithPin(kind: MapPinKind) {
    val location = EPFL_LOCATION
    composeTestRule.setContent {
      AppTheme { FestivarMap(listOf(location), Modifier.fillMaxSize()) { MapPin(location, kind) } }
    }
  }

  /** Breaks if the event pin is not displayed under its own tag. */
  @Test
  fun eventPin_isDisplayed() {
    composeTestRule.setContent { AppTheme { MapPinImage(MapPinKind.EVENT) } }

    composeTestRule.onNodeWithTag(MapPinKind.EVENT.testTag).assertIsDisplayed()
  }

  /** Breaks if the task pin is not displayed under its own tag. */
  @Test
  fun taskPin_isDisplayed() {
    composeTestRule.setContent { AppTheme { MapPinImage(MapPinKind.TASK) } }

    composeTestRule.onNodeWithTag(MapPinKind.TASK.testTag).assertIsDisplayed()
  }

  /** Breaks if the preview of the two pins fails to compose, or does not draw both of them. */
  @Test
  fun pinImagesPreview_showsBothPins() {
    composeTestRule.setContent { MapPinImagePreview() }

    composeTestRule.onNodeWithTag(MapPinKind.EVENT.testTag).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapPinKind.TASK.testTag).assertIsDisplayed()
  }

  /** Breaks if the preview of the map with its pins fails to compose or to display. */
  @Test
  fun mapPinPreview_isDisplayed() {
    composeTestRule.setContent { MapPinPreview() }

    composeTestRule.onNodeWithTag(FestivarMapTestTags.MAP).assertIsDisplayed()
  }

  /** Breaks if the ring of the event pin is not centred on [MapPinAnchor], where a marker sits. */
  @Test fun eventPin_hasItsRingOnTheAnchor() = assertRingOnAnchor(MapPinKind.EVENT)

  /** Breaks if the ring of the task pin is not centred on [MapPinAnchor], where a marker sits. */
  @Test fun taskPin_hasItsRingOnTheAnchor() = assertRingOnAnchor(MapPinKind.TASK)

  /**
   * Draws the pin of [kind] and checks that the ring at its foot is centred on [MapPinAnchor]: on
   * the row of the anchor, the stroke of the ring is as far on its left as on its right. If the
   * anchor missed the ring, the pin would hang off the map centre.
   */
  private fun assertRingOnAnchor(kind: MapPinKind) {
    composeTestRule.setContent { AppTheme { MapPinImage(kind) } }

    val pin = composeTestRule.onNodeWithTag(kind.testTag).captureToImage().toPixelMap()
    val anchorX = (MapPinAnchor.x * pin.width).toInt()
    val anchorY = (MapPinAnchor.y * pin.height).toInt()
    val strokeXs = (0 until pin.width).filter { pin[it, anchorY].alpha > 0.5f }

    val left = strokeXs.lastOrNull { it < anchorX }
    val right = strokeXs.firstOrNull { it > anchorX }
    assertNotNull("No ring on the left of the anchor", left)
    assertNotNull("No ring on the right of the anchor", right)
    assertEquals((anchorX - left!!).toFloat(), (right!! - anchorX).toFloat(), 1f)
  }
}
