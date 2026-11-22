// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MapGeometryTest {

  private val lausanne = GeoPosition(46.5191, 6.5668)
  private val millimetre = 1e-3

  private fun at(x: Double, y: Double) = LocalPosition(x, y)

  private fun assertPosition(expected: LocalPosition, actual: LocalPosition, delta: Double = 1e-9) {
    assertEquals(expected.x, actual.x, delta)
    assertEquals(expected.y, actual.y, delta)
  }

  // ---------------------------------------------------------------------------------------------
  // toLocal and toGeo
  // ---------------------------------------------------------------------------------------------

  /** Breaks if the origin is not the zero of the local coordinates. */
  @Test
  fun theOriginIsAtZero() {
    assertPosition(at(0.0, 0.0), toLocal(lausanne, lausanne))
    assertEquals(lausanne, toGeo(lausanne, at(0.0, 0.0)))
  }

  /** Breaks if y points south, or latitude and longitude are swapped. */
  @Test
  fun northIsPositiveYAndEastIsPositiveX() {
    val north = toLocal(lausanne, GeoPosition(lausanne.latitude + 0.001, lausanne.longitude))
    val east = toLocal(lausanne, GeoPosition(lausanne.latitude, lausanne.longitude + 0.001))

    assertEquals(0.0, north.x, millimetre)
    assertTrue(north.y > 0)
    assertTrue(east.x > 0)
    assertEquals(0.0, east.y, millimetre)
  }

  /**
   * Breaks if the cosine of the latitude is dropped, or degrees are taken for radians: a thousandth
   * of a degree is 111.19 m of latitude anywhere, and 76.52 m of longitude at Lausanne.
   */
  @Test
  fun aThousandthOfADegreeHasTheRightLength() {
    val north = toLocal(lausanne, GeoPosition(lausanne.latitude + 0.001, lausanne.longitude))
    val east = toLocal(lausanne, GeoPosition(lausanne.latitude, lausanne.longitude + 0.001))

    assertEquals(111.19, north.y, 0.01)
    assertEquals(76.52, east.x, 0.01)
  }

  /**
   * Breaks if toGeo is not the exact inverse of toLocal, for instance a cosine taken at another
   * latitude in one direction only. Checked up to 5 km from the origin, in the four quadrants.
   */
  @Test
  fun aRoundTripGivesBackTheSamePoint() {
    val origins = listOf(lausanne, GeoPosition(0.0, 0.0), GeoPosition(-33.86, 151.21))
    val points = listOf(at(1.0, 1.0), at(-250.0, 80.0), at(3000.0, -4000.0), at(-3500.0, -3500.0))

    for (origin in origins) {
      for (point in points) {
        assertPosition(point, toLocal(origin, toGeo(origin, point)), delta = 1e-6)
      }
    }
  }

  /**
   * Breaks if a result more than half a turn of longitude from the origin is returned instead of
   * being refused: next to a pole one metre east is more than a full turn.
   */
  @Test
  fun toGeoRefusesMoreThanHalfATurnOfLongitude() {
    assertThrows(IllegalArgumentException::class.java) {
      toGeo(GeoPosition(90.0, 0.0), at(1.0, 0.0))
    }
  }

  /**
   * Breaks if a result past a pole is returned instead of being refused: 100 m north of latitude
   * 89.9999 is beyond 90 degrees, which a GeoPosition cannot hold.
   */
  @Test
  fun toGeoRefusesAResultPastAPole() {
    assertThrows(IllegalArgumentException::class.java) {
      toGeo(GeoPosition(89.9999, 0.0), at(0.0, 100.0))
    }
  }

  /**
   * Breaks if a site next to longitude 180 is refused: 100 m east of 179.9995 is a real place, on
   * the other side of the antimeridian.
   */
  @Test
  fun aSiteAcrossTheAntimeridianIsConverted() {
    val origin = GeoPosition(0.0, 179.9995)

    val east = toGeo(origin, at(100.0, 0.0))

    assertEquals(-179.9996, east.longitude, 1e-4)
    assertPosition(at(100.0, 0.0), toLocal(origin, east), delta = 1e-6)
    assertPosition(at(-100.0, 0.0), toLocal(east, origin), delta = 1e-6)
  }

  // ---------------------------------------------------------------------------------------------
  // distanceMeters
  // ---------------------------------------------------------------------------------------------

  /** Breaks if the local distance is not the straight line: a 3, 4, 5 triangle. */
  @Test
  fun theLocalDistanceIsTheStraightLine() {
    assertEquals(5.0, distanceMeters(at(1.0, 1.0), at(4.0, 5.0)), 1e-12)
    assertEquals(0.0, distanceMeters(at(2.0, 2.0), at(2.0, 2.0)), 0.0)
  }

  /**
   * Breaks if the distance on Earth uses the wrong radius or mixes degrees and radians: one degree
   * of latitude is 111.19 km.
   */
  @Test
  fun theDistanceOnEarthOfOneDegreeOfLatitude() {
    val distance = distanceMeters(GeoPosition(46.0, 6.0), GeoPosition(47.0, 6.0))

    assertEquals(111_194.9, distance, 0.1)
    assertEquals(0.0, distanceMeters(lausanne, lausanne), 0.0)
  }

  /**
   * Breaks if the two distances disagree at the scale of a site, where a boundary is measured with
   * one and drawn with the other.
   */
  @Test
  fun bothDistancesAgreeAtTheScaleOfASite() {
    val far = toGeo(lausanne, at(600.0, 800.0))

    assertEquals(1000.0, distanceMeters(lausanne, far), 0.1)
  }

  // ---------------------------------------------------------------------------------------------
  // normalizeDegrees
  // ---------------------------------------------------------------------------------------------

  /** Breaks if a negative angle stays negative, or a full turn is not brought back to 0. */
  @Test
  fun degreesAreBroughtBackWithinOneTurn() {
    assertEquals(0.0, normalizeDegrees(0.0), 0.0)
    assertEquals(0.0, normalizeDegrees(360.0), 0.0)
    assertEquals(90.0, normalizeDegrees(450.0), 0.0)
    assertEquals(270.0, normalizeDegrees(-90.0), 0.0)
    assertEquals(359.5, normalizeDegrees(-720.5), 1e-9)
  }

  /** Breaks if an angle that is not a number is passed on as NaN instead of being refused. */
  @Test
  fun anAngleThatIsNotFiniteIsRefused() {
    for (angle in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
      val exception = assertThrows(IllegalArgumentException::class.java) { normalizeDegrees(angle) }
      assertEquals("The angle must be finite.", exception.message)
    }
  }

  // ---------------------------------------------------------------------------------------------
  // distanceToSegment
  // ---------------------------------------------------------------------------------------------

  /**
   * Breaks if the distance is taken to the whole line: level with the segment it is the
   * perpendicular, beyond an end it is the distance to that end.
   */
  @Test
  fun theDistanceToASegmentStopsAtItsEnds() {
    val a = at(0.0, 0.0)
    val b = at(10.0, 0.0)

    assertEquals(3.0, distanceToSegment(at(4.0, 3.0), a, b), 1e-12)
    assertEquals(3.0, distanceToSegment(at(4.0, -3.0), a, b), 1e-12)
    assertEquals(5.0, distanceToSegment(at(13.0, 4.0), a, b), 1e-12)
    assertEquals(5.0, distanceToSegment(at(-3.0, -4.0), a, b), 1e-12)
  }

  /** Breaks if a point of the segment is not at distance zero, its ends included. */
  @Test
  fun aPointOfTheSegmentIsAtDistanceZero() {
    val a = at(2.0, 2.0)
    val b = at(6.0, 4.0)

    assertEquals(0.0, distanceToSegment(at(4.0, 3.0), a, b), 1e-12)
    assertEquals(0.0, distanceToSegment(a, a, b), 0.0)
    assertEquals(0.0, distanceToSegment(b, a, b), 1e-12)
  }

  /** Breaks if a segment without length divides by zero. */
  @Test
  fun aSegmentWithoutLengthIsAPoint() {
    assertEquals(5.0, distanceToSegment(at(3.0, 4.0), at(0.0, 0.0), at(0.0, 0.0)), 1e-12)
  }
}
