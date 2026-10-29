package com.android.festivar.ui.event

// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
// Co-authored-by: Copilot App

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The save states of the create event screen: saving, saved, failed. */
@RunWith(AndroidJUnit4::class)
class CreateEventScreenSaveTest {
  @get:Rule val composeTestRule = createComposeRule()

  private class GatedRepository(val gate: CompletableDeferred<Unit>) :
      EventsRepository by EventsRepositoryLocal() {
    override suspend fun addEvent(event: Event) = gate.await()
  }

  private class FailingRepository : EventsRepository by EventsRepositoryLocal() {
    override suspend fun addEvent(event: Event): Unit = throw IllegalStateException("offline")
  }

  private fun filledViewModel(repository: EventsRepository) =
      CreateEventViewModel(repository).apply {
        updateName("Summer festival")
        updateStartDate(LocalDate.of(2026, 10, 8))
        updateEndDate(LocalDate.of(2026, 10, 9))
        updateVenue("Main square")
      }

  private fun waitForTag(tag: String) {
    composeTestRule.waitUntil(5_000) {
      composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
  }

  /**
   * Red if the button stays enabled or shows no spinner while the write runs, or if the screen
   * never reports the saved event.
   */
  @Test
  fun savingDisablesTheButtonWithASpinnerAndASavedEventCallsOnCreated() {
    val gate = CompletableDeferred<Unit>()
    val viewModel = filledViewModel(GatedRepository(gate))
    var created = 0
    composeTestRule.setContent {
      CreateEventScreen(viewModel = viewModel, onCreated = { created++ })
    }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).performClick()

    waitForTag(CreateEventScreenTestTags.SAVING)
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsNotEnabled()
    assertEquals(0, created)

    gate.complete(Unit)
    composeTestRule.waitUntil(5_000) { created == 1 }
    assertEquals(1, created)
  }

  /** Red if a failed write shows nothing, or leaves the button disabled so no retry is possible. */
  @Test
  fun aFailedSaveShowsTheErrorLineAndKeepsTheButtonEnabled() {
    val viewModel = filledViewModel(FailingRepository())
    var created = 0
    composeTestRule.setContent {
      CreateEventScreen(viewModel = viewModel, onCreated = { created++ })
    }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).performClick()

    waitForTag(CreateEventScreenTestTags.SAVE_ERROR)
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.SAVE_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).assertIsEnabled()
    assertEquals(0, created)
  }
}
