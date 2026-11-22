// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.map.geometry.EPFL_LOCATION
import com.android.festivar.model.map.geometry.GeoPosition
import com.android.festivar.ui.theme.AppTheme
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberUpdatedMarkerState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** There is no API key in instrumented tests: the map shows no tiles, and no test looks at them. */
@RunWith(AndroidJUnit4::class)
class MapCardTest {
  @get:Rule val composeTestRule = createComposeRule()

  private fun setCard(
      locations: List<GeoPosition>,
      shape: Shape? = null,
      content: @Composable () -> Unit = {},
  ) {
    composeTestRule.setContent {
      AppTheme {
        val modifier = Modifier.fillMaxWidth().height(156.dp)
        if (shape == null) MapCard(locations, modifier, content = content)
        else MapCard(locations, modifier, shape, content)
      }
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

  /** Breaks if the card becomes clickable, since it must not react to touches in S6. */
  @Test
  fun card_isNotClickable() {
    setCard(listOf(EPFL_LOCATION))

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).assertHasNoClickAction()
  }

  /** Breaks if the card does not hand its content to the map, so markers are never composed. */
  @Test
  fun card_composesTheContentItIsGiven() {
    var composed = false
    setCard(listOf(EPFL_LOCATION)) {
      SideEffect { composed = true }
      Marker(
          state =
              rememberUpdatedMarkerState(LatLng(EPFL_LOCATION.latitude, EPFL_LOCATION.longitude))
      )
    }

    composeTestRule.waitForIdle()
    assertTrue(composed)
  }

  /** Breaks if the card fails to display when a screen gives it its own shape. */
  @Test
  fun card_withCustomShape_isDisplayed() {
    setCard(listOf(EPFL_LOCATION), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))

    composeTestRule.onNodeWithTag(MapCardTestTags.CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FestivarMapTestTags.MAP).assertIsDisplayed()
  }
}
