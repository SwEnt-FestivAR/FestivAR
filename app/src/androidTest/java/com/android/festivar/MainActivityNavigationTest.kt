// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
package com.android.festivar

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventRepositoryFirebase
import com.android.festivar.model.event.joinCodeOf
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.task.TasksRepositoryProvider
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UserRepositoryFirebase
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.model.user.UsersRepositoryProvider
import com.android.festivar.navigation.AppScreens
import com.android.festivar.ui.authentication.login.LoginScreenTestTags
import com.android.festivar.ui.authentication.signup.SignUpScreenTestTags
import com.android.festivar.ui.components.AvatarTestTags
import com.android.festivar.ui.event.CreateEventScreenTestTags
import com.android.festivar.ui.event.EventDetailsScreenTestTags
import com.android.festivar.ui.event.EventsOverviewScreenTestTags
import com.android.festivar.ui.event.JoinEventScreenTestTags
import com.android.festivar.ui.profile.ProfileScreenTestTags
import com.android.festivar.ui.task.create.CreateTaskScreenTestTags
import com.android.festivar.ui.task.details.TaskDetailsScreenTestTags
import com.android.festivar.ui.tasks.TaskOverviewScreenTestTags
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import java.time.ZonedDateTime
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
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
    val task = Task(taskId, eventId, title, ownerId = "owner-1")
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

    waitUntilTagExists(ProfileScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.TITLE).assertIsDisplayed()
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

    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun joinEventBackReturnsToEventsOverview() {
    setSignedInContent()

    waitUntilTagExists(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON)
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON).performClick()
    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.BACK).performClick()

    waitUntilTagExists(EventsOverviewScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun joinEventWithoutSignedInUserNavigatesToLogin() {
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = FakeAuthRepository(currentUser = null),
          startDestinationOverride = AppScreens.JoinEvent.name,
      )
    }

    waitUntilTagExists(LoginScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun joinEventWithAValidCodeOpensTheEventDetails() {
    // The join screen reads and writes the events in the Firestore emulator, whose rules need a
    // signed-in user, and the ViewModel reads the stored profile back by uid before it writes
    val start = ZonedDateTime.now().plusDays(1)
    val event =
        Event(
            eventId = "join-${UUID.randomUUID()}",
            title = "Joinable Event",
            startDate = start,
            endDate = start.plusHours(2),
        )
    // The joined user is written on the event as a member, and every later read of the events
    // resolves its members in Firestore, so the stored profile must exist there too
    val users = UserRepositoryFirebase(Firebase.firestore)
    val events = EventRepositoryFirebase(Firebase.firestore)
    runBlocking {
      Firebase.auth.signInAnonymously().await()
      users.addUser(storedUser)
      events.addEvent(event)
    }

    try {
      setSignedInContent()

      waitUntilTagExists(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON)
      composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON).performClick()
      composeTestRule
          .onNodeWithTag(JoinEventScreenTestTags.CODE_INPUT)
          .performTextInput(joinCodeOf(event))
      waitUntilTagExists(JoinEventScreenTestTags.join(event.eventId))
      composeTestRule.onNodeWithTag(JoinEventScreenTestTags.join(event.eventId)).performClick()

      composeTestRule.waitUntil(timeoutMillis = 10_000) {
        composeTestRule
            .onAllNodesWithTag(EventDetailsScreenTestTags.EVENT_NAME)
            .fetchSemanticsNodes()
            .isNotEmpty()
      }
      composeTestRule
          .onNodeWithTag(EventDetailsScreenTestTags.EVENT_NAME)
          .assertTextEquals(event.title)

      // The join screen was dropped from the back stack, so back lands on the overview
      composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.BACK_BUTTON).performClick()
      waitUntilTagExists(EventsOverviewScreenTestTags.TITLE)
      composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.TITLE).assertIsDisplayed()
    } finally {
      runBlocking {
        runCatching { events.deleteEvent(event.eventId) }
        runCatching { users.deleteUser(storedUser.uid) }
        Firebase.auth.signOut()
      }
    }
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
  fun eventOverviewEventClickNavigatesToEventDetails() {
    // The overview reads the events from the Firestore emulator, whose rules need a signed-in user
    val now = ZonedDateTime.now()
    val event =
        Event(
            eventId = "event-${UUID.randomUUID()}",
            title = "Fête de l'Asso",
            members = listOf(storedUser),
            startDate = now.minusHours(1),
            endDate = now.plusHours(5),
        )
    val users = UserRepositoryFirebase(Firebase.firestore)
    val events = EventRepositoryFirebase(Firebase.firestore)
    runBlocking {
      Firebase.auth.signInAnonymously().await()
      users.addUser(storedUser)
      events.addEvent(event)
    }

    try {
      setSignedInContent()

      // The event is ongoing, so it is shown by default
      val eventItem = EventsOverviewScreenTestTags.getTestTagForEventItem(event)
      waitUntilTagExists(eventItem)
      composeTestRule.onNodeWithTag(eventItem).performClick()

      composeTestRule.waitUntil(timeoutMillis = 10_000) {
        composeTestRule.onAllNodesWithText(event.title).fetchSemanticsNodes().isNotEmpty()
      }
      composeTestRule.onNodeWithText(event.title).assertIsDisplayed()
    } finally {
      runBlocking {
        runCatching { events.deleteEvent(event.eventId) }
        runCatching { users.deleteUser(storedUser.uid) }
        Firebase.auth.signOut()
      }
    }
  }

  @Test
  fun eventDetailsRouteDisplaysEventDetailsScreen() {
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = FakeAuthRepository(currentUser = signedInUser),
          startDestinationOverride = AppScreens.eventDetails("e1"),
      )
    }

    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      composeTestRule
          .onAllNodesWithTag(EventDetailsScreenTestTags.FETCH_ERROR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.FETCH_ERROR).assertIsDisplayed()
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
  fun addTaskOnTheOverviewOpensCreateTaskAndTheNewTaskShowsOnReturn() {
    TasksRepositoryProvider.repository = TasksRepositoryLocal()
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = FakeAuthRepository(currentUser = firebaseUser),
          startDestinationOverride = AppScreens.taskOverview("e1"),
      )
    }

    waitUntilTagExists(TaskOverviewScreenTestTags.ADD_TASK)
    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.ADD_TASK).performClick()
    waitUntilTagExists(CreateTaskScreenTestTags.TITLE)

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD)
        .performTextInput("Hang the banner")
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).performClick()

    // The create screen pops itself once saved, and the overview reloads on return. The local
    // repository numbers its ids from 0, so the first saved task is row "0"; the row tag is used
    // because the title also sits in the create screen's field while that screen leaves.
    waitUntilTagExists(TaskOverviewScreenTestTags.TITLE)
    waitUntilTagExists(TaskOverviewScreenTestTags.row("0"))
    composeTestRule
        .onNodeWithTag(TaskOverviewScreenTestTags.row("0"))
        .assertIsDisplayed()
        .assert(hasAnyDescendant(hasText("Hang the banner")) or hasText("Hang the banner"))
  }

  @Test
  fun createEventRouteDisplaysCreateEventScreen() {
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = FakeAuthRepository(currentUser = firebaseUser),
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

  /** Puts a user with the uid of [firebaseUser] in the users repository, for the profile screen. */
  private fun seedUser() {
    UsersRepositoryProvider.repository =
        UsersRepositoryLocal(listOf(User(uid = testUserId, name = "Ada", surname = "Lovelace")))
  }

  @Test
  fun profileRouteDisplaysProfileScreen() {
    seedUser()

    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = FakeAuthRepository(currentUser = firebaseUser),
          startDestinationOverride = AppScreens.Profile.name,
      )
    }

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun profileSignOutReturnsToLogin() {
    seedUser()

    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = FakeAuthRepository(currentUser = firebaseUser),
          startDestinationOverride = AppScreens.Profile.name,
      )
    }

    // The sign out button only shows once the profile is loaded
    composeTestRule.waitUntil(5_000) {
      composeTestRule
          .onAllNodes(hasTestTag(ProfileScreenTestTags.SIGN_OUT_BUTTON))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SIGN_OUT_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun profileRouteRedirectsToLoginWhenUserIsNull() {
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          authRepository = FakeAuthRepository(currentUser = null),
          startDestinationOverride = AppScreens.Profile.name,
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
