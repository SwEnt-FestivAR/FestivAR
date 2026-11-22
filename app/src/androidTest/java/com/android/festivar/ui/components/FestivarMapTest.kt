// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.map.geometry.EPFL_LOCATION
import com.android.festivar.model.map.geometry.GeoPosition
import com.android.festivar.ui.theme.AppTheme
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberUpdatedMarkerState
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
      AppTheme(darkTheme = true) { FestivarMap(listOf(EPFL_LOCATION), Modifier.fillMaxSize()) }
    }

    composeTestRule.onNodeWithTag(FestivarMapTestTags.MAP).assertIsDisplayed()
  }

  /** Breaks if the map needs a modifier from its caller to be composed. */
  @Test
  fun map_withoutModifier_isComposed() {
    composeTestRule.setContent { AppTheme { FestivarMap(listOf(EPFL_LOCATION)) } }

    composeTestRule.onNodeWithTag(FestivarMapTestTags.MAP).assertExists()
  }

  /** Breaks if giving the map its gestures and a marker makes it fail to display. */
  @Test
  fun map_withGesturesAndMarker_isDisplayed() {
    composeTestRule.setContent {
      AppTheme {
        FestivarMap(
            listOf(EPFL_LOCATION),
            Modifier.fillMaxSize(),
            gesturesEnabled = true,
        ) {
          Marker(
              state =
                  rememberUpdatedMarkerState(
                      position = LatLng(EPFL_LOCATION.latitude, EPFL_LOCATION.longitude)
                  )
          )
        }
      }
    }

    composeTestRule.onNodeWithTag(FestivarMapTestTags.MAP).assertIsDisplayed()
  }

  /** Breaks if the map crashes or disappears when its locations, theme or gestures change. */
  @Test
  fun map_whenItsInputsChange_staysDisplayed() {
    var locations by mutableStateOf(listOf(EPFL_LOCATION))
    var darkTheme by mutableStateOf(false)
    var gesturesEnabled by mutableStateOf(false)
    composeTestRule.setContent {
      AppTheme(darkTheme = darkTheme) {
        FestivarMap(locations, Modifier.fillMaxSize(), gesturesEnabled = gesturesEnabled)
      }
    }

    composeTestRule.runOnIdle { locations = listOf(GeoPosition(46.0, 7.0), GeoPosition(47.0, 8.0)) }
    composeTestRule.runOnIdle { darkTheme = true }
    composeTestRule.runOnIdle { gesturesEnabled = true }

    composeTestRule.onNodeWithTag(FestivarMapTestTags.MAP).assertIsDisplayed()
  }
}
