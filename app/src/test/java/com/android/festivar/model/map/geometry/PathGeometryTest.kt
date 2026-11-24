// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.model.map.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PathGeometryTest {

  private val lausanne = GeoPosition(46.5191, 6.5668)

  private fun at(x: Double, y: Double) = LocalPosition(x, y)

  /** A 10 by 10 m square with its south west corner on the origin, counterclockwise. */
  private val square = listOf(at(0.0, 0.0), at(10.0, 0.0), at(10.0, 10.0), at(0.0, 10.0))

  private fun assertInvalid(expectedMessage: String, call: () -> Any) {
    val exception = assertThrows(IllegalArgumentException::class.java) { call() }
    assertEquals(expectedMessage, exception.message)
  }

  // ---------------------------------------------------------------------------------------------
  // simplifyPath
  // ---------------------------------------------------------------------------------------------

  /** Breaks if points that add no shape are kept: a straight walk is its two ends. */
  @Test
  fun pointsOnAStraightLineAreRemoved() {
    val walk = listOf(at(0.0, 0.0), at(1.0, 0.02), at(2.0, -0.03), at(3.0, 0.01), at(4.0, 0.0))

    assertEquals(listOf(at(0.0, 0.0), at(4.0, 0.0)), simplifyPath(walk, toleranceMeters = 0.5))
  }

  /** Breaks if a real corner is removed, or the order of the points changes. */
  @Test
  fun aCornerIsKept() {
    val walk = listOf(at(0.0, 0.0), at(5.0, 0.1), at(10.0, 0.0), at(10.1, 5.0), at(10.0, 10.0))

    assertEquals(
        listOf(at(0.0, 0.0), at(10.0, 0.0), at(10.0, 10.0)),
        simplifyPath(walk, toleranceMeters = 0.5),
    )
  }

  /** Breaks if a point exactly at the tolerance is kept, or one just beyond it is removed. */
  @Test
  fun thePointAtTheToleranceIsRemovedAndTheOneBeyondIsKept() {
    val atTolerance = listOf(at(0.0, 0.0), at(5.0, 1.0), at(10.0, 0.0))
    val beyond = listOf(at(0.0, 0.0), at(5.0, 1.01), at(10.0, 0.0))

    assertEquals(2, simplifyPath(atTolerance, toleranceMeters = 1.0).size)
    assertEquals(beyond, simplifyPath(beyond, toleranceMeters = 1.0))
  }

  /** Breaks if a tolerance of zero removes anything but the points exactly on the line. */
  @Test
  fun aToleranceOfZeroOnlyRemovesWhatIsExactlyOnTheLine() {
    val walk = listOf(at(0.0, 0.0), at(5.0, 0.0), at(10.0, 0.0), at(10.0, 0.5), at(20.0, 0.0))

    assertEquals(
        listOf(at(0.0, 0.0), at(10.0, 0.0), at(10.0, 0.5), at(20.0, 0.0)),
        simplifyPath(walk, toleranceMeters = 0.0),
    )
  }

  /**
   * Breaks if a walk that comes back to its start collapses: its two ends are at the same place, so
   * the distance is taken to that point.
   */
  @Test
  fun aWalkBackToItsStartKeepsItsShape() {
    val loop = listOf(at(0.0, 0.0), at(10.0, 0.0), at(10.0, 10.0), at(0.0, 10.0), at(0.0, 0.0))

    assertEquals(loop, simplifyPath(loop, toleranceMeters = 1.0))
  }

  /** Breaks if a path too short to simplify is changed. */
  @Test
  fun aPathOfTwoPointsOrFewerIsReturnedAsIs() {
    val two = listOf(at(0.0, 0.0), at(4.0, 0.0))

    assertSame(two, simplifyPath(two, toleranceMeters = 100.0))
    assertEquals(emptyList<LocalPosition>(), simplifyPath(emptyList(), toleranceMeters = 1.0))
  }

  /**
   * Breaks if a long recording overflows the stack: ten thousand points on a zigzag are all kept.
   */
  @Test
  fun aLongPathIsSimplifiedWithoutOverflow() {
    val zigzag = List(10_000) { at(it.toDouble(), if (it % 2 == 0) 0.0 else 5.0) }

    assertEquals(zigzag, simplifyPath(zigzag, toleranceMeters = 1.0))
  }

  /** Breaks if a negative or undefined tolerance is accepted. */
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

  /** Breaks if an imprecise sample is kept, or one exactly at the limit is dropped. */
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

  /**
   * Breaks if the distance is measured from the previous sample instead of the last one kept: three
   * samples one metre apart never leave each other, yet the third is two metres from the first.
   */
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

  /**
   * Breaks if a sample exactly at the minimum distance from the last one kept is dropped (`>=`
   * turned into `>`), or if one a centimetre short of it is kept.
   */
  @Test
  fun aSampleExactlyAtTheMinimumDistanceIsKept() {
    val first = sample(0.0, 0.0, 3.0)
    val next = sample(2.0, 0.0, 3.0)
    val apart = distanceMeters(first.position, next.position)

    assertEquals(
        listOf(first, next),
        filterGpsSamples(listOf(first, next), maxAccuracyMeters = 10.0, minDistanceMeters = apart),
    )
    assertEquals(
        listOf(first),
        filterGpsSamples(
            listOf(first, next),
            maxAccuracyMeters = 10.0,
            minDistanceMeters = apart + 0.01,
        ),
    )
  }

  /** Breaks if an imprecise sample counts as the last one kept and hides the good one after it. */
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

  /** Breaks if the first good sample needs a previous one, or an empty recording fails. */
  @Test
  fun theFirstGoodSampleIsAlwaysKept() {
    val only = sample(0.0, 0.0, 3.0)

    assertEquals(listOf(only), filterGpsSamples(listOf(only), 10.0, 100.0))
    assertEquals(emptyList<GpsSample>(), filterGpsSamples(emptyList(), 10.0, 1.0))
  }

  /** Breaks if a negative or undefined limit is accepted. */
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
