// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.map

import kotlin.math.cos
import org.junit.Assert.assertEquals
import org.junit.Test

class VenueGeometryTest {
  private val lausanneLat = 46.5197
  private val lausanneLng = 6.6323

  @Test
  fun atBearingNinetyAPlanPointEastLandsSouthOfTheCentre() {
    val p = VenueGeometry.latLngFrom(lausanneLat, lausanneLng, 90.0, 10.0, 0.0)

    val (east, south) = VenueGeometry.metresFrom(lausanneLat, lausanneLng, p.latitude, p.longitude)
    assertEquals(0.0, east, 1e-3)
    assertEquals(10.0, south, 1e-3)
  }

  @Test
  fun atBearingNinetyAPlanPointSouthLandsWestOfTheCentre() {
    val p = VenueGeometry.latLngFrom(lausanneLat, lausanneLng, 90.0, 0.0, 10.0)

    val (east, south) = VenueGeometry.metresFrom(lausanneLat, lausanneLng, p.latitude, p.longitude)
    assertEquals(-10.0, east, 1e-3)
    assertEquals(0.0, south, 1e-3)
  }

  @Test
  fun latLngFromThenMetresFromReturnsThePlanPointAtEveryBearing() {
    val centres = listOf(lausanneLat to lausanneLng, 0.0 to 0.0)
    for ((lat, lng) in centres) {
      for (bearing in listOf(0.0, 37.0, 90.0, 180.0, 270.0)) {
        val p = VenueGeometry.latLngFrom(lat, lng, bearing, 42.5, -17.25)

        val (x, y) = VenueGeometry.metresFrom(lat, lng, bearing, p.latitude, p.longitude)
        assertEquals("x at $lat, $lng, bearing $bearing", 42.5, x, 1e-3)
        assertEquals("y at $lat, $lng, bearing $bearing", -17.25, y, 1e-3)
      }
    }
  }

  @Test
  fun aHundredMetresEastAtLausanneIsAboutAThousandthOfADegreeOfLongitude() {
    val p = VenueGeometry.latLngFrom(46.52, 0.0, 0.0, 100.0, 0.0)

    val expected = Math.toDegrees(100.0 / (cos(Math.toRadians(46.52)) * 6_371_000.0))
    assertEquals(0.001306, expected, 1e-6)
    assertEquals(expected, p.longitude, 1e-9)
    assertEquals(46.52, p.latitude, 1e-12)
  }

  @Test
  fun cornersGoClockwiseFromTheTopLeft() {
    val corners = VenueGeometry.corners(lausanneLat, lausanneLng, 0.0, 2.0, 3.0, 10.0, 4.0)

    assertEquals(4, corners.size)
    val plan = corners.map {
      VenueGeometry.metresFrom(lausanneLat, lausanneLng, it.latitude, it.longitude)
    }
    val expected = listOf(2.0 to 3.0, 12.0 to 3.0, 12.0 to 7.0, 2.0 to 7.0)
    plan.zip(expected).forEach { (actual, wanted) ->
      assertEquals(wanted.first, actual.first, 1e-3)
      assertEquals(wanted.second, actual.second, 1e-3)
    }
  }

  @Test
  fun cornersTurnWithTheBearing() {
    val corners = VenueGeometry.corners(lausanneLat, lausanneLng, 90.0, 0.0, 0.0, 10.0, 4.0)

    val (east, south) =
        VenueGeometry.metresFrom(
            lausanneLat,
            lausanneLng,
            corners[1].latitude,
            corners[1].longitude,
        )
    assertEquals(0.0, east, 1e-3)
    assertEquals(10.0, south, 1e-3)
  }

  @Test
  fun aPointNorthOfTheVenueHasANegativeYAndNoX() {
    val (x, y) = VenueGeometry.metresFrom(46.5165, 6.5990, 46.5175, 6.5990)

    assertEquals(0.0, x, 1e-6)
    assertEquals(-111.19, y, 0.05)
  }

  @Test
  fun aPointEastOfTheVenueShrinksWithTheCosineOfTheLatitude() {
    val (x, y) = VenueGeometry.metresFrom(46.5165, 6.5990, 46.5165, 6.6000)

    assertEquals(111.19 * cos(Math.toRadians(46.5165)), x, 0.05)
    assertEquals(0.0, y, 1e-6)
  }

  @Test
  fun distanceIsTheStraightLineBetweenTwoPoints() {
    assertEquals(5.0, VenueGeometry.distanceMetres(1.0, 1.0, 4.0, 5.0), 1e-9)
  }

  @Test
  fun walkingMinutesRoundUpAtOnePointTwoMetresASecondAndNeverDropBelowOne() {
    assertEquals(1, VenueGeometry.walkingMinutes(0.0))
    assertEquals(1, VenueGeometry.walkingMinutes(72.0))
    assertEquals(2, VenueGeometry.walkingMinutes(73.0))
    assertEquals(2, VenueGeometry.walkingMinutes(120.0))
  }

  @Test
  fun snapRoundsToTheNearestHalfMetre() {
    assertEquals(-20.5, VenueGeometry.snap(-20.7), 1e-9)
    assertEquals(6.0, VenueGeometry.snap(5.8), 1e-9)
    assertEquals(2.5, VenueGeometry.snap(2.4), 1e-9)
  }

  @Test
  fun metresTextDropsTheDecimalOfWholeMetresOnly() {
    assertEquals("6", VenueGeometry.metresText(6.0))
    assertEquals("2.5", VenueGeometry.metresText(2.5))
  }
}
