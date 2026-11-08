// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.Venue
import com.android.festivar.model.structure.Structure
import com.android.festivar.model.structure.StructureKind
import com.android.festivar.model.task.TaskSpot
import com.android.festivar.ui.map.VenueGeometry
import com.android.festivar.ui.task.create.CreateTaskScreenTestTags
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the spot field of the task forms on a device. The map is swapped for empty content that
 * keeps the tap lambda, so a test taps the map by calling it; one test keeps the Google map.
 */
@RunWith(AndroidJUnit4::class)
class TaskSpotFieldInstrumentedTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val venue = Venue(latitude = 46.5170, longitude = 6.5990)
  private val marquee =
      Structure("s2", "e1", StructureKind.MARQUEE, "Marquee A", -18.0, 0.0, 12.0, 8.0)

  /** Every spot the field reported, in order; a null is a "No spot". */
  private val changes = mutableListOf<TaskSpot?>()

  /** The tap lambda the picker gave its map, once the sheet is open. */
  private var tap: ((Double, Double) -> Unit)? = null

  private fun show(spot: TaskSpot? = null, venue: Venue? = this.venue, realMap: Boolean = false) {
    composeTestRule.setContent {
      AppTheme {
        if (realMap) {
          TaskSpotField(spot, venue, listOf(marquee), onSpotChange = { changes.add(it) })
        } else {
          TaskSpotField(
              spot,
              venue,
              listOf(marquee),
              onSpotChange = { changes.add(it) },
              mapContent = { onTap -> tap = onTap },
          )
        }
      }
    }
  }

  private fun openMenu() =
      composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.SPOT_DROPDOWN).performClick()

  private fun openSheet() {
    openMenu()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.SPOT_PICK_POINT).performClick()
    composeTestRule.waitForIdle()
  }

  // Taps the swapped map at plan metres (x, y).
  private fun tapMapAt(x: Double, y: Double) {
    val point = VenueGeometry.latLngFrom(venue.latitude, venue.longitude, venue.bearing, x, y)
    composeTestRule.runOnIdle { tap!!(point.latitude, point.longitude) }
  }

  // Breaks if a task without a spot shows anything but "No spot".
  @Test
  fun theDropdownShowsNoSpotByDefault() {
    show()

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.SPOT_DROPDOWN)
        .assertTextContains("No spot")
  }

  // Breaks if choosing a structure reports another spot, or a free point.
  @Test
  fun choosingAStructureReportsItsSpot() {
    show()

    openMenu()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.spotStructure("s2")).performClick()

    composeTestRule.runOnIdle { assertEquals(listOf(TaskSpot.onStructure("s2")), changes) }
  }

  // Breaks if "No spot" does not clear the spot.
  @Test
  fun choosingNoSpotReportsNull() {
    show(spot = TaskSpot.onStructure("s2"))
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.SPOT_DROPDOWN)
        .assertTextContains("On Marquee A")

    openMenu()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.SPOT_NONE).performClick()

    composeTestRule.runOnIdle { assertEquals(listOf<TaskSpot?>(null), changes) }
  }

  // Breaks if the map option is offered for an event whose venue is not placed.
  @Test
  fun thePickOptionIsAbsentWithoutAVenue() {
    show(venue = null)

    openMenu()

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.SPOT_NONE).assertExists()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.SPOT_PICK_POINT).assertDoesNotExist()
  }

  // Breaks if the map option is missing for an event whose venue is placed.
  @Test
  fun thePickOptionIsPresentWithAVenue() {
    show()

    openMenu()

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.SPOT_PICK_POINT).assertExists()
  }

  // Breaks if a tap on a footprint does not snap to it, or the confirm reports another spot.
  @Test
  fun aTapInsideAStructureIsConfirmedAsThatStructure() {
    show()
    openSheet()
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.SPOT_CONFIRM).assertIsNotEnabled()

    tapMapAt(-12.0, 4.0)

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.SPOT_PENDING)
        .assertTextEquals("On Marquee A")
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.SPOT_CONFIRM).performClick()
    composeTestRule.runOnIdle { assertEquals(listOf(TaskSpot.onStructure("s2")), changes) }
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.SPOT_SHEET).assertDoesNotExist()
  }

  // Breaks if a tap off every footprint is not described as a snapped point.
  @Test
  fun aTapOutsideShowsThePoint() {
    show()
    openSheet()

    tapMapAt(2.3, 7.1)

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.SPOT_PENDING)
        .assertTextEquals("Point at 2.5 m east, 7 m south")
  }

  // Breaks if Cancel reports the pending spot instead of dropping it.
  @Test
  fun cancelReportsNothing() {
    show()
    openSheet()
    tapMapAt(2.3, 7.1)

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.SPOT_CANCEL).performClick()

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.SPOT_SHEET).assertDoesNotExist()
    composeTestRule.runOnIdle { assertTrue(changes.isEmpty()) }
  }

  // Breaks if the real Google map crashes the sheet, for example without Play services tiles.
  @Test
  fun theSheetOpensWithTheRealMap() {
    show(realMap = true)

    openSheet()

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.SPOT_SHEET).assertExists()
  }
}
