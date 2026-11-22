// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.map.geometry.EPFL_LOCATION
import com.android.festivar.model.map.geometry.GeoPosition
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FestivarMapCameraTest {

  /** Breaks if the map no longer looks at EPFL when it is given no location. */
  @Test
  fun cameraPosition_withoutLocation_looksAtEpfl() {
    val target = cameraPositionFor(emptyList()).target

    assertEquals(EPFL_LOCATION.latitude, target.latitude, 0.0)
    assertEquals(EPFL_LOCATION.longitude, target.longitude, 0.0)
  }

  /** Breaks if the map looks anywhere but at its only location (the S6 case, with EPFL). */
  @Test
  fun cameraPosition_withOneLocation_looksAtIt() {
    val target = cameraPositionFor(listOf(EPFL_LOCATION)).target

    assertEquals(EPFL_LOCATION.latitude, target.latitude, 0.0)
    assertEquals(EPFL_LOCATION.longitude, target.longitude, 0.0)
  }

  /** Breaks if the camera is put on the first location, not on the barycenter of all of them. */
  @Test
  fun cameraPosition_withSeveralLocations_looksAtTheirBarycenter() {
    val target =
        cameraPositionFor(
                listOf(GeoPosition(46.0, 6.0), GeoPosition(48.0, 8.0), GeoPosition(47.0, 7.0))
            )
            .target

    assertEquals(47.0, target.latitude, 1e-9)
    assertEquals(7.0, target.longitude, 1e-9)
  }

  /** Breaks if the dark map style is not valid JSON, which Google Maps ignores without a word. */
  @Test
  fun darkMapStyle_isAJsonArrayOfRules() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val json = context.resources.openRawResource(R.raw.map_style_dark).bufferedReader().readText()

    val rules = JSONArray(json)

    assertTrue(rules.length() > 0)
  }

  /** Breaks if the dark theme does not put the dark style on the map, or the light one does. */
  @Test
  fun mapProperties_haveTheDarkStyleOnlyInDarkTheme() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    assertNotNull(mapPropertiesFor(darkTheme = true, context).mapStyleOptions)
    assertNull(mapPropertiesFor(darkTheme = false, context).mapStyleOptions)
  }

  /**
   * Breaks if the gestures parameter stops moving the map, or the map gets a control of its own.
   */
  @Test
  fun mapUiSettings_followTheGesturesParameterAndHaveNoControl() {
    val on = mapUiSettingsFor(gesturesEnabled = true)
    val off = mapUiSettingsFor(gesturesEnabled = false)

    assertTrue(on.scrollGesturesEnabled && on.zoomGesturesEnabled)
    assertTrue(on.rotationGesturesEnabled && on.tiltGesturesEnabled)
    assertFalse(off.scrollGesturesEnabled || off.zoomGesturesEnabled)
    assertFalse(off.rotationGesturesEnabled || off.tiltGesturesEnabled)
    assertFalse(on.zoomControlsEnabled || on.myLocationButtonEnabled || on.mapToolbarEnabled)
  }
}
