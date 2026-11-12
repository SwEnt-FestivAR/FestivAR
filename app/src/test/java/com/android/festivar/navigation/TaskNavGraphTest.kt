// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.task.TasksRepositoryProvider
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.navigation.task.taskGraph
import com.android.festivar.ui.task.create.CreateTaskScreenTestTags
import com.android.festivar.ui.task.details.TaskDetailsScreenTestTags
import com.android.festivar.ui.task.mytasks.MyTasksScreenTestTags
import com.android.festivar.ui.theme.AppTheme
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The create and edit task routes, opened through the navigation graph with a fake signed-in user.
 */
@RunWith(AndroidJUnit4::class)
class TaskNavGraphTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val repository = TasksRepositoryLocal()

  @Before
  fun setUp() {
    TasksRepositoryProvider.repository = repository
  }

  @After
  fun tearDown() {
    TasksRepositoryProvider.reset()
  }

  // The create route saves through the data repository, which also adds the task to its event and
  // to its owner: both must exist, and the task lands in the same [repository] as the edit route.
  private fun dataRepository(
      authRepository: AuthRepository,
      ownerTasks: List<String> = emptyList(),
  ): DataRepositoryLocal {
    val owner = User("owner-1", joinedEvents = listOf("event-1"), tasksAssigned = ownerTasks)
    val start = ZonedDateTime.now().plusDays(1)
    val event =
        Event(
            eventId = "event-1",
            ownerId = owner.uid,
            title = "Summer festival",
            members = listOf(owner),
            startDate = start,
            endDate = start.plusDays(2),
        )
    return DataRepositoryLocal(
        authRepository = authRepository,
        tasksRepository = repository,
        eventsRepository = EventsRepositoryLocal(listOf(event)),
        usersRepository = UsersRepositoryLocal(listOf(owner)),
    )
  }

  private fun openCreateTaskRoute(authRepository: AuthRepository) {
    composeTestRule.setContent {
      AppTheme {
        val navController = rememberNavController()
        NavHost(navController, startDestination = AppScreens.createTask("event-1")) {
          taskGraph(navController, dataRepository(authRepository))
        }
      }
    }
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD)
        .performTextInput("Run power to stage")
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).performClick()
  }

  @Test
  fun createTaskRoute_makesTheSignedInUserTheOwnerOfTheTask() {
    val user = mockk<FirebaseUser> { every { uid } returns "owner-1" }
    openCreateTaskRoute(mockk<AuthRepository> { every { currentUser } returns user })
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      runBlocking { repository.getAllTasks("event-1") }.isNotEmpty()
    }

    val saved = runBlocking { repository.getAllTasks("event-1") }.single()
    assertEquals("owner-1", saved.ownerId)
    assertEquals("event-1", saved.eventId)
  }

  @Test
  fun createTaskRoute_withNobodySignedIn_savesNothingAndShowsTheSaveError() {
    openCreateTaskRoute(mockk<AuthRepository> { every { currentUser } returns null })
    composeTestRule.waitForIdle()

    val message =
        ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.create_task_error_save_failed)
    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.SAVE_ERROR)
        .assertIsDisplayed()
        .assertTextEquals(message)
    assertTrue(runBlocking { repository.getAllTasks("event-1") }.isEmpty())
  }

  // The [route] is opened on top of a "start" screen, to see where leaving goes back to.
  private fun openOnTopOfStart(
      route: String,
      authRepository: AuthRepository,
      ownerTasks: List<String> = emptyList(),
  ): NavHostController {
    lateinit var navController: NavHostController
    composeTestRule.setContent {
      AppTheme {
        navController = rememberNavController()
        NavHost(navController, startDestination = "start") {
          composable("start") {}
          taskGraph(navController, dataRepository(authRepository, ownerTasks))
        }
      }
    }
    composeTestRule.runOnIdle { navController.navigate(route) }
    composeTestRule.waitForIdle()
    return navController
  }

  private fun signedInAs(userId: String): AuthRepository {
    val user = mockk<FirebaseUser> { every { uid } returns userId }
    return mockk<AuthRepository> { every { currentUser } returns user }
  }

  @Test
  fun createTaskRoute_backButton_returnsToThePreviousScreen() {
    val navController =
        openOnTopOfStart(AppScreens.createTask("event-1"), mockk<AuthRepository>(relaxed = true))

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.waitForIdle()

    assertEquals("start", navController.currentBackStackEntry?.destination?.route)
  }

  @Test
  fun createTaskRoute_afterTheTaskIsCreated_returnsToThePreviousScreen() {
    val navController = openOnTopOfStart(AppScreens.createTask("event-1"), signedInAs("owner-1"))

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD)
        .performTextInput("Run power to stage")
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      navController.currentBackStackEntry?.destination?.route == "start"
    }
    val saved = runBlocking { repository.getAllTasks("event-1") }.single()
    assertEquals("owner-1", saved.ownerId)
  }

  // Opens the edit of task "t1", owned by the signed-in "owner-1", once its form is loaded.
  private fun openEditTaskOnTopOfStart(): NavHostController {
    runBlocking { repository.addTask(Task("t1", "event-1", "Old title", ownerId = "owner-1")) }
    val navController = openOnTopOfStart(AppScreens.editTask("t1"), signedInAs("owner-1"))
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(CreateTaskScreenTestTags.TITLE_FIELD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    return navController
  }

  // Breaks if the edit route does not save as the signed-in owner or does not leave once saved.
  @Test
  fun editTaskRoute_afterTheEditIsSaved_returnsToThePreviousScreen() {
    val navController = openEditTaskOnTopOfStart()

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD)
        .performTextReplacement("New title")
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      navController.currentBackStackEntry?.destination?.route == "start"
    }
    assertEquals("New title", runBlocking { repository.getTask("t1") }.title)
  }

  // Breaks if back from an unchanged edit form does not pop the edit route.
  @Test
  fun editTaskRoute_back_returnsToThePreviousScreen() {
    val navController = openEditTaskOnTopOfStart()

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.waitForIdle()

    assertEquals("start", navController.currentBackStackEntry?.destination?.route)
    assertEquals("Old title", runBlocking { repository.getTask("t1") }.title)
  }

  // Breaks if the My tasks route stops showing the signed-in user's tasks, if a tapped row stops
  // opening the details of that task, or if the arrow or the system back of the details stops
  // returning to My tasks.
  @Test
  fun myTasksRoute_aRowOpensItsTaskDetailsAndBackReturns() {
    val owner = User("owner-1")
    runBlocking {
      repository.addTask(
          Task(
              "t1",
              "event-1",
              "Run power to stage",
              assignees = listOf(owner),
              ownerId = owner.uid,
          )
      )
    }
    // Relaxed: the task details read the user's name and email too
    val user = mockk<FirebaseUser>(relaxed = true) { every { uid } returns owner.uid }
    val navController =
        openOnTopOfStart(
            AppScreens.MyTasks.name,
            mockk<AuthRepository> { every { currentUser } returns user },
            ownerTasks = listOf("t1"),
        )
    val route = { navController.currentBackStackEntry?.destination?.route }
    // Waits for the row, taps it, and checks the details of t1 opened
    val openTheTask = {
      composeTestRule.waitUntil(timeoutMillis = 5_000) {
        composeTestRule
            .onAllNodesWithTag(MyTasksScreenTestTags.row("t1"))
            .fetchSemanticsNodes()
            .isNotEmpty()
      }
      composeTestRule.onNodeWithTag(MyTasksScreenTestTags.row("t1")).performClick()
      composeTestRule.waitForIdle()
      assertEquals("${AppScreens.TaskDetails.name}/{taskId}", route())
      assertEquals("t1", navController.currentBackStackEntry?.arguments?.getString("taskId"))
    }

    openTheTask()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.waitForIdle()
    assertEquals(AppScreens.MyTasks.name, route())

    openTheTask()
    Espresso.pressBack()
    composeTestRule.waitForIdle()
    assertEquals(AppScreens.MyTasks.name, route())
  }
}
