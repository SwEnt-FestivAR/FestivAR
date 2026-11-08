// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.event.Venue
import com.android.festivar.model.location.LocationFix
import com.android.festivar.model.structure.Structure
import com.android.festivar.model.structure.StructureKind
import com.android.festivar.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What each state of the venue map shows, with the Google map swapped out, plus one test that the
 * real Google map composes with shapes on it.
 */
@RunWith(AndroidJUnit4::class)
class VenueMapScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val venue = Venue(46.5170, 6.5990, 0.0)
  private val structures =
      listOf(
          Structure("s1", "e1", StructureKind.STAGE, "Main stage", -6.0, -20.0, 12.0, 8.0),
          Structure("s2", "e1", StructureKind.MARQUEE, "Marquee A", -18.0, 0.0, 12.0, 8.0),
          Structure("s3", "e1", StructureKind.STAND, "Bar", 8.0, 4.0, 2.5, 3.0),
      )
  private val loaded =
      VenueMapUiState(
          isLoading = false,
          eventTitle = "Fête de Vidy",
          eventLocation = "Parc de Vidy",
          venue = venue,
          structures = structures,
      )

  private fun setContent(
      state: VenueMapUiState,
      onBack: () -> Unit = {},
      onSelect: (String?) -> Unit = {},
      onRetry: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      AppTheme {
        VenueMapContent(
            state = state,
            onBack = onBack,
            onSelect = onSelect,
            onRetry = onRetry,
            onDismissError = {},
            mapContent = { _, _ -> },
        )
      }
    }
  }

  /** Red if the screen shows anything but the spinner while the event loads. */
  @Test
  fun loadingShowsTheSpinner() {
    setContent(VenueMapUiState())

    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.LOADING).assertIsDisplayed()
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.SHEET).assertDoesNotExist()
  }

  /** Red if a loaded venue misses its title, structure count or the no fix pill. */
  @Test
  fun aVenueShowsTheTitleTheSheetAndTheNoGpsPill() {
    setContent(loaded)

    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.TITLE_PILL).assertIsDisplayed()
    composeTestRule.onNodeWithText("Fête de Vidy").assertIsDisplayed()
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.SHEET).assertIsDisplayed()
    composeTestRule.onNodeWithText("3 structures · Parc de Vidy").assertIsDisplayed()
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.NO_GPS).assertIsDisplayed()
  }

  /** Red if the no fix pill stays once a position arrives. */
  @Test
  fun aFixHidesTheNoGpsPill() {
    setContent(loaded.copy(me = LocationFix(46.5170, 6.5990, 5f)))

    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.NO_GPS).assertDoesNotExist()
  }

  /** Red if the sheet does not describe the selected structure or Clear does not deselect. */
  @Test
  fun aSelectedStructureShowsItsLabelAndSizeAndClearDeselects() {
    val selections = mutableListOf<String?>()
    setContent(loaded.copy(selectedStructureId = "s3"), onSelect = { selections += it })

    composeTestRule.onNodeWithText("Bar").assertIsDisplayed()
    composeTestRule.onNodeWithText("2.5 × 3 m").assertIsDisplayed()
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.CLEAR_SELECTION).performClick()

    assertEquals(listOf<String?>(null), selections)
  }

  /** Red if an event without venue shows the map instead of the empty state. */
  @Test
  fun aNullVenueShowsTheEmptyState() {
    setContent(loaded.copy(venue = null))

    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.EMPTY).assertIsDisplayed()
    composeTestRule.onNodeWithText("No venue yet").assertIsDisplayed()
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.SHEET).assertDoesNotExist()
  }

  /** Red if a load error is hidden or Retry does not call back. */
  @Test
  fun anErrorShowsRetryWhichCallsBack() {
    var retries = 0
    setContent(
        VenueMapUiState(isLoading = false, errorMsg = R.string.map_load_failed),
        onRetry = { retries++ },
    )

    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.RETRY).performClick()

    assertEquals(1, retries)
  }

  /** Red if the back button does not call back. */
  @Test
  fun backCallsBack() {
    var backs = 0
    setContent(loaded, onBack = { backs++ })

    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.BACK).performClick()

    assertEquals(1, backs)
  }

  /** Red if the real Google map crashes with polygons, labels and circles on it. */
  @Test
  fun theGoogleMapComposesWithStructuresAndAFix() {
    composeTestRule.setContent {
      AppTheme {
        VenueGoogleMap(
            state =
                loaded.copy(
                    structures = structures.take(2),
                    me = LocationFix(46.5170, 6.5990, 5f),
                ),
            onSelect = {},
        )
      }
    }

    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.MAP).assertExists()
  }
}
