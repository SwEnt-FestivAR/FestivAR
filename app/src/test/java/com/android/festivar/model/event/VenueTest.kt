// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VenueTest {

  /** The bearing is optional and defaults to north. */
  @Test
  fun bearing_defaultsToZero() {
    assertEquals(0.0, Venue(46.5, 6.6).bearing, 0.0)
  }

  /** The poles are valid latitudes, anything past them is refused. */
  @Test
  fun latitude_acceptsThePoles_refusesBeyond() {
    Venue(90.0, 0.0)
    Venue(-90.0, 0.0)

    assertThrows(IllegalArgumentException::class.java) { Venue(90.1, 0.0) }
    assertThrows(IllegalArgumentException::class.java) { Venue(-90.1, 0.0) }
  }

  /** The antimeridian is a valid longitude on both sides, anything past it is refused. */
  @Test
  fun longitude_acceptsTheAntimeridian_refusesBeyond() {
    Venue(0.0, -180.0)
    Venue(0.0, 180.0)

    assertThrows(IllegalArgumentException::class.java) { Venue(0.0, -180.1) }
    assertThrows(IllegalArgumentException::class.java) { Venue(0.0, 180.1) }
  }

  /** The bearing covers 0 up to, but not including, a full turn. */
  @Test
  fun bearing_acceptsZeroUpToBelow360_refusesOutside() {
    Venue(0.0, 0.0, 0.0)
    Venue(0.0, 0.0, 359.9)

    assertThrows(IllegalArgumentException::class.java) { Venue(0.0, 0.0, 360.0) }
    assertThrows(IllegalArgumentException::class.java) { Venue(0.0, 0.0, -1.0) }
  }

  /** Not-a-number coordinates would poison every distance computed from the centre. */
  @Test
  fun nan_isRefused() {
    assertThrows(IllegalArgumentException::class.java) { Venue(Double.NaN, 0.0) }
    assertThrows(IllegalArgumentException::class.java) { Venue(0.0, Double.NaN) }
    assertThrows(IllegalArgumentException::class.java) { Venue(0.0, 0.0, Double.NaN) }
  }
}
