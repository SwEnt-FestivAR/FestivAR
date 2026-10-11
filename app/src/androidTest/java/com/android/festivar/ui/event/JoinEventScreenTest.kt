// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.joinCodeOf
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private class InMemoryUsers(initial: List<User>) : UsersRepository {
  private val users = initial.toMutableList()

  override fun getNewUid() = "uid${users.size}"

  override suspend fun getAllUsers() = users.toList()

  override suspend fun getUser(uId: String) =
      users.firstOrNull { it.uid == uId } ?: throw NoSuchElementException(uId)

  override suspend fun getListUsers(uIds: List<String>) = uIds.map { getUser(it) }

  override suspend fun addUser(user: User) {
    users.add(user)
  }

  override suspend fun editUser(uId: String, newValue: User) {
    users[users.indexOfFirst { it.uid == uId }] = newValue
  }

  override suspend fun deleteUser(uId: String) {
    users.removeAll { it.uid == uId }
  }
}

@RunWith(AndroidJUnit4::class)
class JoinEventScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val found = FoundEventUi("e1", "Vidy Festival", "Sat 17 Oct", 12)

  private fun content(
      state: JoinEventUiState,
      onBack: () -> Unit = {},
      onCode: (String) -> Unit = {},
      onJoin: () -> Unit = {},
  ) {
    composeTestRule.setContent { JoinEventContent(state, onBack, onCode, onJoin) }
  }

  @Test
  fun typingInTheCodeFieldSendsTheTextToTheViewModel() {
    var typed = ""
    content(JoinEventUiState(), onCode = { typed = it })

    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.CODE_INPUT).performTextInput("ab12")

    assertEquals("ab12", typed)
  }

  @Test
  fun aFoundEventShowsItsNameAndEnablesJoin() {
    var joins = 0
    content(JoinEventUiState(code = "VIDY26", found = found), onJoin = { joins++ })

    composeTestRule
        .onNodeWithTag(JoinEventScreenTestTags.RESULT)
        .assertIsDisplayed()
        .assertTextContains("Vidy Festival", substring = true)
    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.JOIN).assertIsEnabled().performClick()
    assertEquals(1, joins)
  }

  @Test
  fun anUnknownCodeShowsNotFoundAndNoJoin() {
    content(JoinEventUiState(code = "ZZZZZZ", notFound = true))

    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.NOT_FOUND).assertIsDisplayed()
    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.RESULT).assertDoesNotExist()
    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.JOIN).assertIsNotEnabled()
  }

  @Test
  fun joinIsDisabledWhileTheJoinIsRunning() {
    content(JoinEventUiState(code = "VIDY26", found = found, isJoining = true))

    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.JOIN).assertIsNotEnabled()
  }

  @Test
  fun backCallsOnBack() {
    var backs = 0
    content(JoinEventUiState(), onBack = { backs++ })

    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.BACK).performClick()

    assertEquals(1, backs)
  }

  @Test
  fun aValidCodeOpensTheEventOnceJoined() {
    val start = ZonedDateTime.now().plusDays(1)
    val event =
        Event(
            eventId = "vidy26abc",
            title = "Vidy Festival",
            startDate = start,
            endDate = start.plusHours(5),
        )
    val user = User(uid = "u1")
    val viewModel =
        JoinEventViewModel(user, EventsRepositoryLocal(listOf(event)), InMemoryUsers(listOf(user)))
    var opened: String? = null
    composeTestRule.setContent {
      JoinEventScreen(viewModel = viewModel, onOpenEvent = { opened = it })
    }

    composeTestRule
        .onNodeWithTag(JoinEventScreenTestTags.CODE_INPUT)
        .performTextInput(joinCodeOf(event))
    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.RESULT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.JOIN).performClick()

    composeTestRule.waitUntil(5_000) { opened != null }
    assertEquals(event.eventId, opened)
  }
}
