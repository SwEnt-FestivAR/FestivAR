// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task

import com.android.festivar.model.event.Venue
import com.android.festivar.model.structure.Structure
import com.android.festivar.model.structure.StructureKind
import com.android.festivar.model.task.TaskSpot
import com.android.festivar.ui.map.VenueGeometry
import org.junit.Assert.assertEquals
import org.junit.Test

/** Tests of [spotForTap], the snap rule of the spot picker, without any map. */
class TaskSpotFieldTest {
  private val venue = Venue(latitude = 46.5170, longitude = 6.5990, bearing = 30.0)
  private val marquee =
      Structure("s2", "e1", StructureKind.MARQUEE, "Marquee A", -18.0, 0.0, 12.0, 8.0)

  // The spot a tap at plan metres (x, y) of [on] picks, going through the earth like a real tap.
  private fun tapAt(x: Double, y: Double, on: Venue = venue): TaskSpot {
    val point = VenueGeometry.latLngFrom(on.latitude, on.longitude, on.bearing, x, y)
    return spotForTap(on, listOf(marquee), point.latitude, point.longitude)
  }

  // Breaks if a tap on a footprint stays a free point, or the bearing is ignored, so the pin lands
  // beside the structure the organizer tapped.
  @Test
  fun aTapInsideAStructureSnapsToIt() {
    assertEquals(TaskSpot.onStructure("s2"), tapAt(-12.2, 3.9))
  }

  // Breaks if a free point is not snapped to half a metre.
  @Test
  fun aTapOutsideGivesASnappedPoint() {
    assertEquals(TaskSpot.at(2.5, 7.0), tapAt(2.3, 7.1))
  }

  // Breaks if the plan rotation is applied the wrong way round at the centre, or if snapping
  // leaves a negative zero.
  @Test
  fun aTapAtTheVenueCentreWithBearing90IsTheOrigin() {
    val turned = venue.copy(bearing = 90.0)

    assertEquals(
        TaskSpot.at(0.0, 0.0),
        spotForTap(turned, emptyList(), turned.latitude, turned.longitude),
    )
  }

  // Breaks if the footprint test excludes its edges, so a tap on the outline becomes a point.
  @Test
  fun theFootprintBoundaryIsInclusive() {
    assertEquals(TaskSpot.onStructure("s2"), tapAt(-6.0, 8.0))
    assertEquals(TaskSpot.onStructure("s2"), tapAt(-18.0, 0.0))
    assertEquals(TaskSpot.at(-5.5, 8.0), tapAt(-5.5, 8.0))
  }
}
