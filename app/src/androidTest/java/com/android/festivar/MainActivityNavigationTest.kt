// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.task.TasksRepositoryProvider
import com.android.festivar.navigation.AppScreens
import com.android.festivar.ui.authentication.login.LoginScreenTestTags
import com.android.festivar.ui.authentication.signup.SignUpScreenTestTags
import com.android.festivar.ui.event.CreateEventScreenTestTags
import com.android.festivar.ui.task.create.CreateTaskScreenTestTags
import com.android.festivar.ui.task.details.TaskDetailsScreenTestTags
import com.android.festivar.ui.tasks.TaskOverviewScreenTestTags
import com.google.firebase.auth.FirebaseUser
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityNavigationTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val testUserId = "test-user-id"

  private lateinit var firebaseUser: FirebaseUser

  @Before
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)

    firebaseUser = mockk(relaxed = true) { every { uid } returns testUserId }
  }

  private fun seedTask(taskId: String = "t1", eventId: String = "e1", title: String = "Test Task") {
    val localRepo = TasksRepositoryLocal()
    val task = Task(taskId, eventId, title)
    runBlocking { localRepo.addTask(task) }
    TasksRepositoryProvider.repository = localRepo
  }

  @Test
  fun loginNavigatesToSignUpAndBackToLogin() {
    val authRepository = FakeAuthRepository(currentUser = null)

    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = authRepository,
      )
    }

    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()

    composeTestRule.onNodeWithTag(LoginScreenTestTags.CREATE_ACC_BUTTON).performClick()
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.CREATE_ACC_TITLE).assertIsDisplayed()

    composeTestRule.onNodeWithTag(SignUpScreenTestTags.LOGIN_NOW_BUTTON).performClick()
    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
  }

  // TODO when screen is actually in navigation, delete this test and use the real screen instead of
  // the placeholder
  @Test
  fun eventOverviewLogoutSignsOutAndReturnsToLogin() {
    val authRepository = FakeAuthRepository(currentUser = mockk())

    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = authRepository,
      )
    }

    composeTestRule
        .onNodeWithText("Hello event overview, click to logout!")
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun taskOverviewRouteDisplaysTaskOverviewScreen() {
    TasksRepositoryProvider.repository = TasksRepositoryLocal()
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = FakeAuthRepository(currentUser = firebaseUser),
          startDestinationOverride = AppScreens.taskOverview("e1"),
      )
    }

    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun createEventRouteDisplaysCreateEventScreen() {
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = FakeAuthRepository(currentUser = mockk()),
          startDestinationOverride = AppScreens.CreateEvent.name,
      )
    }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun createTaskRouteDisplaysCreateTaskScreen() {
    TasksRepositoryProvider.repository = TasksRepositoryLocal()
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = FakeAuthRepository(currentUser = firebaseUser),
          startDestinationOverride = "${AppScreens.CreateTask.name}/e1",
      )
    }

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun taskDetailsRouteDisplaysTaskDetailsScreen() {
    seedTask()

    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = FakeAuthRepository(currentUser = firebaseUser),
          startDestinationOverride = AppScreens.taskDetails("t1"),
      )
    }

    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodes(hasTestTag(TaskDetailsScreenTestTags.TITLE))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun clickingTaskInOverviewNavigatesToTaskDetails() {
    seedTask()

    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = FakeAuthRepository(currentUser = firebaseUser),
          startDestinationOverride = AppScreens.taskOverview("e1"),
      )
    }

    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodes(hasTestTag(TaskOverviewScreenTestTags.row("t1")))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.row("t1")).performClick()
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodes(hasTestTag(TaskDetailsScreenTestTags.TITLE))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun taskDetailsRouteRedirectsToLoginWhenUserIsNull() {
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = FakeAuthRepository(currentUser = null),
          startDestinationOverride = AppScreens.taskDetails("t1"),
      )
    }

    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
  }

  @After
  fun tearDown() {
    TasksRepositoryProvider.reset()
  }
}

class FakeAuthRepository(override val currentUser: FirebaseUser?) : AuthRepository {
  override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> =
      Result.failure(UnsupportedOperationException())

  override suspend fun signOut(): Result<Unit> = Result.success(Unit)

  override suspend fun signInWithEmailAndPassword(
      email: String,
      password: String,
  ): Result<FirebaseUser> = Result.failure(UnsupportedOperationException())

  override suspend fun signUpWithEmailAndPassword(
      email: String,
      password: String,
  ): Result<FirebaseUser> = Result.failure(UnsupportedOperationException())
}
