// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventRepositoryFirebase
import com.android.festivar.model.event.joinCodeOf
import com.android.festivar.model.task.TaskRepositoryFirebase
import com.android.festivar.model.user.UsersRepositoryFirebase
import com.android.festivar.model.user.UsersRepositoryProvider
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
import com.android.festivar.ui.task.mytasks.MyTasksPlaceholderTestTags
import com.android.festivar.ui.task.overview.TaskOverviewScreenTestTags
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.AfterClass
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The end-to-end journeys of the app, on the real [MainActivity] with the real
 * `AuthRepositoryFirebase` and the real Firestore repositories, all talking to the Firebase
 * emulators: no fake repository and no start destination override.
 *
 * The member journey: sign up, join a seeded event by its code, open it, see its tasks, add one,
 * take it, sign out. The organizer journey: sign up, create an event, find it in the overview, open
 * it. The rule launches the activity before any `@Before`, so the emulators are configured and the
 * event is seeded in the class level [seedTheEvent], and everything the journeys created is removed
 * in [cleanUp]. Each journey ends signed out so the next starts on the login screen.
 *
 * The M2 journey: a member joins the event, opens its task board with the bottom bar, takes a task,
 * finds it in My tasks and gives it back after the confirmation.
 */
@RunWith(AndroidJUnit4::class)
class EndToEndTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun aNewMemberSignsUpJoinsAnEventByItsCodeAddsATaskTakesItAndSignsOut() {
    try {
      signUp(MEMBER_EMAIL)

      // A new member has no event yet, so the overview shows its empty state with the join button
      waitUntilTagExists(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON)
      composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON).performClick()

      waitUntilTagExists(JoinEventScreenTestTags.TITLE)
      composeTestRule
          .onNodeWithTag(JoinEventScreenTestTags.CODE_INPUT)
          .performTextInput(joinCodeOf(event))
      waitUntilTagExists(JoinEventScreenTestTags.join(event.eventId))
      composeTestRule.onNodeWithTag(JoinEventScreenTestTags.join(event.eventId)).performClick()

      // The joined event opens on its details
      waitUntilTagExists(EventDetailsScreenTestTags.EVENT_NAME)
      composeTestRule
          .onNodeWithTag(EventDetailsScreenTestTags.EVENT_NAME)
          .assertTextEquals(event.title)

      // Its tasks: none yet, so the member adds one, saves it and finds it listed
      composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.SEE_TASKS_BUTTON).performClick()
      waitUntilTagExists(TaskOverviewScreenTestTags.ADD_TASK)
      composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.ADD_TASK).performClick()
      waitUntilTagExists(CreateTaskScreenTestTags.TITLE_FIELD)
      composeTestRule
          .onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD)
          .performTextInput(TASK_TITLE)
      // The create button sits in the bottom bar, outside the scrolling form
      Espresso.closeSoftKeyboard()
      composeTestRule
          .onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON)
          .assertIsEnabled()
          .performClick()
      waitUntilTagExists(TaskOverviewScreenTestTags.TITLE)
      waitUntilTextExists(TASK_TITLE)

      // The task opens on its details, and taking it offers to give it back
      composeTestRule.onNodeWithText(TASK_TITLE).performClick()
      waitUntilTagExists(TaskDetailsScreenTestTags.TAKE_BUTTON)
      composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TAKE_BUTTON).performClick()
      waitUntilTagExists(TaskDetailsScreenTestTags.GIVE_BACK_BUTTON)

      // Back through the overview and the details to the events, where the event is now listed
      // under the upcoming chip; the keyboard is closed first, or back would only dismiss it
      Espresso.closeSoftKeyboard()
      Espresso.pressBack()
      waitUntilTagExists(TaskOverviewScreenTestTags.TITLE)
      Espresso.pressBack()
      waitUntilTagExists(EventDetailsScreenTestTags.EVENT_NAME)
      Espresso.pressBack()
      waitUntilTagExists(EventsOverviewScreenTestTags.UPCOMING_CHIP)
      composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.UPCOMING_CHIP).performClick()
      waitUntilTagExists(EventsOverviewScreenTestTags.getTestTagForEventItem(event))

      signOutFromTheProfile(MEMBER_EMAIL)

      // Signing back in through the login form lists the joined event again
      logIn(MEMBER_EMAIL)
      composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.UPCOMING_CHIP).performClick()
      waitUntilTagExists(EventsOverviewScreenTestTags.getTestTagForEventItem(event))
      signOutFromTheProfile(MEMBER_EMAIL)

      // The join and the task are stored: the event lists the member, the member lists the event,
      // and the task belongs to the event with the member on it
      asTheUser(MEMBER_EMAIL) { uid ->
        val stored = EventRepositoryFirebase(Firebase.firestore).getEvent(event.eventId)
        assertTrue("The event does not list the new member", stored.members.any { it.uid == uid })
        val user = UsersRepositoryFirebase(Firebase.firestore).getUser(uid)
        assertTrue("The member does not list the joined event", event.eventId in user.joinedEvents)
        val tasks = TaskRepositoryFirebase(Firebase.firestore).getAllTasks(event.eventId)
        val task = tasks.single { it.title == TASK_TITLE }
        assertTrue("The task is not taken by the member", task.assignees.any { it.uid == uid })
      }
    } finally {
      Firebase.auth.signOut()
    }
  }

  /**
   * Walks the M2 path as a new member: sign up, join the seeded event, open its task board and
   * check the bottom bar (Events nested under the tasks disc), add a task and open it, take it and see
   * the "On this task" section, visit My tasks, come back, give the task back after keeping it
   * once, and sign out through the Profile tab. It proves the bar follows the member through the
   * board and the task, that taking and giving back are stored, and that the confirmation can be
   * declined. It turns red if a tag disappears, the bar highlights a tab inside the board, the
   * section outlives the give back, or the assignee is still stored at the end.
   */
  @Test
  fun aMemberWalksTheM2PathBoardTaskBottomBarTakeAndGiveBack() {
    try {
      signUp(M2_MEMBER_EMAIL)

      waitUntilTagExists(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON)
      composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON).performClick()
      waitUntilTagExists(JoinEventScreenTestTags.TITLE)
      composeTestRule
          .onNodeWithTag(JoinEventScreenTestTags.CODE_INPUT)
          .performTextInput(joinCodeOf(event))
      waitUntilTagExists(JoinEventScreenTestTags.join(event.eventId))
      composeTestRule.onNodeWithTag(JoinEventScreenTestTags.join(event.eventId)).performClick()

      waitUntilTagExists(EventDetailsScreenTestTags.EVENT_NAME)
      composeTestRule
          .onNodeWithTag(EventDetailsScreenTestTags.EVENT_NAME)
          .assertTextEquals(event.title)
      // To add once the event details show it: check that the join code is visible

      // The board: the bar nests the Events tab under the tasks disc
      composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.SEE_TASKS_BUTTON).performClick()
      waitUntilTagExists(TaskOverviewScreenTestTags.ADD_TASK)
      assertTheBoardBar()

      // The M1 journey may have left its own task here, so the board is not assumed empty
      composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.ADD_TASK).performClick()
      waitUntilTagExists(CreateTaskScreenTestTags.TITLE_FIELD)
      composeTestRule
          .onNodeWithTag(CreateTaskScreenTestTags.TITLE_FIELD)
          .performTextInput(M2_TASK_TITLE)
      Espresso.closeSoftKeyboard()
      composeTestRule
          .onNodeWithTag(CreateTaskScreenTestTags.CREATE_BUTTON)
          .assertIsEnabled()
          .performClick()
      waitUntilTagExists(TaskOverviewScreenTestTags.TITLE)
      waitUntilTextExists(M2_TASK_TITLE)

      composeTestRule.onNodeWithText(M2_TASK_TITLE).performClick()
      waitUntilTagExists(TaskDetailsScreenTestTags.TAKE_BUTTON)
      assertTheBarNests(NavigationTab.EVENTS, NavigationSubScreen.TASK_DETAILS)

      // Taking the task shows who is on it
      composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.TAKE_BUTTON).performClick()
      waitUntilTagExists(TaskDetailsScreenTestTags.GIVE_BACK_BUTTON)
      composeTestRule
          .onNodeWithTag(TaskDetailsScreenTestTags.ON_THIS_TASK_SECTION)
          .performScrollTo()
          .assertIsDisplayed()
      composeTestRule
          .onNodeWithTag(TaskDetailsScreenTestTags.ON_THIS_TASK_COUNT, useUnmergedTree = true)
          .performScrollTo()
          .assertTextEquals("1 person")

      // My tasks is a placeholder until #397, with its tab selected
      composeTestRule
          .onNodeWithTag(BottomNavigationBarTestTags.getTestTagForTab(NavigationTab.MY_TASKS))
          .performClick()
      waitUntilTagExists(MyTasksPlaceholderTestTags.SCREEN)
      composeTestRule
          .onNodeWithTag(BottomNavigationBarTestTags.getTestTagForTab(NavigationTab.MY_TASKS))
          .assertIsSelected()
      // To add once My tasks lists tasks (#397): check that the taken task appears in the list

      // Back from a tab returns to the events overview, then down to the task again
      Espresso.pressBack()
      waitUntilTagExists(EventsOverviewScreenTestTags.UPCOMING_CHIP)
      composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.UPCOMING_CHIP).performClick()
      waitUntilTagExists(EventsOverviewScreenTestTags.getTestTagForEventItem(event))
      composeTestRule
          .onNodeWithTag(EventsOverviewScreenTestTags.getTestTagForEventItem(event))
          .performClick()
      waitUntilTagExists(EventDetailsScreenTestTags.SEE_TASKS_BUTTON)
      composeTestRule.onNodeWithTag(EventDetailsScreenTestTags.SEE_TASKS_BUTTON).performClick()
      // The board opens on the Open chip, and the task is now mine
      waitUntilTagExists(TaskOverviewScreenTestTags.CHIP_MINE)
      composeTestRule.onNodeWithTag(TaskOverviewScreenTestTags.CHIP_MINE).performClick()
      waitUntilTextExists(M2_TASK_TITLE)
      composeTestRule.onNodeWithText(M2_TASK_TITLE).performClick()
      waitUntilTagExists(TaskDetailsScreenTestTags.GIVE_BACK_BUTTON)

      // To add once the owner menu exists (#399, PR #440): as the organizer, delete a task after
      // the confirmation

      // Giving back asks first: keeping the task leaves it taken, confirming frees it
      composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_BUTTON).performClick()
      waitUntilTagExists(TaskDetailsScreenTestTags.GIVE_BACK_DIALOG)
      composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_KEEP_BUTTON).performClick()
      composeTestRule.waitUntil(timeoutMillis = TIMEOUT) {
        composeTestRule
            .onAllNodesWithTag(TaskDetailsScreenTestTags.GIVE_BACK_DIALOG)
            .fetchSemanticsNodes()
            .isEmpty()
      }
      composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_BUTTON).assertIsDisplayed()
      composeTestRule.onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_BUTTON).performClick()
      waitUntilTagExists(TaskDetailsScreenTestTags.GIVE_BACK_CONFIRM_BUTTON)
      composeTestRule
          .onNodeWithTag(TaskDetailsScreenTestTags.GIVE_BACK_CONFIRM_BUTTON)
          .performClick()
      waitUntilTagExists(TaskDetailsScreenTestTags.TAKE_BUTTON)
      composeTestRule
          .onNodeWithTag(TaskDetailsScreenTestTags.ON_THIS_TASK_SECTION)
          .assertDoesNotExist()

      signOutFromTheProfile(M2_MEMBER_EMAIL)

      // The task is free again and the member still lists the event
      asTheUser(M2_MEMBER_EMAIL) { uid ->
        val tasks = TaskRepositoryFirebase(Firebase.firestore).getAllTasks(event.eventId)
        val task = tasks.single { it.title == M2_TASK_TITLE }
        assertTrue("The task still has an assignee", task.assignees.isEmpty())
        val user = UsersRepositoryFirebase(Firebase.firestore).getUser(uid)
        assertTrue("The member does not list the joined event", event.eventId in user.joinedEvents)
      }
    } finally {
      Firebase.auth.signOut()
    }
  }

  @Test
  fun aNewOrganizerSignsUpCreatesAnEventAndFindsIt() {
    try {
      signUp(ORGANIZER_EMAIL)

      waitUntilTagExists(EventsOverviewScreenTestTags.CREATE_EVENT_BUTTON)
      composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.CREATE_EVENT_BUTTON).performClick()
      waitUntilTagExists(CreateEventScreenTestTags.NAME_FIELD)
      composeTestRule
          .onNodeWithTag(CreateEventScreenTestTags.NAME_FIELD)
          .performTextInput(CREATED_EVENT_TITLE)
      Espresso.closeSoftKeyboard()
      selectDate(CreateEventScreenTestTags.START_DATE_FIELD, LocalDate.now().plusDays(1))
      selectDate(CreateEventScreenTestTags.END_DATE_FIELD, LocalDate.now().plusDays(2))
      composeTestRule
          .onNodeWithTag(CreateEventScreenTestTags.LOCATION_FIELD)
          .performTextInput("Vidy")
      Espresso.closeSoftKeyboard()
      composeTestRule.onNodeWithTag(CreateEventScreenTestTags.CREATE_BUTTON).performClick()

      // A saved event returns to the overview and is listed under the upcoming chip
      waitUntilTagExists(EventsOverviewScreenTestTags.UPCOMING_CHIP)
      composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.UPCOMING_CHIP).performClick()
      waitUntilTextExists(CREATED_EVENT_TITLE)
      composeTestRule.onNodeWithText(CREATED_EVENT_TITLE).performClick()
      waitUntilTagExists(EventDetailsScreenTestTags.EVENT_NAME)
      composeTestRule
          .onNodeWithTag(EventDetailsScreenTestTags.EVENT_NAME)
          .assertTextEquals(CREATED_EVENT_TITLE)
      Espresso.pressBack()
      waitUntilTagExists(EventsOverviewScreenTestTags.TITLE)

      signOutFromTheProfile(ORGANIZER_EMAIL)

      // The organizer is the first member of the event they created
      asTheUser(ORGANIZER_EMAIL) { uid ->
        val created =
            EventRepositoryFirebase(Firebase.firestore).getAllEvents().single {
              it.title == CREATED_EVENT_TITLE
            }
        assertTrue("The creator is not a member", created.members.any { it.uid == uid })
      }
    } finally {
      Firebase.auth.signOut()
    }
  }

  /** Creates the account [email] from the login screen and lands on the events overview. */
  private fun signUp(email: String) {
    waitUntilTagExists(LoginScreenTestTags.TITLE)
    composeTestRule
        .onNodeWithTag(LoginScreenTestTags.CREATE_ACC_BUTTON)
        .performScrollTo()
        .performClick()

    waitUntilTagExists(SignUpScreenTestTags.EMAIL_FIELD)
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.EMAIL_FIELD).performTextInput(email)
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.PASS_FIELD).performTextInput(PASSWORD)
    composeTestRule
        .onNodeWithTag(SignUpScreenTestTags.PASS_CONFIRM_FIELD)
        .performTextInput(PASSWORD)
    // The keyboard covers the button, which sits in a scrolling column under the fields
    Espresso.closeSoftKeyboard()
    composeTestRule
        .onNodeWithTag(SignUpScreenTestTags.SIGNUP_BUTTON)
        .assertIsEnabled()
        .performScrollTo()
        .performClick()
    waitUntilTagExists(EventsOverviewScreenTestTags.TITLE)
  }

  /** Signs in as the existing account [email] from the login form, to the events overview. */
  private fun logIn(email: String) {
    waitUntilTagExists(LoginScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(LoginScreenTestTags.EMAIL_INPUT).performTextInput(email)
    composeTestRule.onNodeWithTag(LoginScreenTestTags.PASSWORD_INPUT).performTextInput(PASSWORD)
    Espresso.closeSoftKeyboard()
    composeTestRule.onNodeWithTag(LoginScreenTestTags.LOGIN_BUTTON).assertIsEnabled().performClick()
    waitUntilTagExists(EventsOverviewScreenTestTags.TITLE)
  }

  /** On the board, the bar nests the Events tab under the tasks disc. */
  private fun assertTheBoardBar() {
    composeTestRule
        .onNodeWithTag(BottomNavigationBarTestTags.BOTTOM_NAVIGATION_BAR)
        .assertIsDisplayed()
    assertTheBarNests(NavigationTab.EVENTS, NavigationSubScreen.EVENT_TASKS)
  }

  /** The bar shows [subScreen]'s disc on [tab], which is the only selected tab. */
  private fun assertTheBarNests(tab: NavigationTab, subScreen: NavigationSubScreen) {
    composeTestRule
        .onNodeWithTag(
            BottomNavigationBarTestTags.getTestTagForSubScreen(subScreen),
            useUnmergedTree = true,
        )
        .assertIsDisplayed()
    for (other in NavigationTab.entries) {
      val node = composeTestRule.onNodeWithTag(BottomNavigationBarTestTags.getTestTagForTab(other))
      if (other == tab) node.assertIsSelected() else node.assertIsNotSelected()
    }
  }

  /**
   * Opens the profile of [email] from the Profile tab of the bottom bar and signs out, back to the
   * login screen. Both journeys use the tab, which every screen with a bar offers.
   */
  private fun signOutFromTheProfile(email: String) {
    composeTestRule
        .onNodeWithTag(BottomNavigationBarTestTags.getTestTagForTab(NavigationTab.PROFILE))
        .performClick()
    waitUntilTagExists(ProfileScreenTestTags.TITLE)
    // The sign out button only shows once the profile is loaded
    waitUntilTagExists(ProfileScreenTestTags.SIGN_OUT_BUTTON)
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.EMAIL).assertTextEquals(email)
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SIGN_OUT_BUTTON).performClick()
    waitUntilTagExists(LoginScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()
  }

  /** Picks [date] in the dialog the field [tag] opens, as the create event screen test does. */
  private fun selectDate(tag: String, date: LocalDate) {
    composeTestRule.onNodeWithTag(tag).performClick()
    waitUntilTextExists("OK")
    val dateDescription =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
            .withLocale(Locale.getDefault())
            .format(date)
    // The dialog shows one month, so a date near a month end can sit in the next one
    if (
        composeTestRule
            .onAllNodesWithText(dateDescription, substring = true)
            .fetchSemanticsNodes()
            .isEmpty()
    ) {
      composeTestRule
          .onNodeWithContentDescription("next month", substring = true, ignoreCase = true)
          .performClick()
    }
    composeTestRule.onNodeWithText(dateDescription, substring = true).performClick()
    composeTestRule.onNodeWithText("OK").performClick()
  }

  private fun waitUntilTagExists(tag: String, timeout: Long = TIMEOUT) {
    composeTestRule.waitUntil(timeoutMillis = timeout) {
      composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
  }

  private fun waitUntilTextExists(text: String, timeout: Long = TIMEOUT) {
    composeTestRule.waitUntil(timeoutMillis = timeout) {
      composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
  }

  companion object {
    private const val TAG = "EndToEndTest"
    // A real sign up and the Firestore round trips are slower than the fake auth tests
    private const val TIMEOUT = 15_000L
    private const val PASSWORD = "Password123!"
    private const val TASK_TITLE = "Hang the banner"
    private const val M2_TASK_TITLE = "Stack the chairs"
    private const val CREATED_EVENT_TITLE = "Vidy Afterparty"
    private val MEMBER_EMAIL = "e2e-member-${UUID.randomUUID()}@festivar.test"
    private val M2_MEMBER_EMAIL = "e2e-m2-member-${UUID.randomUUID()}@festivar.test"
    private val ORGANIZER_EMAIL = "e2e-organizer-${UUID.randomUUID()}@festivar.test"

    private val start = ZonedDateTime.now().plusDays(1)
    private val event =
        Event(
            eventId = "e2e-${UUID.randomUUID()}",
            title = "Vidy Festival",
            startDate = start,
            endDate = start.plusHours(5),
        )

    /** Runs [block] as a throwaway anonymous user, as the Firestore rules need a signed-in one. */
    private fun asAnonymousUser(block: suspend () -> Unit) = runBlocking {
      Firebase.auth.signInAnonymously().await()
      try {
        block()
      } finally {
        logged("delete the anonymous user") { Firebase.auth.currentUser?.delete()?.await() }
        Firebase.auth.signOut()
      }
    }

    /**
     * Signs in as the account [email] a journey created, straight through Firebase Auth so no
     * profile is recreated, and runs [block] with its uid; the uid comes from the sign in, not from
     * the test.
     */
    private fun asTheUser(email: String, block: suspend (uid: String) -> Unit) = runBlocking {
      val uid =
          checkNotNull(Firebase.auth.signInWithEmailAndPassword(email, PASSWORD).await().user?.uid)
      try {
        block(uid)
      } finally {
        Firebase.auth.signOut()
      }
    }

    /** Runs a cleanup step and logs a failure instead of hiding it, so residue can be traced. */
    private suspend fun logged(step: String, action: suspend () -> Unit) {
      runCatching { action() }.onFailure { Log.w(TAG, "Could not $step", it) }
    }

    @JvmStatic
    @BeforeClass
    fun seedTheEvent() {
      FirebaseEmulatorSetup.configure()
      // Another test class may have left a fake users repository or a signed in user behind
      UsersRepositoryProvider.reset()
      Firebase.auth.signOut()

      // Clean up any potential leftover test accounts or events from previous runs
      asAnonymousUser {
        val events = EventRepositoryFirebase(Firebase.firestore)
        val tasks = TaskRepositoryFirebase(Firebase.firestore)
        logged("delete leftover seeded event's tasks") {
          runCatching { tasks.getAllTasks(event.eventId).forEach { tasks.deleteTask(it.taskId) } }
        }
        logged("delete leftover seeded event") { runCatching { events.deleteEvent(event.eventId) } }
        logged("delete leftover created events") {
          runCatching {
            events
                .getAllEvents()
                .filter { it.title == CREATED_EVENT_TITLE }
                .forEach { events.deleteEvent(it.eventId) }
          }
        }
      }
      for (email in listOf(MEMBER_EMAIL, M2_MEMBER_EMAIL, ORGANIZER_EMAIL)) {
        runCatching {
          asTheUser(email) { uid ->
            logged("delete leftover profile of $email") {
              UsersRepositoryFirebase(Firebase.firestore).deleteUser(uid)
            }
            logged("delete leftover account of $email") {
              Firebase.auth.currentUser?.delete()?.await()
            }
          }
        }
      }
      Firebase.auth.signOut()

      asAnonymousUser { EventRepositoryFirebase(Firebase.firestore).addEvent(event) }
      // Signed out, so the app starts on the login screen
    }

    @JvmStatic
    @AfterClass
    fun cleanUp() {
      // Tasks and events go first, so nothing is left pointing at a deleted member
      asAnonymousUser {
        val events = EventRepositoryFirebase(Firebase.firestore)
        val tasks = TaskRepositoryFirebase(Firebase.firestore)
        logged("delete the seeded event's tasks") {
          tasks.getAllTasks(event.eventId).forEach { tasks.deleteTask(it.taskId) }
        }
        logged("delete the seeded event") { events.deleteEvent(event.eventId) }
        logged("delete the created event") {
          events
              .getAllEvents()
              .filter { it.title == CREATED_EVENT_TITLE }
              .forEach { events.deleteEvent(it.eventId) }
        }
      }
      // An Auth account can only be deleted by itself, so each member signs in once more
      for (email in listOf(MEMBER_EMAIL, M2_MEMBER_EMAIL, ORGANIZER_EMAIL)) {
        runCatching {
          asTheUser(email) { uid ->
            logged("delete the profile of $email") {
              UsersRepositoryFirebase(Firebase.firestore).deleteUser(uid)
            }
            logged("delete the account of $email") { Firebase.auth.currentUser?.delete()?.await() }
          }
        }
            .onFailure { Log.w(TAG, "$email could not sign in to clean up", it) }
      }
    }
  }
}
