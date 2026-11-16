// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map.geometry

import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MapGeometryTest {

  private val lausanne = GeoPosition(46.5191, 6.5668)
  private val millimetre = 1e-3

  private fun at(x: Double, y: Double) = LocalPosition(x, y)

  /** A 10 by 10 m square with its south west corner on the origin, counterclockwise. */
  private val square = listOf(at(0.0, 0.0), at(10.0, 0.0), at(10.0, 10.0), at(0.0, 10.0))

  private fun assertPosition(expected: LocalPosition, actual: LocalPosition, delta: Double = 1e-9) {
    assertEquals(expected.x, actual.x, delta)
    assertEquals(expected.y, actual.y, delta)
  }

  private fun assertInvalid(expectedMessage: String, call: () -> Any) {
    val exception = assertThrows(IllegalArgumentException::class.java) { call() }
    assertEquals(expectedMessage, exception.message)
  }

  // ---------------------------------------------------------------------------------------------
  // toLocal and toGeo
  // ---------------------------------------------------------------------------------------------

  // Breaks if the origin is not the zero of the local coordinates.
  @Test
  fun theOriginIsAtZero() {
    assertPosition(at(0.0, 0.0), toLocal(lausanne, lausanne))
    assertEquals(lausanne, toGeo(lausanne, at(0.0, 0.0)))
  }

  // Breaks if y points south, or latitude and longitude are swapped.
  @Test
  fun northIsPositiveYAndEastIsPositiveX() {
    val north = toLocal(lausanne, GeoPosition(lausanne.latitude + 0.001, lausanne.longitude))
    val east = toLocal(lausanne, GeoPosition(lausanne.latitude, lausanne.longitude + 0.001))

    assertEquals(0.0, north.x, millimetre)
    assertTrue(north.y > 0)
    assertTrue(east.x > 0)
    assertEquals(0.0, east.y, millimetre)
  }

  // Breaks if the cosine of the latitude is dropped, or degrees are taken for radians: a thousandth
  // of a degree is 111.19 m of latitude anywhere, and 76.52 m of longitude at Lausanne.
  @Test
  fun aThousandthOfADegreeHasTheRightLength() {
    val north = toLocal(lausanne, GeoPosition(lausanne.latitude + 0.001, lausanne.longitude))
    val east = toLocal(lausanne, GeoPosition(lausanne.latitude, lausanne.longitude + 0.001))

    assertEquals(111.19, north.y, 0.01)
    assertEquals(76.52, east.x, 0.01)
  }

  // Breaks if toGeo is not the exact inverse of toLocal, for instance a cosine taken at another
  // latitude in one direction only. Checked up to 5 km from the origin, in the four quadrants.
  @Test
  fun aRoundTripGivesBackTheSamePoint() {
    val origins = listOf(lausanne, GeoPosition(0.0, 0.0), GeoPosition(-33.86, 151.21))
    val points = listOf(at(1.0, 1.0), at(-250.0, 80.0), at(3000.0, -4000.0), at(-3500.0, -3500.0))

    for (origin in origins) {
      for (point in points) {
        assertPosition(point, toLocal(origin, toGeo(origin, point)), delta = 0.01)
      }
    }
  }

  // Breaks if a result that is not on Earth is returned instead of being refused. Next to a pole
  // one metre east is more than a full turn of longitude.
  @Test
  fun toGeoRefusesAResultThatIsNotOnEarth() {
    assertThrows(IllegalArgumentException::class.java) {
      toGeo(GeoPosition(90.0, 0.0), at(1.0, 0.0))
    }
  }

  // ---------------------------------------------------------------------------------------------
  // distanceMeters
  // ---------------------------------------------------------------------------------------------

  // Breaks if the local distance is not the straight line: a 3, 4, 5 triangle.
  @Test
  fun theLocalDistanceIsTheStraightLine() {
    assertEquals(5.0, distanceMeters(at(1.0, 1.0), at(4.0, 5.0)), 1e-12)
    assertEquals(0.0, distanceMeters(at(2.0, 2.0), at(2.0, 2.0)), 0.0)
  }

  // Breaks if the distance on Earth uses the wrong radius or mixes degrees and radians: one degree
  // of latitude is 111.19 km.
  @Test
  fun theDistanceOnEarthOfOneDegreeOfLatitude() {
    val distance = distanceMeters(GeoPosition(46.0, 6.0), GeoPosition(47.0, 6.0))

    assertEquals(111_194.9, distance, 0.1)
    assertEquals(0.0, distanceMeters(lausanne, lausanne), 0.0)
  }

  // Breaks if the two distances disagree at the scale of a site, where a boundary is measured with
  // one and drawn with the other.
  @Test
  fun bothDistancesAgreeAtTheScaleOfASite() {
    val far = toGeo(lausanne, at(600.0, 800.0))

    assertEquals(1000.0, distanceMeters(lausanne, far), 0.1)
  }

  // ---------------------------------------------------------------------------------------------
  // normalizeDegrees
  // ---------------------------------------------------------------------------------------------

  // Breaks if a negative angle stays negative, or a full turn is not brought back to 0.
  @Test
  fun degreesAreBroughtBackWithinOneTurn() {
    assertEquals(0.0, normalizeDegrees(0.0), 0.0)
    assertEquals(0.0, normalizeDegrees(360.0), 0.0)
    assertEquals(90.0, normalizeDegrees(450.0), 0.0)
    assertEquals(270.0, normalizeDegrees(-90.0), 0.0)
    assertEquals(359.5, normalizeDegrees(-720.5), 1e-9)
  }

  // ---------------------------------------------------------------------------------------------
  // polygonArea and polygonCentroid
  // ---------------------------------------------------------------------------------------------

  // Breaks if the area depends on the direction the corners are listed in.
  @Test
  fun theAreaIsTheSameInBothDirections() {
    assertEquals(100.0, polygonArea(square), 1e-9)
    assertEquals(100.0, polygonArea(square.reversed()), 1e-9)
  }

  // Breaks if the shoelace sum forgets the edge that closes the polygon: a 3, 4, 5 triangle.
  @Test
  fun theAreaOfATriangle() {
    assertEquals(6.0, polygonArea(listOf(at(0.0, 0.0), at(4.0, 0.0), at(0.0, 3.0))), 1e-9)
  }

  // Breaks if something that is not a surface gets an area.
  @Test
  fun whatIsNotASurfaceHasNoArea() {
    assertEquals(0.0, polygonArea(emptyList()), 0.0)
    assertEquals(0.0, polygonArea(listOf(at(0.0, 0.0), at(4.0, 0.0))), 0.0)
    assertEquals(0.0, polygonArea(listOf(at(0.0, 0.0), at(2.0, 2.0), at(4.0, 4.0))), 1e-9)
  }

  // Breaks if the centroid depends on the direction, which flips the sign of the area.
  @Test
  fun theCentroidOfASquareIsItsCentre() {
    assertPosition(at(5.0, 5.0), polygonCentroid(square))
    assertPosition(at(5.0, 5.0), polygonCentroid(square.reversed()))
  }

  // Breaks if the centroid is the average of the corners: on an L shape that average is (5, 5), the
  // centre of mass is (4.167, 4.167).
  @Test
  fun theCentroidIsTheCentreOfMassNotTheAverageOfTheCorners() {
    val lShape =
        listOf(
            at(0.0, 0.0),
            at(10.0, 0.0),
            at(10.0, 5.0),
            at(5.0, 5.0),
            at(5.0, 10.0),
            at(0.0, 10.0),
        )

    assertPosition(at(25.0 / 6, 25.0 / 6), polygonCentroid(lShape))
  }

  // Breaks if a polygon without area divides by zero and returns NaN.
  @Test
  fun theCentroidWithoutAreaIsTheAverageOfTheCorners() {
    assertPosition(at(3.0, 4.0), polygonCentroid(listOf(at(3.0, 4.0))))
    assertPosition(at(2.0, 0.0), polygonCentroid(listOf(at(0.0, 0.0), at(4.0, 0.0))))
    assertPosition(
        at(2.0, 2.0),
        polygonCentroid(listOf(at(0.0, 0.0), at(2.0, 2.0), at(4.0, 4.0))),
    )
  }

  // Breaks if an empty polygon returns a made up centre.
  @Test
  fun anEmptyPolygonHasNoCentroid() {
    assertInvalid("An empty polygon has no centroid.") { polygonCentroid(emptyList()) }
  }

  // ---------------------------------------------------------------------------------------------
  // polygonContains
  // ---------------------------------------------------------------------------------------------

  // Breaks if the ray casting counts wrong, in either direction of the corners.
  @Test
  fun aPointInsideIsContainedAndAPointOutsideIsNot() {
    for (polygon in listOf(square, square.reversed())) {
      assertTrue(polygonContains(polygon, at(5.0, 5.0)))
      assertFalse(polygonContains(polygon, at(15.0, 5.0)))
      assertFalse(polygonContains(polygon, at(-1.0, 5.0)))
      assertFalse(polygonContains(polygon, at(5.0, 10.5)))
    }
  }

  // Breaks if the edges are left out: someone standing on the fence is on the site.
  @Test
  fun aPointOnAnEdgeOrACornerIsContained() {
    assertTrue(polygonContains(square, at(5.0, 0.0)))
    assertTrue(polygonContains(square, at(10.0, 7.5)))
    assertTrue(polygonContains(square, at(0.0, 0.0)))
    assertTrue(polygonContains(square, at(10.0, 10.0)))
  }

  // Breaks if a ray passing exactly through a corner is counted twice or not at all: (5, 5) is in
  // the notch of the L, level with its inner corner.
  @Test
  fun aPointLevelWithACornerIsCountedOnce() {
    val lShape =
        listOf(
            at(0.0, 0.0),
            at(10.0, 0.0),
            at(10.0, 5.0),
            at(5.0, 5.0),
            at(5.0, 10.0),
            at(0.0, 10.0),
        )

    assertTrue(polygonContains(lShape, at(2.0, 5.0)))
    assertFalse(polygonContains(lShape, at(7.0, 7.0)))
    assertFalse(polygonContains(lShape, at(-2.0, 5.0)))
  }

  // Breaks if a line is taken for a surface.
  @Test
  fun fewerThanThreeCornersContainNothing() {
    assertFalse(polygonContains(emptyList(), at(0.0, 0.0)))
    assertFalse(polygonContains(listOf(at(0.0, 0.0), at(4.0, 0.0)), at(2.0, 0.0)))
  }

  // ---------------------------------------------------------------------------------------------
  // isSimplePolygon
  // ---------------------------------------------------------------------------------------------

  // Breaks if a plain outline is refused, a triangle and a concave shape included.
  @Test
  fun plainOutlinesAreSimple() {
    val lShape =
        listOf(
            at(0.0, 0.0),
            at(10.0, 0.0),
            at(10.0, 5.0),
            at(5.0, 5.0),
            at(5.0, 10.0),
            at(0.0, 10.0),
        )

    assertTrue(isSimplePolygon(square))
    assertTrue(isSimplePolygon(square.reversed()))
    assertTrue(isSimplePolygon(listOf(at(0.0, 0.0), at(4.0, 0.0), at(0.0, 3.0))))
    assertTrue(isSimplePolygon(lShape))
  }

  // Breaks if two edges that cross are accepted: a bow tie.
  @Test
  fun aBowTieIsNotSimple() {
    assertFalse(isSimplePolygon(listOf(at(0.0, 0.0), at(10.0, 10.0), at(10.0, 0.0), at(0.0, 10.0))))
  }

  // Breaks if an edge that only touches another one, without crossing it, is accepted.
  @Test
  fun anOutlineThatTouchesItselfIsNotSimple() {
    val touching = listOf(at(0.0, 0.0), at(10.0, 0.0), at(10.0, 10.0), at(5.0, 0.0), at(0.0, 10.0))

    assertFalse(isSimplePolygon(touching))
  }

  // Breaks if a walk that comes back on its own steps is accepted.
  @Test
  fun anOutlineThatFoldsBackIsNotSimple() {
    assertFalse(isSimplePolygon(listOf(at(0.0, 0.0), at(10.0, 0.0), at(5.0, 0.0), at(5.0, 5.0))))
    assertFalse(isSimplePolygon(listOf(at(0.0, 0.0), at(2.0, 2.0), at(4.0, 4.0))))
  }

  // Breaks if a corner recorded twice in a row is accepted.
  @Test
  fun twoConsecutiveCornersAtTheSamePlaceAreNotSimple() {
    assertFalse(isSimplePolygon(listOf(at(0.0, 0.0), at(10.0, 0.0), at(10.0, 0.0), at(0.0, 10.0))))
    assertFalse(isSimplePolygon(listOf(at(0.0, 0.0), at(10.0, 0.0), at(0.0, 10.0), at(0.0, 0.0))))
  }

  // Breaks if a line or a point is accepted as an outline.
  @Test
  fun fewerThanThreeCornersAreNotSimple() {
    assertFalse(isSimplePolygon(emptyList()))
    assertFalse(isSimplePolygon(listOf(at(0.0, 0.0), at(4.0, 0.0))))
  }

  // Breaks if a corner in the middle of a straight side is refused: a recorded walk has many.
  @Test
  fun aCornerOnAStraightSideIsSimple() {
    assertTrue(
        isSimplePolygon(
            listOf(at(0.0, 0.0), at(5.0, 0.0), at(10.0, 0.0), at(10.0, 10.0), at(0.0, 10.0))
        )
    )
  }

  // ---------------------------------------------------------------------------------------------
  // rectangleCorners
  // ---------------------------------------------------------------------------------------------

  // Breaks if the width and the depth are swapped, the centre is ignored or the order changes.
  @Test
  fun withoutRotationTheCornersStartNorthWestAndTurnClockwise() {
    val corners =
        rectangleCorners(at(100.0, 50.0), width = 10.0, depth = 4.0, rotationDegrees = 0.0)

    assertEquals(4, corners.size)
    assertPosition(at(95.0, 52.0), corners[0])
    assertPosition(at(105.0, 52.0), corners[1])
    assertPosition(at(105.0, 48.0), corners[2])
    assertPosition(at(95.0, 48.0), corners[3])
  }

  // Breaks if the rotation goes counterclockwise: after a quarter turn clockwise the north west
  // corner is to the north east, and the wide side runs north to south.
  @Test
  fun aQuarterTurnGoesClockwise() {
    val corners = rectangleCorners(at(0.0, 0.0), width = 10.0, depth = 4.0, rotationDegrees = 90.0)

    assertPosition(at(2.0, 5.0), corners[0])
    assertPosition(at(2.0, -5.0), corners[1])
    assertPosition(at(-2.0, -5.0), corners[2])
    assertPosition(at(-2.0, 5.0), corners[3])
  }

  // Breaks if a full turn or a negative angle is not the same rectangle as its normalized angle.
  @Test
  fun aFullTurnChangesNothingAndANegativeAngleTurnsTheOtherWay() {
    val still = rectangleCorners(at(3.0, 7.0), 10.0, 4.0, 0.0)
    val fullTurn = rectangleCorners(at(3.0, 7.0), 10.0, 4.0, 360.0)
    val back = rectangleCorners(at(3.0, 7.0), 10.0, 4.0, -90.0)
    val threeQuarters = rectangleCorners(at(3.0, 7.0), 10.0, 4.0, 270.0)

    for (i in 0..3) {
      assertPosition(still[i], fullTurn[i])
      assertPosition(threeQuarters[i], back[i])
    }
  }

  // Breaks if the rotation stretches the rectangle: turned by 30 degrees a 10 by 8 m stage still
  // covers 80 square metres around the same centre, with sides of 10 and 8 m.
  @Test
  fun aRotationKeepsTheSizeAndTheCentre() {
    val corners =
        rectangleCorners(at(20.0, -5.0), width = 10.0, depth = 8.0, rotationDegrees = 30.0)

    assertEquals(80.0, polygonArea(corners), 1e-9)
    assertPosition(at(20.0, -5.0), polygonCentroid(corners))
    assertEquals(10.0, distanceMeters(corners[0], corners[1]), 1e-9)
    assertEquals(8.0, distanceMeters(corners[1], corners[2]), 1e-9)
    // The north east corner of a rectangle turned clockwise moves south.
    assertEquals(20.0 + 5.0 * sqrt(3.0) / 2 + 4.0 * 0.5, corners[1].x, 1e-9)
    assertEquals(-5.0 - 5.0 * 0.5 + 4.0 * sqrt(3.0) / 2, corners[1].y, 1e-9)
  }

  // Breaks if a rectangle without size or with an undefined rotation is drawn.
  @Test
  fun aRectangleWithoutSizeIsRefused() {
    val centre = at(0.0, 0.0)
    assertInvalid("The width must be finite and positive.") {
      rectangleCorners(centre, 0.0, 4.0, 0.0)
    }
    assertInvalid("The width must be finite and positive.") {
      rectangleCorners(centre, Double.NaN, 4.0, 0.0)
    }
    assertInvalid("The depth must be finite and positive.") {
      rectangleCorners(centre, 10.0, -4.0, 0.0)
    }
    assertInvalid("The depth must be finite and positive.") {
      rectangleCorners(centre, 10.0, Double.POSITIVE_INFINITY, 0.0)
    }
    assertInvalid("The rotation must be finite.") {
      rectangleCorners(centre, 10.0, 4.0, Double.NaN)
    }
  }

  // ---------------------------------------------------------------------------------------------
  // simplifyPath
  // ---------------------------------------------------------------------------------------------

  // Breaks if points that add no shape are kept: a straight walk is its two ends.
  @Test
  fun pointsOnAStraightLineAreRemoved() {
    val walk = listOf(at(0.0, 0.0), at(1.0, 0.02), at(2.0, -0.03), at(3.0, 0.01), at(4.0, 0.0))

    assertEquals(listOf(at(0.0, 0.0), at(4.0, 0.0)), simplifyPath(walk, toleranceMeters = 0.5))
  }

  // Breaks if a real corner is removed, or the order of the points changes.
  @Test
  fun aCornerIsKept() {
    val walk = listOf(at(0.0, 0.0), at(5.0, 0.1), at(10.0, 0.0), at(10.1, 5.0), at(10.0, 10.0))

    assertEquals(
        listOf(at(0.0, 0.0), at(10.0, 0.0), at(10.0, 10.0)),
        simplifyPath(walk, toleranceMeters = 0.5),
    )
  }

  // Breaks if a point exactly at the tolerance is kept, or one just beyond it is removed.
  @Test
  fun thePointAtTheToleranceIsRemovedAndTheOneBeyondIsKept() {
    val atTolerance = listOf(at(0.0, 0.0), at(5.0, 1.0), at(10.0, 0.0))
    val beyond = listOf(at(0.0, 0.0), at(5.0, 1.01), at(10.0, 0.0))

    assertEquals(2, simplifyPath(atTolerance, toleranceMeters = 1.0).size)
    assertEquals(beyond, simplifyPath(beyond, toleranceMeters = 1.0))
  }

  // Breaks if a tolerance of zero removes anything but the points exactly on the line.
  @Test
  fun aToleranceOfZeroOnlyRemovesWhatIsExactlyOnTheLine() {
    val walk = listOf(at(0.0, 0.0), at(5.0, 0.0), at(10.0, 0.0), at(10.0, 0.5), at(20.0, 0.0))

    assertEquals(
        listOf(at(0.0, 0.0), at(10.0, 0.0), at(10.0, 0.5), at(20.0, 0.0)),
        simplifyPath(walk, toleranceMeters = 0.0),
    )
  }

  // Breaks if a walk that comes back to its start collapses: its two ends are at the same place, so
  // the distance is taken to that point.
  @Test
  fun aWalkBackToItsStartKeepsItsShape() {
    val loop = listOf(at(0.0, 0.0), at(10.0, 0.0), at(10.0, 10.0), at(0.0, 10.0), at(0.0, 0.0))

    assertEquals(loop, simplifyPath(loop, toleranceMeters = 1.0))
  }

  // Breaks if a path too short to simplify is changed.
  @Test
  fun aPathOfTwoPointsOrFewerIsReturnedAsIs() {
    val two = listOf(at(0.0, 0.0), at(4.0, 0.0))

    assertSame(two, simplifyPath(two, toleranceMeters = 100.0))
    assertEquals(emptyList<LocalPosition>(), simplifyPath(emptyList(), toleranceMeters = 1.0))
  }

  // Breaks if a long recording overflows the stack: ten thousand points on a zigzag are all kept.
  @Test
  fun aLongPathIsSimplifiedWithoutOverflow() {
    val zigzag = List(10_000) { at(it.toDouble(), if (it % 2 == 0) 0.0 else 5.0) }

    assertEquals(zigzag, simplifyPath(zigzag, toleranceMeters = 1.0))
  }

  // Breaks if a negative or undefined tolerance is accepted.
  @Test
  fun aNegativeToleranceIsRefused() {
    assertInvalid("The tolerance cannot be negative.") { simplifyPath(square, -0.1) }
    assertInvalid("The tolerance cannot be negative.") { simplifyPath(square, Double.NaN) }
  }

  // ---------------------------------------------------------------------------------------------
  // filterGpsSamples
  // ---------------------------------------------------------------------------------------------

  private fun sample(x: Double, y: Double, accuracy: Double) =
      GpsSample(toGeo(lausanne, at(x, y)), accuracy)

  // Breaks if an imprecise sample is kept, or one exactly at the limit is dropped.
  @Test
  fun impreciseSamplesAreDropped() {
    val good = sample(0.0, 0.0, 4.0)
    val atLimit = sample(10.0, 0.0, 8.0)
    val bad = sample(20.0, 0.0, 8.1)

    assertEquals(
        listOf(good, atLimit),
        filterGpsSamples(
            listOf(good, atLimit, bad),
            maxAccuracyMeters = 8.0,
            minDistanceMeters = 0.0,
        ),
    )
  }

  // Breaks if the distance is measured from the previous sample instead of the last one kept:
  // three samples one metre apart never leave each other, yet the third is two metres from the
  // first.
  @Test
  fun aSampleTooCloseToTheLastKeptOneIsDropped() {
    val first = sample(0.0, 0.0, 3.0)
    val tooClose = sample(1.0, 0.0, 3.0)
    val farEnough = sample(2.0, 0.0, 3.0)
    val alsoTooClose = sample(3.0, 0.0, 3.0)

    assertEquals(
        listOf(first, farEnough),
        filterGpsSamples(
            listOf(first, tooClose, farEnough, alsoTooClose),
            maxAccuracyMeters = 10.0,
            minDistanceMeters = 1.5,
        ),
    )
  }

  // Breaks if an imprecise sample counts as the last one kept and hides the good one after it.
  @Test
  fun aDroppedSampleDoesNotCountAsThePreviousOne() {
    val first = sample(0.0, 0.0, 3.0)
    val imprecise = sample(5.0, 0.0, 30.0)
    val next = sample(5.5, 0.0, 3.0)

    assertEquals(
        listOf(first, next),
        filterGpsSamples(
            listOf(first, imprecise, next),
            maxAccuracyMeters = 10.0,
            minDistanceMeters = 2.0,
        ),
    )
  }

  // Breaks if the first good sample needs a previous one, or an empty recording fails.
  @Test
  fun theFirstGoodSampleIsAlwaysKept() {
    val only = sample(0.0, 0.0, 3.0)

    assertEquals(listOf(only), filterGpsSamples(listOf(only), 10.0, 100.0))
    assertEquals(emptyList<GpsSample>(), filterGpsSamples(emptyList(), 10.0, 1.0))
  }

  // Breaks if a negative or undefined limit is accepted.
  @Test
  fun negativeLimitsAreRefused() {
    assertInvalid("The maximum accuracy cannot be negative.") {
      filterGpsSamples(emptyList(), -1.0, 1.0)
    }
    assertInvalid("The maximum accuracy cannot be negative.") {
      filterGpsSamples(emptyList(), Double.NaN, 1.0)
    }
    assertInvalid("The minimum distance cannot be negative.") {
      filterGpsSamples(emptyList(), 1.0, -1.0)
    }
    assertInvalid("The minimum distance cannot be negative.") {
      filterGpsSamples(emptyList(), 1.0, Double.NaN)
    }
  }
}
