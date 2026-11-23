// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.map.geometry.EPFL_LOCATION
import com.android.festivar.model.map.geometry.GeoPosition
import com.android.festivar.ui.theme.AppTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FestivarMapCameraTest {

  @get:Rule val composeTestRule = createComposeRule()

  /** Breaks if the map no longer looks at EPFL when it is given no location. */
  @Test
  fun cameraPosition_withoutLocation_looksAtEpfl() {
    val target = cameraPositionFor(emptyList()).target

    assertEquals(EPFL_LOCATION.latitude, target.latitude, 0.0)
    assertEquals(EPFL_LOCATION.longitude, target.longitude, 0.0)
  }

  /** Breaks if the map looks anywhere but at its only location, for example EPFL. */
  @Test
  fun cameraPosition_withOneLocation_looksAtIt() {
    val target = cameraPositionFor(listOf(EPFL_LOCATION)).target

    assertEquals(EPFL_LOCATION.latitude, target.latitude, 0.0)
    assertEquals(EPFL_LOCATION.longitude, target.longitude, 0.0)
  }

  /**
   * Breaks if the camera is put on the first, the last or the median location, not on the average
   * of all of them.
   */
  @Test
  fun cameraPosition_withSeveralLocations_looksAtTheirBarycenter() {
    // The barycenter, (48.0, 7.0), is none of the three points.
    val target =
        cameraPositionFor(
                listOf(GeoPosition(46.0, 6.0), GeoPosition(48.0, 8.0), GeoPosition(50.0, 7.0))
            )
            .target

    assertEquals(48.0, target.latitude, 1e-9)
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

  /** Breaks if a gesture stays off when gestures are enabled. */
  @Test
  fun mapUiSettings_withGestures_allowAllGestures() {
    val settings = mapUiSettingsFor(gesturesEnabled = true)

    assertTrue(settings.scrollGesturesEnabled)
    assertTrue(settings.zoomGesturesEnabled)
    assertTrue(settings.rotationGesturesEnabled)
    assertTrue(settings.tiltGesturesEnabled)
  }

  /** Breaks if a gesture stays on when gestures are disabled. */
  @Test
  fun mapUiSettings_withoutGestures_blockAllGestures() {
    val settings = mapUiSettingsFor(gesturesEnabled = false)

    assertFalse(settings.scrollGesturesEnabled)
    assertFalse(settings.zoomGesturesEnabled)
    assertFalse(settings.rotationGesturesEnabled)
    assertFalse(settings.tiltGesturesEnabled)
  }

  /** Breaks if the map gets a button of its own, with gestures on or off. */
  @Test
  fun mapUiSettings_haveNoControl() {
    listOf(true, false).forEach { gestures ->
      val settings = mapUiSettingsFor(gesturesEnabled = gestures)

      assertFalse(settings.compassEnabled)
      assertFalse(settings.mapToolbarEnabled)
      assertFalse(settings.myLocationButtonEnabled)
      assertFalse(settings.zoomControlsEnabled)
    }
  }

  /** Breaks if the map no longer reads a dark theme as dark. */
  @Test
  fun colorScheme_ofTheDarkTheme_isDark() {
    var isDark = false
    composeTestRule.setContent {
      AppTheme(darkTheme = true) { isDark = MaterialTheme.colorScheme.isDark() }
    }

    composeTestRule.waitForIdle()

    assertTrue(isDark)
  }

  /** Breaks if the map reads a light theme as dark. */
  @Test
  fun colorScheme_ofTheLightTheme_isNotDark() {
    var isDark = true
    composeTestRule.setContent {
      AppTheme(darkTheme = false) { isDark = MaterialTheme.colorScheme.isDark() }
    }

    composeTestRule.waitForIdle()

    assertFalse(isDark)
  }

  /**
   * Breaks if the camera stays where it was when the locations change after the first display, for
   * example when an event is loaded after its screen is shown (empty list, so EPFL, then the
   * event).
   */
  @Test
  fun cameraState_whenTheLocationsChange_movesToTheNewBarycenter() {
    var locations by mutableStateOf(emptyList<GeoPosition>())
    lateinit var state: CameraPositionState
    composeTestRule.setContent { state = rememberFestivarCameraState(locations) }
    composeTestRule.waitForIdle()
    assertEquals(EPFL_LOCATION.latitude, state.position.target.latitude, 0.0)

    composeTestRule.runOnIdle { locations = listOf(GeoPosition(46.0, 6.0), GeoPosition(48.0, 8.0)) }
    composeTestRule.waitForIdle()

    assertEquals(47.0, state.position.target.latitude, 1e-9)
    assertEquals(7.0, state.position.target.longitude, 1e-9)
  }

  /**
   * Breaks if the camera is put back on the barycenter each time the caller recomposes with the
   * same locations in a new list, which undoes the user's panning.
   */
  @Test
  fun cameraState_withTheSameLocationsInANewList_staysWhereTheUserLeftIt() {
    var recompositions by mutableIntStateOf(0)
    lateinit var state: CameraPositionState
    composeTestRule.setContent {
      // A new list with the same points at every recomposition, as `listOf(...)` in a caller.
      state =
          rememberFestivarCameraState(
              listOf(GeoPosition(46.0 + recompositions * 0.0, 6.0), GeoPosition(48.0, 8.0))
          )
    }
    composeTestRule.waitForIdle()
    val movedByTheUser = CameraPosition.fromLatLngZoom(LatLng(10.0, 20.0), 5f)
    composeTestRule.runOnIdle { state.position = movedByTheUser }

    composeTestRule.runOnIdle { recompositions++ }
    composeTestRule.waitForIdle()

    assertEquals(movedByTheUser, state.position)
  }

  /** Breaks if the manifest asks for a location permission, which the map must never need. */
  @Test
  fun manifest_requestsNoLocationPermission() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val info =
        context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
    val permissions = info.requestedPermissions?.toList().orEmpty()

    assertFalse(permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION))
    assertFalse(permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION))
    assertFalse(permissions.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
  }
}
