// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar

import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
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
import com.android.festivar.model.user.UserRepositoryFirebase
import com.android.festivar.model.user.UsersRepositoryProvider
import com.android.festivar.ui.authentication.login.LoginScreenTestTags
import com.android.festivar.ui.authentication.signup.SignUpScreenTestTags
import com.android.festivar.ui.event.EventsOverviewScreenTestTags
import com.android.festivar.ui.event.JoinEventScreenTestTags
import com.android.festivar.ui.profile.ProfileScreenTestTags
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.time.ZonedDateTime
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
 * The milestone 1 end-to-end journey: a new member signs up, joins an event by its code, finds it
 * in the overview and signs out.
 *
 * It runs on the real [MainActivity] with the real `AuthRepositoryFirebase` and the real Firestore
 * repositories, all talking to the Firebase emulators: no fake repository and no start destination
 * override. The rule launches the activity before any `@Before`, so the emulators are configured
 * and the event is seeded in the class level [seedTheEvent], and everything the journey created is
 * removed in [cleanUp].
 */
@RunWith(AndroidJUnit4::class)
class EndToEndTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun aNewMemberSignsUpJoinsAnEventByItsCodeAndSignsOut() {
    waitUntilTagExists(LoginScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(LoginScreenTestTags.CREATE_ACC_BUTTON).performClick()

    waitUntilTagExists(SignUpScreenTestTags.EMAIL_FIELD)
    composeTestRule.onNodeWithTag(SignUpScreenTestTags.EMAIL_FIELD).performTextInput(EMAIL)
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

    // A new member has no event yet, so the overview shows its empty state with the join button
    waitUntilTagExists(EventsOverviewScreenTestTags.TITLE)
    waitUntilTagExists(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON)
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.JOIN_EVENT_BUTTON).performClick()

    waitUntilTagExists(JoinEventScreenTestTags.TITLE)
    composeTestRule
        .onNodeWithTag(JoinEventScreenTestTags.CODE_INPUT)
        .performTextInput(joinCodeOf(event))
    waitUntilTagExists(JoinEventScreenTestTags.join(event.eventId))
    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.join(event.eventId)).performClick()

    // The event details route is still a placeholder: this text becomes the EventDetailsScreen
    // title tag once that screen is routed
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) {
      composeTestRule
          .onAllNodesWithText(EVENT_DETAILS_PLACEHOLDER)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithText(EVENT_DETAILS_PLACEHOLDER).assertIsDisplayed()

    // The join screen was dropped from the back stack, so back lands on the overview, where the
    // event starting tomorrow is listed under the upcoming chip, not the default ongoing one. The
    // keyboard is closed first, or back would only dismiss it.
    Espresso.closeSoftKeyboard()
    Espresso.pressBack()
    waitUntilTagExists(EventsOverviewScreenTestTags.UPCOMING_CHIP)
    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.UPCOMING_CHIP).performClick()
    waitUntilTagExists(EventsOverviewScreenTestTags.getTestTagForEventItem(event))

    composeTestRule.onNodeWithTag(EventsOverviewScreenTestTags.USER_AVATAR).performClick()
    waitUntilTagExists(ProfileScreenTestTags.TITLE)
    // The sign out button only shows once the profile is loaded
    waitUntilTagExists(ProfileScreenTestTags.SIGN_OUT_BUTTON)
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SIGN_OUT_BUTTON).performClick()
    waitUntilTagExists(LoginScreenTestTags.TITLE)
    composeTestRule.onNodeWithTag(LoginScreenTestTags.TITLE).assertIsDisplayed()

    // The join is stored on both sides: the event lists the member, the member lists the event
    asTheNewMember { uid ->
      val stored = EventRepositoryFirebase(Firebase.firestore).getEvent(event.eventId)
      assertTrue(stored.members.any { it.uid == uid })
      val user = UserRepositoryFirebase(Firebase.firestore).getUser(uid)
      assertTrue(event.eventId in user.joinedEvents)
    }
  }

  private fun waitUntilTagExists(tag: String, timeout: Long = TIMEOUT) {
    composeTestRule.waitUntil(timeoutMillis = timeout) {
      composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }
  }

  companion object {
    // Sign up and Firestore round trips are slower than the fake auth tests
    private const val TAG = "EndToEndTest"
    private const val TIMEOUT = 15_000L
    private const val PASSWORD = "Password123!"
    private const val EVENT_DETAILS_PLACEHOLDER = "Hello event details!"
    private val EMAIL = "e2e-${UUID.randomUUID()}@festivar.test"

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
     * Signs in as the member the journey created, straight through Firebase Auth so no profile is
     * recreated, and runs [block] with its uid; the uid comes from the sign in, not from the test.
     */
    private fun asTheNewMember(block: suspend (uid: String) -> Unit) = runBlocking {
      val uid =
          checkNotNull(Firebase.auth.signInWithEmailAndPassword(EMAIL, PASSWORD).await().user?.uid)
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
      asAnonymousUser { EventRepositoryFirebase(Firebase.firestore).addEvent(event) }
      // Signed out, so the app starts on the login screen
    }

    @JvmStatic
    @AfterClass
    fun cleanUp() {
      // The event goes first, so no event is left pointing at a deleted member
      asAnonymousUser {
        logged("delete the event") {
          EventRepositoryFirebase(Firebase.firestore).deleteEvent(event.eventId)
        }
      }
      // The Auth account can only be deleted by itself, so the member signs in once more
      runCatching {
        asTheNewMember { uid ->
          logged("delete the profile") {
            UserRepositoryFirebase(Firebase.firestore).deleteUser(uid)
          }
          logged("delete the account") { Firebase.auth.currentUser?.delete()?.await() }
        }
      }
          .onFailure { Log.w(TAG, "The member of this run could not sign in to clean up", it) }
    }
  }
}
