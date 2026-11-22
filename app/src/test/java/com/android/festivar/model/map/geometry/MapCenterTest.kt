// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.map.geometry

import org.junit.Assert.assertEquals
import org.junit.Test

class MapCenterTest {

  /** Breaks if the EPFL location is not the campus coordinates (46.5191, 6.5668). */
  @Test
  fun theEpflLocationIsTheOneOfTheCampus() {
    assertEquals(GeoPosition(46.5191, 6.5668), EPFL_LOCATION)
  }

  /** Breaks if the barycenter of a single location is anything but that location. */
  @Test
  fun theBarycenterOfOneLocationIsThatLocation() {
    val lausanne = GeoPosition(46.52, 6.63)

    assertEquals(lausanne, barycenter(listOf(lausanne)))
  }

  /** Breaks if the barycenter is not the mean of the latitudes and of the longitudes. */
  @Test
  fun theBarycenterOfSeveralLocationsIsTheirMean() {
    val result =
        barycenter(listOf(GeoPosition(10.0, 20.0), GeoPosition(20.0, -10.0), GeoPosition(0.0, 5.0)))

    assertEquals(10.0, result.latitude, 1e-9)
    assertEquals(5.0, result.longitude, 1e-9)
  }

  /** Breaks if an empty list crashes or gives anything but the EPFL location. */
  @Test
  fun theBarycenterOfNoLocationIsTheEpflLocation() {
    assertEquals(EPFL_LOCATION, barycenter(emptyList()))
  }
}
