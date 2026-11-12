// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Gemini <noreply@google.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.credentials.CredentialManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryFirebase
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventRepositoryFirebase
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.EventsRepositoryProvider
import com.android.festivar.model.event.announcement.Announcement
import com.android.festivar.model.event.joinCodeOf
import com.android.festivar.model.task.Task
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.task.TasksRepositoryProvider
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryFirebase
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.model.user.UsersRepositoryProvider
import com.android.festivar.navigation.AppScreens
import com.android.festivar.navigation.TaskOrigin
import com.android.festivar.ui.announcement.create.CreateAnnouncementScreenTestTags
import com.android.festivar.ui.announcement.feed.AnnouncementFeedScreenTestTags
import com.android.festivar.ui.authentication.login.LoginScreenTestTags
import com.android.festivar.ui.authentication.signup.SignUpScreenTestTags
import com.android.festivar.ui.components.BottomNavigationBarTestTags
import com.android.festivar.ui.components.NavigationSubScreen
import com.android.festivar.ui.components.NavigationTab
import com.android.festivar.ui.event.create.CreateEventScreenTestTags
import com.android.festivar.ui.event.details.EventDetailsScreenTestTags
import com.android.festivar.ui.event.join.JoinEventScreenTestTags
import com.android.festivar.ui.event.overview.EventsOverviewScreenTestTags
import com.android.festivar.ui.profile.ProfileScreenTestTags
import com.android.festivar.ui.task.create.CreateTaskScreenTestTags
import com.android.festivar.ui.task.details.TaskDetailsScreenTestTags
import com.android.festivar.ui.task.edit.EditTaskScreenTestTags
import com.android.festivar.ui.task.mytasks.MyTasksScreenTestTags
import com.android.festivar.ui.task.overview.TaskOverviewScreenTestTags
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

  // The stored profile of the signed-in user
  private val storedUser = User(uid = signedInUid, name = "Sara", surname = "Keller")

  @Before
  fun setUp() {
    FirebaseEmulatorSetup.configure()
    MockKAnnotations.init(this, relaxUnitFun = true)

    UsersRepositoryProvider.repository = UsersRepositoryLocal(listOf(storedUser))

    firebaseUser = mockk(relaxed = true) { every { uid } returns testUserId }
  }

  private fun seedTask(
      taskId: String = "t1",
      eventId: String = "e1",
      title: String = "Test Task",
      ownerId: String = "owner-1",
  ) {
    val localRepo = TasksRepositoryLocal()
    val task = Task(taskId, eventId, title, ownerId = ownerId)
    runBlocking { localRepo.addTask(task) }
    TasksRepositoryProvider.repository = localRepo
  }

  @Test
  fun loginNavigatesToSignUpAndBackToLogin() {
    val authRepository = FakeAuthRepository(currentUser = null)

    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          dataRepository = DataRepositoryFirebase(authRepository = authRepository),
      )
    }

    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(LoginScreenTestTags.CREATE_ACC_BUTTON)
        .performScrollTo()
        .performClick()
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.CREATE_ACC_TITLE).assertIsDisplayed()

    composeTestRule.onNodeWithTag(SignUpScreenTestTags.LOGIN_NOW_BUTTON).performClick()
    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun loginNavigatesToForgotPasswordAndBackToLogin() {
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          dataRepository =
              DataRepositoryFirebase(authRepository = FakeAuthRepository(currentUser = null)),
      )
    }

    composeTestRule.onNodeWithText("Forgot password?").performClick()
    composeTestRule
        .onNodeWithText("Enter your email address to reset your password")
        .assertIsDisplayed()

    composeTestRule.onNodeWithText("Return to login").performClick()

    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun eventOverviewJoinNavigatesToJoinEvent() {
    seedNoEvents()
    setSignedInContent()

    // The new user has no events, so the "Join" button of the empty state shows once loaded
    waitUntilTagExists(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON)
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON).performClick()

    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun joinEventBackReturnsToEventsOverview() {
    seedNoEvents()
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
          dataRepository =
              DataRepositoryFirebase(authRepository = FakeAuthRepository(currentUser = null)),
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
    val owner = User(uid = "event-owner")
    val event =
        Event(
            eventId = "join-${UUID.randomUUID()}",
            ownerId = owner.uid,
            title = "Joinable Event",
            members = listOf(owner),
            startDate = start,
            endDate = start.plusHours(2),
        )
    // The joined user is written on the event as a member, and every later read of the events
    // resolves its members in Firestore, so the stored profile must exist there too
    val users = UsersRepositoryFirebase(Firebase.firestore)
    val events = EventRepositoryFirebase(Firebase.firestore)
    runBlocking {
      Firebase.auth.signInAnonymously().await()
      users.addUser(owner)
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
        runCatching { users.deleteUser(owner.uid) }
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
          dataRepository =
              DataRepositoryFirebase(authRepository = FakeAuthRepository(currentUser = null)),
          startDestinationOverride = AppScreens.EventsOverview.name,
      )
    }

    waitUntilTagExists(LoginScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun eventOverviewCreateNavigatesToCreateEventAndBack() {
    seedNoEvents()
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
            ownerId = storedUser.uid,
            title = "Fête de l'Asso",
            members = listOf(storedUser),
            startDate = now.minusHours(1),
            endDate = now.plusHours(5),
        )
    val users = UsersRepositoryFirebase(Firebase.firestore)
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
          dataRepository =
              DataRepositoryFirebase(
                  authRepository = FakeAuthRepository(currentUser = signedInUser)
              ),
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

  /** Turns red when back from the overview pops some other entry than the overview itself. */
  @Test
  fun backFromTheTaskOverviewReturnsToTheEventDetails() {
    // The see tasks button only shows once the event is read from the Firestore emulator, whose
    // rules need a signed-in user
    TasksRepositoryProvider.repository = TasksRepositoryLocal()
    val start = ZonedDateTime.now().plusDays(1)
    val event =
        Event(
            eventId = "back-${UUID.randomUUID()}",
            ownerId = storedUser.uid,
            title = "Event With Tasks",
            members = listOf(storedUser),
            startDate = start,
            endDate = start.plusHours(2),
        )
    val events = EventRepositoryFirebase(Firebase.firestore)
    val users = UsersRepositoryFirebase(Firebase.firestore)
    runBlocking {
      Firebase.auth.signInAnonymously().await()
      users.addUser(storedUser)
      events.addEvent(event)
    }

    try {
      composeTestRule.setContent {
        FestivAR(
            credentialManager = mockk<CredentialManager>(relaxed = true),
            dataRepository =
                DataRepositoryFirebase(
                    authRepository = FakeAuthRepository(currentUser = signedInUser)
                ),
            startDestinationOverride = AppScreens.eventDetails(event.eventId),
        )
      }

      waitUntilTagExists(EventDetailsScreenTestTags.SEE_TASKS_BUTTON)
      composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.SEE_TASKS_BUTTON).performClick()
      waitUntilTagExists(TaskOverviewScreenTestTags.BACK)
      composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.BACK).performClick()

      waitUntilTagExists(EventDetailsScreenTestTags.EVENT_NAME)
      composeTestRule
          .onNodeWithTag(EventDetailsScreenTestTags.EVENT_NAME)
          .assertTextEquals(event.title)
      composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.TITLE).assertDoesNotExist()
    } finally {
      runBlocking {
        runCatching { events.deleteEvent(event.eventId) }
        runCatching { users.deleteUser(storedUser.uid) }
        Firebase.auth.signOut()
      }
    }
  }

  @Test
  fun taskOverviewRouteDisplaysTaskOverviewScreen() {
    TasksRepositoryProvider.repository = TasksRepositoryLocal()
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          dataRepository =
              DataRepositoryFirebase(
                  authRepository = FakeAuthRepository(currentUser = firebaseUser)
              ),
          startDestinationOverride = AppScreens.taskOverview("e1"),
      )
    }

    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.TITLE).assertIsDisplayed()
  }

  /** Turns red when the overview is shown to a signed-out user instead of sending them to login. */
  @Test
  fun taskOverviewRouteRedirectsToLoginWhenUserIsNull() {
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          dataRepository =
              DataRepositoryFirebase(authRepository = FakeAuthRepository(currentUser = null)),
          startDestinationOverride = AppScreens.taskOverview("e1"),
      )
    }

    waitUntilTagExists(LoginScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.TITLE).assertDoesNotExist()
  }

  /** Turns red when the blank eventId check runs before the sign-in check and crashes the app. */
  @Test
  fun aSignedOutUserOnABlankOverviewRouteReachesTheLogin() {
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          dataRepository =
              DataRepositoryFirebase(authRepository = FakeAuthRepository(currentUser = null)),
          startDestinationOverride = AppScreens.taskOverview(" "),
      )
    }

    waitUntilTagExists(LoginScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun addTaskOnTheOverviewOpensCreateTaskAndTheNewTaskShowsOnReturn() {
    val tasks = TasksRepositoryLocal()
    TasksRepositoryProvider.repository = tasks
    // The create screen saves through the data repository, which also adds the task to its event
    // and to its owner: both must exist. The overview reads the same local tasks.
    val owner = User(testUserId, joinedEvents = listOf("e1"))
    val start = ZonedDateTime.now().plusDays(1)
    val event =
        Event(
            eventId = "e1",
            ownerId = owner.uid,
            title = "Summer festival",
            members = listOf(owner),
            startDate = start,
            endDate = start.plusDays(2),
        )
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          dataRepository =
              DataRepositoryLocal(
                  authRepository = FakeAuthRepository(currentUser = firebaseUser),
                  tasksRepository = tasks,
                  eventsRepository = EventsRepositoryLocal(listOf(event)),
                  usersRepository = UsersRepositoryLocal(listOf(owner)),
              ),
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
          dataRepository =
              DataRepositoryFirebase(
                  authRepository = FakeAuthRepository(currentUser = signedInUser)
              ),
          startDestinationOverride = AppScreens.CreateEvent.name,
      )
    }

    composeTestRule.onNodeWithTag(CreateEventScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun createEventRouteDisplaysLoginScreenIfFirebaseUserIsNull() {
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          dataRepository =
              DataRepositoryFirebase(authRepository = FakeAuthRepository(currentUser = null)),
          startDestinationOverride = AppScreens.CreateEvent.name,
      )
    }

    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun createTaskRouteDisplaysCreateTaskScreen() {
    TasksRepositoryProvider.repository = TasksRepositoryLocal()
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          dataRepository =
              DataRepositoryFirebase(
                  authRepository = FakeAuthRepository(currentUser = firebaseUser)
              ),
          startDestinationOverride = "${AppScreens.CreateTask.name}/e1",
      )
    }

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun createAnnouncementRouteDisplaysCreateAnnouncementScreen() {
    EventsRepositoryProvider.repository = EventsRepositoryLocal()
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          dataRepository =
              DataRepositoryFirebase(
                  authRepository = FakeAuthRepository(currentUser = firebaseUser)
              ),
          startDestinationOverride = AppScreens.createAnnouncement("e1"),
      )
    }

    composeTestRule.onNodeWithTag(CreateAnnouncementScreenTestTags.TITLE).assertIsDisplayed()
  }

  /** Turns red when the announcements row of the event details does not open that event's feed. */
  @Test
  fun announcementsRowOfTheEventDetailsOpensTheFeedOfTheEvent() {
    val start = ZonedDateTime.now().plusDays(1)
    val announcement =
        Announcement(
            announcementId = "a1",
            title = "Stage B is closed",
            authorId = "u0",
            updatedAt = ZonedDateTime.now(),
        )
    val event =
        Event(
            eventId = "e1",
            ownerId = "u0",
            title = "Balelec",
            startDate = start,
            endDate = start.plusHours(2),
            announcements = listOf(announcement),
        )
    EventsRepositoryProvider.repository = EventsRepositoryLocal(listOf(event))
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          dataRepository =
              DataRepositoryFirebase(
                  authRepository = FakeAuthRepository(currentUser = firebaseUser)
              ),
          startDestinationOverride = AppScreens.eventDetails(event.eventId),
      )
    }

    waitUntilTagExists(EventDetailsScreenTestTags.ANNOUNCEMENTS_ROW)
    composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.ANNOUNCEMENTS_ROW).performClick()

    waitUntilTagExists(AnnouncementFeedScreenTestTags.TITLE)
    // The feed shows the announcements of the event whose details were open
    waitUntilTagExists(AnnouncementFeedScreenTestTags.announcementRow(announcement))
  }

  @Test
  fun taskDetailsRouteDisplaysTaskDetailsScreen() {
    seedTask()

    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          // The task details read the task through the data repository, so it gets the seeded one.
          dataRepository =
              DataRepositoryLocal(
                  authRepository = FakeAuthRepository(currentUser = firebaseUser),
                  tasksRepository = TasksRepositoryProvider.repository,
              ),
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
          dataRepository =
              DataRepositoryFirebase(
                  authRepository = FakeAuthRepository(currentUser = firebaseUser)
              ),
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
          dataRepository =
              DataRepositoryFirebase(authRepository = FakeAuthRepository(currentUser = null)),
          startDestinationOverride = AppScreens.taskDetails("t1"),
      )
    }

    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
  }

  // Breaks if the edit route opens for nobody signed in instead of sending them to the login.
  @Test
  fun editTaskRouteRedirectsToLoginWhenUserIsNull() {
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          dataRepository =
              DataRepositoryFirebase(authRepository = FakeAuthRepository(currentUser = null)),
          startDestinationOverride = AppScreens.editTask("t1"),
      )
    }

    waitUntilTagExists(LoginScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
  }

  // Breaks if the details edit button is not wired to the edit route, or back from it does not
  // return to the details.
  @Test
  fun taskDetailsEditOpensTheEditScreenAndBackReturns() {
    openTheEditOfAnOwnedTaskFromItsDetails()

    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.BACK_BUTTON).performClick()
    waitUntilTagExists(TaskDetailsScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TITLE).assertIsDisplayed()
  }

  // Breaks if the details do not reload on resume, so a saved edit is not shown on return.
  @Test
  fun savingTheTitleShowsItOnTheDetailsOnReturn() {
    openTheEditOfAnOwnedTaskFromItsDetails()

    composeTestRule
        .onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD)
        .performTextReplacement("Hang the banner")
    composeTestRule.onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON).performClick()

    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      composeTestRule.onAllNodesWithTag(TaskDetailsScreenTestTags.TITLE).fetchSemanticsNodes().any {
        it.config.getOrNull(SemanticsProperties.Text)?.joinToString("") == "Hang the banner"
      }
    }
    composeTestRule
        .onNodeWithTag(TaskDetailsScreenTestTags.TITLE)
        .assertIsDisplayed()
        .assertTextEquals("Hang the banner")
  }

  /** Opens the details of a task the signed-in [firebaseUser] owns, then its edit screen. */
  private fun openTheEditOfAnOwnedTaskFromItsDetails() {
    seedTask(ownerId = testUserId)
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          dataRepository =
              DataRepositoryFirebase(
                  authRepository = FakeAuthRepository(currentUser = firebaseUser)
              ),
          startDestinationOverride = AppScreens.taskDetails("t1"),
      )
    }

    waitUntilTagExists(TaskDetailsScreenTestTags.MORE_ACTIONS_BUTTON)
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.MORE_ACTIONS_BUTTON).performClick()
    composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.EDIT_MENU_ITEM).performClick()
    waitUntilTagExists(CreateTaskScreenTestTags.TITLE_FIELD)
    composeTestRule.onNodeWithTag(EditTaskScreenTestTags.SCREEN).assertIsDisplayed()
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
          dataRepository =
              DataRepositoryFirebase(
                  authRepository = FakeAuthRepository(currentUser = firebaseUser)
              ),
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
          dataRepository =
              DataRepositoryFirebase(
                  authRepository = FakeAuthRepository(currentUser = firebaseUser)
              ),
          startDestinationOverride = AppScreens.Profile.name,
      )
    }

    // The sign out button only shows once the profile load ends
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
          dataRepository =
              DataRepositoryFirebase(authRepository = FakeAuthRepository(currentUser = null)),
          startDestinationOverride = AppScreens.Profile.name,
      )
    }

    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
  }

  /**
   * Gives the signed-in user no events, kept in memory: the Firestore emulator only answers a user
   * signed in to Firebase, which [signedInUser] is not, so the overview would show its error state
   * instead of its empty state.
   */
  private fun seedNoEvents() {
    EventsRepositoryProvider.repository = EventsRepositoryLocal()
  }

  // Breaks if the bar shows over the authentication screens.
  @Test
  fun theBarIsHiddenOnTheLogin() {
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          dataRepository =
              DataRepositoryFirebase(authRepository = FakeAuthRepository(currentUser = null)),
      )
    }

    waitUntilTagExists(LoginScreenTestTags.TITLE)
    composeTestRule
        .onNodeWithTag(BottomNavigationBarTestTags.BOTTOM_NAVIGATION_BAR)
        .assertDoesNotExist()
  }

  // Breaks if the overview loses its bar, selects another tab or shows a sub-screen disc.
  @Test
  fun theEventsOverviewShowsTheBarWithEventsSelected() {
    seedNoEvents()
    setSignedInContent()

    waitUntilTagExists(BottomNavigationBarTestTags.BOTTOM_NAVIGATION_BAR)
    composeTestRule.onNodeWithTag(tabTag(NavigationTab.EVENTS)).assertIsSelected()
    NavigationSubScreen.entries.forEach { disc(it).assertDoesNotExist() }
  }

  // Breaks if the task board of an event loses the tasks disc on the Events tab or selects another
  // tab.
  @Test
  fun theTaskOverviewNestsEventsUnderTheTaskBoard() {
    TasksRepositoryProvider.repository = TasksRepositoryLocal()
    setContentStartingOn(AppScreens.taskOverview("e1"))

    waitUntilTagExists(BottomNavigationBarTestTags.BOTTOM_NAVIGATION_BAR)
    disc(NavigationSubScreen.EVENT_TASKS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(tabTag(NavigationTab.EVENTS)).assertIsSelected()
    composeTestRule.onNodeWithTag(tabTag(NavigationTab.MY_TASKS)).assertIsNotSelected()
    composeTestRule.onNodeWithTag(tabTag(NavigationTab.PROFILE)).assertIsNotSelected()
  }

  // Breaks if tapping Events on the task board no longer returns to the events overview.
  @Test
  fun reselectingEventsOnTheTaskBoardReturnsToTheOverview() {
    seedNoEvents()
    TasksRepositoryProvider.repository = TasksRepositoryLocal()
    setContentStartingOn(AppScreens.taskOverview("e1"))

    waitUntilTagExists(BottomNavigationBarTestTags.BOTTOM_NAVIGATION_BAR)
    composeTestRule.onNodeWithTag(tabTag(NavigationTab.EVENTS)).performClick()

    waitUntilTagExists(EventsOverviewScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.TITLE).assertIsDisplayed()
    disc(NavigationSubScreen.EVENT_TASKS).assertDoesNotExist()
  }

  // Breaks if a task opened from My tasks falls back to the Events tab or loses the task disc.
  @Test
  fun aTaskOpenedFromMyTasksKeepsMyTasksSelected() {
    seedTask()
    setContentStartingOn(AppScreens.taskDetails("t1", TaskOrigin.MY_TASKS))

    waitUntilTagExists(BottomNavigationBarTestTags.BOTTOM_NAVIGATION_BAR)
    composeTestRule.onNodeWithTag(tabTag(NavigationTab.MY_TASKS)).assertIsSelected()
    disc(NavigationSubScreen.TASK_DETAILS).assertIsDisplayed()
  }

  // Breaks if a task opened from an event leaves the Events tab or loses the task disc.
  @Test
  fun aTaskOpenedFromAnEventNestsEventsUnderTheTask() {
    seedTask()
    setContentStartingOn(AppScreens.taskDetails("t1"))

    waitUntilTagExists(BottomNavigationBarTestTags.BOTTOM_NAVIGATION_BAR)
    disc(NavigationSubScreen.TASK_DETAILS).assertIsDisplayed()
    composeTestRule.onNodeWithTag(tabTag(NavigationTab.EVENTS)).assertIsSelected()
  }

  // Breaks if the bar shows over a form.
  @Test
  fun theCreateTaskRouteHidesTheBar() {
    TasksRepositoryProvider.repository = TasksRepositoryLocal()
    setContentStartingOn(AppScreens.createTask("e1"))

    waitUntilTagExists(CreateTaskScreenTestTags.TITLE)
    composeTestRule
        .onNodeWithTag(BottomNavigationBarTestTags.BOTTOM_NAVIGATION_BAR)
        .assertDoesNotExist()
  }

  // Breaks if the My tasks tab has no destination or the Events tab does not return.
  @Test
  fun theMyTasksTabOpensMyTasksAndTheEventsTabReturns() {
    seedNoEvents()
    setSignedInContent()

    waitUntilTagExists(BottomNavigationBarTestTags.BOTTOM_NAVIGATION_BAR)
    composeTestRule.onNodeWithTag(tabTag(NavigationTab.MY_TASKS)).performClick()
    waitUntilTagExists(MyTasksScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(MyTasksScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(tabTag(NavigationTab.MY_TASKS)).assertIsSelected()

    composeTestRule.onNodeWithTag(tabTag(NavigationTab.EVENTS)).performClick()
    waitUntilTagExists(EventsOverviewScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.TITLE).assertIsDisplayed()
  }

  // Breaks if the Profile tab does not open the profile.
  @Test
  fun theProfileTabOpensTheProfile() {
    seedNoEvents()
    setSignedInContent()

    waitUntilTagExists(BottomNavigationBarTestTags.BOTTOM_NAVIGATION_BAR)
    composeTestRule.onNodeWithTag(tabTag(NavigationTab.PROFILE)).performClick()

    waitUntilTagExists(ProfileScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.TITLE).assertIsDisplayed()
  }

  private fun tabTag(tab: NavigationTab) = BottomNavigationBarTestTags.getTestTagForTab(tab)

  private fun disc(subScreen: NavigationSubScreen) =
      composeTestRule.onNodeWithTag(
          BottomNavigationBarTestTags.getTestTagForSubScreen(subScreen),
          useUnmergedTree = true,
      )

  /** Shows the app as [firebaseUser] on [route], reading the tasks from the provider. */
  private fun setContentStartingOn(route: String) {
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          dataRepository =
              DataRepositoryLocal(
                  authRepository = FakeAuthRepository(currentUser = firebaseUser),
                  tasksRepository = TasksRepositoryProvider.repository,
              ),
          startDestinationOverride = route,
      )
    }
  }

  @After
  fun tearDown() {
    TasksRepositoryProvider.reset()
    UsersRepositoryProvider.reset()
    EventsRepositoryProvider.reset()
  }

  /** Shows the app as [signedInUser], so it starts on the events overview. */
  private fun setSignedInContent() {
    composeTestRule.setContent {
      FestivAR(
          credentialManager = mockk<CredentialManager>(relaxed = true),
          dataRepository =
              DataRepositoryFirebase(
                  authRepository = FakeAuthRepository(currentUser = signedInUser)
              ),
      )
    }
  }

  private fun waitUntilTagExists(tag: String) {
    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
  }
}
