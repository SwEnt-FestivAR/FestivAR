// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map.geometry

import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PolygonGeometryTest {

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
  // polygonArea and polygonCentroid
  // ---------------------------------------------------------------------------------------------

  /** Breaks if the area depends on the direction the corners are listed in. */
  @Test
  fun theAreaIsTheSameInBothDirections() {
    assertEquals(100.0, polygonArea(square), 1e-9)
    assertEquals(100.0, polygonArea(square.reversed()), 1e-9)
  }

  /** Breaks if the shoelace sum forgets the edge that closes the polygon: a 3, 4, 5 triangle. */
  @Test
  fun theAreaOfATriangle() {
    assertEquals(6.0, polygonArea(listOf(at(0.0, 0.0), at(4.0, 0.0), at(0.0, 3.0))), 1e-9)
  }

  /** Breaks if something that is not a surface gets an area. */
  @Test
  fun whatIsNotASurfaceHasNoArea() {
    assertEquals(0.0, polygonArea(emptyList()), 0.0)
    assertEquals(0.0, polygonArea(listOf(at(0.0, 0.0), at(4.0, 0.0))), 0.0)
    assertEquals(0.0, polygonArea(listOf(at(0.0, 0.0), at(2.0, 2.0), at(4.0, 4.0))), 1e-9)
  }

  /** Breaks if the centroid depends on the direction, which flips the sign of the area. */
  @Test
  fun theCentroidOfASquareIsItsCentre() {
    assertPosition(at(5.0, 5.0), polygonCentroid(square))
    assertPosition(at(5.0, 5.0), polygonCentroid(square.reversed()))
  }

  /**
   * Breaks if the centroid is the average of the corners: on an L shape that average is (5, 5), the
   * centre of mass is (4.167, 4.167).
   */
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

  /** Breaks if a polygon without area divides by zero and returns NaN. */
  @Test
  fun theCentroidWithoutAreaIsTheAverageOfTheCorners() {
    assertPosition(at(3.0, 4.0), polygonCentroid(listOf(at(3.0, 4.0))))
    assertPosition(at(2.0, 0.0), polygonCentroid(listOf(at(0.0, 0.0), at(4.0, 0.0))))
    assertPosition(
        at(2.0, 2.0),
        polygonCentroid(listOf(at(0.0, 0.0), at(2.0, 2.0), at(4.0, 4.0))),
    )
  }

  /**
   * Breaks if "no area" is judged with a fixed threshold: far from the origin rounding leaves a
   * billionth of a square metre on these four corners on one line, and dividing by it sends the
   * centroid off the line, to (1820, 2276).
   */
  @Test
  fun theCentroidWithoutAreaFarFromTheOriginIsTheAverageOfTheCorners() {
    val onOneLine =
        listOf(at(2479.8, 2844.6), at(2603.0, 3317.1), at(2726.2, 3789.6), at(2849.4, 4262.1))

    assertPosition(at(2664.6, 3553.35), polygonCentroid(onOneLine), delta = 1e-6)
  }

  /** Breaks if an empty polygon returns a made up centre. */
  @Test
  fun anEmptyPolygonHasNoCentroid() {
    assertInvalid("An empty polygon has no centroid.") { polygonCentroid(emptyList()) }
  }

  // ---------------------------------------------------------------------------------------------
  // polygonContains
  // ---------------------------------------------------------------------------------------------

  /** Breaks if the ray casting counts wrong, in either direction of the corners. */
  @Test
  fun aPointInsideIsContainedAndAPointOutsideIsNot() {
    for (polygon in listOf(square, square.reversed())) {
      assertTrue(polygonContains(polygon, at(5.0, 5.0)))
      assertFalse(polygonContains(polygon, at(15.0, 5.0)))
      assertFalse(polygonContains(polygon, at(-1.0, 5.0)))
      assertFalse(polygonContains(polygon, at(5.0, 10.5)))
    }
  }

  /** Breaks if the edges are left out: someone standing on the fence is on the site. */
  @Test
  fun aPointOnAnEdgeOrACornerIsContained() {
    assertTrue(polygonContains(square, at(5.0, 0.0)))
    assertTrue(polygonContains(square, at(10.0, 7.5)))
    assertTrue(polygonContains(square, at(0.0, 0.0)))
    assertTrue(polygonContains(square, at(10.0, 10.0)))
  }

  /**
   * Breaks if a ray passing exactly through a corner is counted twice or not at all: (5, 5) is in
   * the notch of the L, level with its inner corner.
   */
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

  /** Breaks if a line is taken for a surface. */
  @Test
  fun fewerThanThreeCornersContainNothing() {
    assertFalse(polygonContains(emptyList(), at(0.0, 0.0)))
    assertFalse(polygonContains(listOf(at(0.0, 0.0), at(4.0, 0.0)), at(2.0, 0.0)))
  }

  // ---------------------------------------------------------------------------------------------
  // isSimplePolygon
  // ---------------------------------------------------------------------------------------------

  /** Breaks if a plain outline is refused, a triangle and a concave shape included. */
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

  /** Breaks if two edges that cross are accepted: a bow tie. */
  @Test
  fun aBowTieIsNotSimple() {
    assertFalse(isSimplePolygon(listOf(at(0.0, 0.0), at(10.0, 10.0), at(10.0, 0.0), at(0.0, 10.0))))
  }

  /** Breaks if an edge that only touches another one, without crossing it, is accepted. */
  @Test
  fun anOutlineThatTouchesItselfIsNotSimple() {
    val touching = listOf(at(0.0, 0.0), at(10.0, 0.0), at(10.0, 10.0), at(5.0, 0.0), at(0.0, 10.0))

    assertFalse(isSimplePolygon(touching))
  }

  /** Breaks if a walk that comes back on its own steps is accepted. */
  @Test
  fun anOutlineThatFoldsBackIsNotSimple() {
    assertFalse(isSimplePolygon(listOf(at(0.0, 0.0), at(10.0, 0.0), at(5.0, 0.0), at(5.0, 5.0))))
    assertFalse(isSimplePolygon(listOf(at(0.0, 0.0), at(2.0, 2.0), at(4.0, 4.0))))
  }

  /**
   * Breaks if "on one line" is judged with a fixed threshold: a few kilometres from the origin
   * rounding leaves about a billionth of a square metre on three corners that are on one line.
   */
  @Test
  fun anOutlineThatFoldsBackFarFromTheOriginIsNotSimple() {
    val onOneLine = listOf(at(2587.3, 2365.7), at(4294.1, 3886.9), at(3440.7, 3126.3))

    assertFalse(isSimplePolygon(onOneLine))
  }

  /** Breaks if a thin but real triangle far from the origin is taken for a line. */
  @Test
  fun aThinTriangleFarFromTheOriginIsSimple() {
    assertTrue(isSimplePolygon(listOf(at(2587.3, 2365.7), at(4294.1, 3886.9), at(3440.7, 3126.4))))
  }

  /** Breaks if a corner recorded twice in a row is accepted. */
  @Test
  fun twoConsecutiveCornersAtTheSamePlaceAreNotSimple() {
    assertFalse(isSimplePolygon(listOf(at(0.0, 0.0), at(10.0, 0.0), at(10.0, 0.0), at(0.0, 10.0))))
    assertFalse(isSimplePolygon(listOf(at(0.0, 0.0), at(10.0, 0.0), at(0.0, 10.0), at(0.0, 0.0))))
  }

  /** Breaks if a line or a point is accepted as an outline. */
  @Test
  fun fewerThanThreeCornersAreNotSimple() {
    assertFalse(isSimplePolygon(emptyList()))
    assertFalse(isSimplePolygon(listOf(at(0.0, 0.0), at(4.0, 0.0))))
  }

  /** Breaks if a corner in the middle of a straight side is refused: a recorded walk has many. */
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

  /** Breaks if the width and the depth are swapped, the centre is ignored or the order changes. */
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

  /**
   * Breaks if the rotation goes counterclockwise: after a quarter turn clockwise the north west
   * corner is to the north east, and the wide side runs north to south.
   */
  @Test
  fun aQuarterTurnGoesClockwise() {
    val corners = rectangleCorners(at(0.0, 0.0), width = 10.0, depth = 4.0, rotationDegrees = 90.0)

    assertPosition(at(2.0, 5.0), corners[0])
    assertPosition(at(2.0, -5.0), corners[1])
    assertPosition(at(-2.0, -5.0), corners[2])
    assertPosition(at(-2.0, 5.0), corners[3])
  }

  /**
   * Breaks if a full turn or a negative angle is not the same rectangle as its normalized angle.
   */
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

  /**
   * Breaks if the rotation stretches the rectangle: turned by 30 degrees a 10 by 8 m stage still
   * covers 80 square metres around the same centre, with sides of 10 and 8 m.
   */
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

  /** Breaks if a rectangle without size or with an undefined rotation is drawn. */
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
}
