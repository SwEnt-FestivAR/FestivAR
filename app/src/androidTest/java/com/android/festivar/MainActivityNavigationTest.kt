// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
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
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.model.user.UsersRepositoryProvider
import com.android.festivar.navigation.AppScreens
import com.android.festivar.ui.authentication.login.LoginScreenTestTags
import com.android.festivar.ui.authentication.signup.SignUpScreenTestTags
import com.android.festivar.ui.components.AvatarTestTags
import com.android.festivar.ui.event.CreateEventScreenTestTags
import com.android.festivar.ui.event.EventsOverviewScreenTestTags
import com.android.festivar.ui.task.create.CreateTaskScreenTestTags
import com.android.festivar.ui.task.details.TaskDetailsScreenTestTags
import com.android.festivar.ui.tasks.TaskOverviewScreenTestTags
import com.google.firebase.auth.FirebaseUser
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
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

  // The events overview fetches the events of the signed-in user from Firestore
  private val signedInUid = "user-${UUID.randomUUID()}"

  private val signedInUser = mockk<FirebaseUser> { every { uid } returns signedInUid }

  // The avatar shows the initials of the stored profile of the signed-in user
  private val storedUser = User(uid = signedInUid, name = "Sara", surname = "Keller")

  @Before
  fun setUp() {
    FirebaseEmulatorSetup.configure()
    MockKAnnotations.init(this, relaxUnitFun = true)

    UsersRepositoryProvider.repository = UsersRepositoryLocal(listOf(storedUser))

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

  @Test
  fun eventOverviewShowsStoredUserAndAvatarNavigatesToProfile() {
    setSignedInContent()

    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      composeTestRule.onAllNodesWithText("SK").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithText("SK").assertIsDisplayed()

    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.USER_AVATAR).performClick()

    composeTestRule.onNodeWithText("Hello profile!").assertIsDisplayed()
  }

  @Test
  fun eventOverviewWithoutStoredUserShowsPersonIcon() {
    UsersRepositoryProvider.repository = UsersRepositoryLocal()
    setSignedInContent()

    waitUntilTagExists(EventsOverviewScreenTestTags.USER_AVATAR)
    composeTestRule
        .onNodeWithTag(AvatarTestTags.PERSON_ICON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun eventOverviewJoinNavigatesToJoinEvent() {
    setSignedInContent()

    // The new user has no events, so the "Join" button of the empty state shows once loaded
    waitUntilTagExists(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON)
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON).performClick()

    composeTestRule.onNodeWithText("Hello join event!").assertIsDisplayed()
  }

  @Test
  fun eventOverviewWithoutSignedInUserNavigatesToLogin() {
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = FakeAuthRepository(currentUser = null),
          startDestinationOverride = AppScreens.EventsOverview.name,
      )
    }

    waitUntilTagExists(LoginScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun eventOverviewCreateNavigatesToCreateEventAndBack() {
    setSignedInContent()

    // The new user has no events, so the "Create" button of the empty state shows once loaded
    waitUntilTagExists(EventsOverviewScreenTestTags.CREATE_EVENT_BUTTON)
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()
    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.TITLE).assertIsDisplayed()

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.NAVIGATION_BUTTON).performClick()
    waitUntilTagExists(EventsOverviewScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.TITLE).assertIsDisplayed()
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
    UsersRepositoryProvider.reset()
  }

  /** Shows the app as [signedInUser], so it starts on the events overview. */
  private fun setSignedInContent() {
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = FakeAuthRepository(currentUser = signedInUser),
      )
    }
  }

  private fun waitUntilTagExists(tag: String) {
    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
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
