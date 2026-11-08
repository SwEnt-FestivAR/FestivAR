// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.map

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.event.Venue
import com.android.festivar.model.location.LocationFix
import com.android.festivar.model.structure.Structure
import com.android.festivar.model.structure.StructureKind
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TaskSpot
import com.android.festivar.model.user.User
import com.android.festivar.ui.theme.AppTheme
import java.time.ZonedDateTime
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

  private val start = ZonedDateTime.parse("2026-07-01T14:00:00+02:00")
  private val me = User("me", "Ada", "Lovelace")
  private val sami = User("u2", "Sami", "Kader")
  private val tasks =
      listOf(
          Task(
              taskId = "t1",
              eventId = "e1",
              title = "Run power to stage",
              location = "Stage, north corner",
              startTime = start.plusMinutes(40),
              maxAssign = 2,
              assignees = listOf(me, sami),
              spot = TaskSpot.onStructure("s1"),
              ownerId = "o",
          ),
          Task(
              taskId = "t2",
              eventId = "e1",
              title = "Ice bags",
              spot = TaskSpot.at(10.0, 10.0),
              ownerId = "o",
          ),
      )
  private val withTasks = loaded.copy(tasks = tasks, currentUserId = "me", now = start)

  /** The organizer's view with the stage selected; one task is pinned on it. */
  private val editing = withTasks.copy(isOrganizer = true, selectedStructureId = "s1")

  /** What the screen shows; a test may change it after [setContent] to follow a callback. */
  private val screen = mutableStateOf(VenueMapUiState())

  private fun setContent(
      state: VenueMapUiState,
      onBack: () -> Unit = {},
      onSelect: (String?) -> Unit = {},
      onRetry: () -> Unit = {},
      onFilter: (PinFilter) -> Unit = {},
      onQuery: (String) -> Unit = {},
      onOpenTask: (String) -> Unit = {},
      onSelectTool: (PaletteTool?) -> Unit = {},
      onStartRename: () -> Unit = {},
      onRename: (String) -> Unit = {},
      onCancelRename: () -> Unit = {},
  ) {
    screen.value = state
    composeTestRule.setContent {
      AppTheme {
        VenueMapContent(
            state = screen.value,
            onBack = onBack,
            onSelect = onSelect,
            onRetry = onRetry,
            onDismissError = {},
            onFilter = onFilter,
            onQuery = onQuery,
            onOpenTask = onOpenTask,
            onSelectTool = onSelectTool,
            onStartRename = onStartRename,
            onRename = onRename,
            onCancelRename = onCancelRename,
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

  /** Red if a loaded venue misses the search, its title, structure count or the no fix pill. */
  @Test
  fun aVenueShowsTheSearchTheSheetAndTheNoGpsPill() {
    setContent(loaded)

    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.SEARCH).assertIsDisplayed()
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.SHEET).assertIsDisplayed()
    composeTestRule.onNodeWithText("Fête de Vidy · 3 structures · Parc de Vidy").assertIsDisplayed()
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.NO_GPS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.EMPTY_PINS).assertDoesNotExist()
  }

  /** Red if a chip shows the wrong count or does not call back with its filter. */
  @Test
  fun chipsShowTheCountsAndCallSetFilter() {
    val filters = mutableListOf<PinFilter>()
    setContent(withTasks, onFilter = { filters += it })

    composeTestRule.onNodeWithText("My pins 1").assertIsDisplayed()
    composeTestRule.onNodeWithText("All 2").assertIsDisplayed()
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.CHIP_ALL).performClick()
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.CHIP_STRUCTURES).performClick()
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.CHIP_MINE).performClick()

    assertEquals(listOf(PinFilter.ALL, PinFilter.STRUCTURES, PinFilter.MINE), filters)
  }

  /** Red if typing in the search does not reach setQuery. */
  @Test
  fun typingInTheSearchCallsSetQuery() {
    val queries = mutableListOf<String>()
    setContent(loaded, onQuery = { queries += it })

    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.SEARCH).performTextInput("bar")

    assertEquals("bar", queries.last())
  }

  /** Red if the task sheet misses a line or Open task does not call back with the id. */
  @Test
  fun aFocusedTaskShowsItsSheetAndOpenTaskCallsBack() {
    val opened = mutableListOf<String>()
    // Me at the venue centre, the pin on the stage centre (0, -16): 16 m, so 1 minute on foot.
    setContent(
        withTasks.copy(selectedTaskId = "t1", me = LocationFix(46.5170, 6.5990, 5f)),
        onOpenTask = { opened += it },
    )

    composeTestRule.onNodeWithText("Run power to stage").assertIsDisplayed()
    composeTestRule.onNodeWithText("Stage, north corner · with Sami K.").assertIsDisplayed()
    composeTestRule.onNodeWithText("14:40").assertIsDisplayed()
    composeTestRule.onNodeWithText("in 40 min").assertIsDisplayed()
    composeTestRule.onNodeWithText("16 m, 1 min").assertIsDisplayed()
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.OPEN_TASK).performClick()

    assertEquals(listOf("t1"), opened)
  }

  /** Red if the task sheet invents a distance without a fix. */
  @Test
  fun aFocusedTaskWithoutAFixSaysTheDistanceIsUnknown() {
    setContent(withTasks.copy(selectedTaskId = "t1"))

    composeTestRule.onNodeWithText("Distance unknown").assertIsDisplayed()
  }

  /** Red if an empty venue does not say that nothing is placed. */
  @Test
  fun nothingPlacedShowsTheEmptyLine() {
    setContent(loaded.copy(structures = emptyList()))

    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.EMPTY_PINS).assertIsDisplayed()
    composeTestRule.onNodeWithText("Nothing placed yet").assertIsDisplayed()
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

  /** Red if the real Google map crashes with polygons, labels, task pins and circles on it. */
  @Test
  fun theGoogleMapComposesWithStructuresPinsAndAFix() {
    composeTestRule.setContent {
      AppTheme {
        VenueGoogleMap(
            state =
                withTasks.copy(
                    structures = structures.take(1),
                    filter = PinFilter.ALL,
                    me = LocationFix(46.5170, 6.5990, 5f),
                ),
            onSelect = {},
            onSelectTask = {},
        )
      }
    }

    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.MAP).assertExists()
  }

  /** Red if the real Google map crashes with the organizer's corner handles on it. */
  @Test
  fun theGoogleMapComposesWithCornerHandlesForTheOrganizer() {
    composeTestRule.setContent {
      AppTheme {
        VenueGoogleMap(
            state = editing.copy(tool = PaletteTool.STAND),
            onSelect = {},
            onSelectTask = {},
            onPlace = { _, _ -> },
            onMove = { _, _ -> },
            onResize = { _, _, _ -> },
        )
      }
    }

    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.MAP).assertExists()
  }

  /** Red if the organizer misses the badge or the palette, or a volunteer sees them. */
  @Test
  fun theOrganizerSeesTheEditingToolsAndAVolunteerDoesNot() {
    setContent(loaded.copy(isOrganizer = true))

    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.EDITING_BADGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.PALETTE_HINT).assertIsDisplayed()
    PaletteTool.entries.forEach {
      composeTestRule.onNodeWithTag(VenueMapScreenTestTags.palette(it)).assertIsDisplayed()
    }

    screen.value = loaded.copy(selectedStructureId = "s1")

    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.EDITING_BADGE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.PALETTE_HINT).assertDoesNotExist()
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.RENAME).assertDoesNotExist()
    PaletteTool.entries.forEach {
      composeTestRule.onNodeWithTag(VenueMapScreenTestTags.palette(it)).assertDoesNotExist()
    }
  }

  /** Red if a palette item does not call selectTool with its tool. */
  @Test
  fun tappingAPaletteItemCallsSelectTool() {
    val tools = mutableListOf<PaletteTool?>()
    setContent(loaded.copy(isOrganizer = true), onSelectTool = { tools += it })

    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.palette(PaletteTool.STAND)).performClick()
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.palette(PaletteTool.STAGE)).performClick()

    assertEquals(listOf<PaletteTool?>(PaletteTool.STAND, PaletteTool.STAGE), tools)
  }

  /** Red if the organizer's sheet misses the label, the size or the pinned task count. */
  @Test
  fun theSelectedStructureSheetShowsItsLabelAndSummary() {
    setContent(editing)

    composeTestRule
        .onNodeWithTag(VenueMapScreenTestTags.SELECTED_LABEL)
        .assertTextEquals("Main stage")
    composeTestRule.onNodeWithText("12 × 8 m · 1 task").assertIsDisplayed()
  }

  /** Red if the pencil does not open the dialog, or Save does not pass the typed name. */
  @Test
  fun renameOpensTheDialogAndSaveCallsRenameWithTheText() {
    val names = mutableListOf<String>()
    setContent(
        editing,
        onStartRename = { screen.value = screen.value.copy(renaming = true) },
        onRename = { names += it },
    )

    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.RENAME).performClick()
    composeTestRule
        .onNodeWithTag(VenueMapScreenTestTags.RENAME_FIELD)
        .performTextReplacement("Big top")
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.RENAME_SAVE).performClick()

    assertEquals(listOf("Big top"), names)
  }

  /** Red if Cancel does not call cancelRename, or a blank name can be saved. */
  @Test
  fun renameCancelCallsBackAndABlankNameCannotBeSaved() {
    var cancels = 0
    setContent(editing.copy(renaming = true), onCancelRename = { cancels++ })

    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.RENAME_FIELD).performTextReplacement(" ")
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.RENAME_SAVE).assertIsNotEnabled()
    composeTestRule.onNodeWithTag(VenueMapScreenTestTags.RENAME_CANCEL).performClick()

    assertEquals(1, cancels)
  }
}
