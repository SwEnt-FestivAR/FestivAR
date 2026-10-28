// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.task.TasksRepositoryProvider
import com.android.festivar.ui.task.create.CreateTaskScreenTestTags
import com.android.festivar.ui.theme.AppTheme
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The create task route, opened through the navigation graph with a fake signed-in user. */
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

  private fun openCreateTaskRoute(authRepository: AuthRepository) {
    composeTestRule.setContent {
      AppTheme {
        val navController = rememberNavController()
        NavHost(navController, startDestination = AppScreens.createTask("event-1")) {
          taskGraph(navController, authRepository)
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

  // The create task screen is opened on top of a "start" screen, to see where leaving goes back to.
  private fun openCreateTaskOnTopOfStart(authRepository: AuthRepository): NavHostController {
    lateinit var navController: NavHostController
    composeTestRule.setContent {
      AppTheme {
        navController = rememberNavController()
        NavHost(navController, startDestination = "start") {
          composable("start") {}
          taskGraph(navController, authRepository)
        }
      }
    }
    composeTestRule.runOnIdle { navController.navigate(AppScreens.createTask("event-1")) }
    composeTestRule.waitForIdle()
    return navController
  }

  @Test
  fun createTaskRoute_backButton_returnsToThePreviousScreen() {
    val navController = openCreateTaskOnTopOfStart(mockk<AuthRepository>(relaxed = true))

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.waitForIdle()

    assertEquals("start", navController.currentBackStackEntry?.destination?.route)
  }

  @Test
  fun createTaskRoute_afterTheTaskIsCreated_returnsToThePreviousScreen() {
    val user = mockk<FirebaseUser> { every { uid } returns "owner-1" }
    val navController =
        openCreateTaskOnTopOfStart(mockk<AuthRepository> { every { currentUser } returns user })

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD)
        .performTextInput("Run power to stage")
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      navController.currentBackStackEntry?.destination?.route == "start"
    }
  }
}
