package com.android.festivar.ui.tasks

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.task.TasksRepositoryProvider
import com.android.festivar.model.temporary.User
import java.time.Duration
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Wiring only: the screen shows the ViewModel's state and calls back. The rules are unit tested.
 */
@RunWith(AndroidJUnit4::class)
class TaskOverviewScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val power =
      Task(
          "power",
          EVENT,
          "Run power to stage",
          estimatedTime = Duration.ofMinutes(80),
          location = "Stage",
          maxAssign = 2,
      )
  private val bunting = Task("bunting", EVENT, "Hang bunting", location = "Marquee A")
  private val sound = Task("sound", EVENT, "Sound check", assignees = listOf(User(ME)))
  private val elsewhere = Task("elsewhere", "other-event", "Other event's task")

  @Before
  fun setUp() {
    val repository = TasksRepositoryLocal()
    runBlocking { listOf(power, bunting, sound, elsewhere).forEach { repository.addTask(it) } }
    TasksRepositoryProvider.repository = repository
  }

  @After
  fun tearDown() {
    TasksRepositoryProvider.repository = TasksRepositoryLocal()
  }

  private fun setOverview(onBack: () -> Unit = {}, onOpenTask: (String) -> Unit = {}) {
    composeTestRule.setContent {
      TaskOverviewScreen(EVENT, userId = ME, onBack = onBack, onOpenTask = onOpenTask)
    }
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodesWithTag(TaskOverviewScreenTestTags.row(power.taskId))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  private fun assertRowGone(taskId: String) =
      composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.row(taskId)).assertDoesNotExist()

  /** Turns red when the title or a chip disappears or a chip stops showing its count. */
  @Test
  fun titleAndTheFourChipsShowWithTheirCounts() {
    setOverview()

    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(TaskOverviewScreenTestTags.CHIP_OPEN)
        .assertIsDisplayed()
        .assertTextContains("2")
    composeTestRule
        .onNodeWithTag(TaskOverviewScreenTestTags.CHIP_MINE)
        .assertIsDisplayed()
        .assertTextContains("1")
    composeTestRule
        .onNodeWithTag(TaskOverviewScreenTestTags.CHIP_DONE)
        .assertIsDisplayed()
        .assertTextContains("0")
    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.CHIP_ALL).assertIsDisplayed()
  }

  /** Turns red when the screen shows another event's tasks or drops one of this event's. */
  @Test
  fun onlyThisEventsRowsAreShown() {
    setOverview()
    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.CHIP_ALL).performClick()

    listOf(power, bunting, sound).forEach {
      composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.row(it.taskId)).assertIsDisplayed()
    }
    assertRowGone(elsewhere.taskId)
  }

  /** Turns red when the screen stops wording a row's place, people and length from resources. */
  @Test
  fun aRowShowsItsPlacePeopleAndLength() {
    setOverview()

    composeTestRule.onNodeWithText("Stage · 2 people needed · 1 h 20 min").assertIsDisplayed()
  }

  /** Turns red when a row stops calling onOpenTask with its own id. */
  @Test
  fun tappingARowOpensThatTask() {
    var opened: String? = null
    setOverview(onOpenTask = { opened = it })

    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.row(bunting.taskId)).performClick()

    assertEquals(bunting.taskId, opened)
  }

  /** Turns red when the Done chip stops filtering or the empty line stops showing. */
  @Test
  fun doneWithNothingDoneShowsTheEmptyLine() {
    setOverview()

    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.CHIP_DONE).performClick()

    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.EMPTY).assertIsDisplayed()
    assertRowGone(power.taskId)
  }

  /** Turns red when the search field stops reaching the ViewModel's query. */
  @Test
  fun searchingByTitleFiltersTheRows() {
    setOverview()

    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.SEARCH).performClick()
    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.SEARCH_FIELD).performTextInput("bunt")

    composeTestRule
        .onNodeWithTag(TaskOverviewScreenTestTags.row(bunting.taskId))
        .assertIsDisplayed()
    assertRowGone(power.taskId)
  }

  /** Turns red when the back button stops calling onBack. */
  @Test
  fun backCallsOnBack() {
    var back = false
    setOverview(onBack = { back = true })

    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.BACK).performClick()

    assertTrue(back)
  }

  private companion object {
    const val EVENT = "fete"
    const val ME = "me"
  }
}
