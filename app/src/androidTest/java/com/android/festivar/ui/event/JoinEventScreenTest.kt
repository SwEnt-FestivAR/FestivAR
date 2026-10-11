// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
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
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// androidTest cannot see src/test, so this duplicates the UsersRepositoryLocal fake that lives
// there. It keeps only what the join flow reads and writes.
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
    val index = users.indexOfFirst { it.uid == uId }
    if (index == -1) throw NoSuchElementException(uId)
    users[index] = newValue
  }

  override suspend fun deleteUser(uId: String) {
    users.removeAll { it.uid == uId }
  }
}

@RunWith(AndroidJUnit4::class)
class JoinEventScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  private val found = FoundEventUi("e1", "Vidy Festival", "Sat 17 Oct", 12)
  private val start = ZonedDateTime.of(2030, 10, 17, 18, 0, 0, 0, ZoneId.of("UTC"))
  private val event =
      Event(
          eventId = "vidy26abc",
          title = "Vidy Festival",
          startDate = start,
          endDate = start.plusHours(5),
      )

  private fun content(
      state: JoinEventUiState,
      onBack: () -> Unit = {},
      onCode: (String) -> Unit = {},
      onJoin: () -> Unit = {},
  ) {
    composeTestRule.setContent { JoinEventContent(state, onBack, onCode, onJoin) }
  }

  private fun screen(opened: MutableList<String>) {
    val user = User(uid = "u1")
    val viewModel =
        JoinEventViewModel(user, EventsRepositoryLocal(listOf(event)), InMemoryUsers(listOf(user)))
    composeTestRule.setContent {
      JoinEventScreen(viewModel = viewModel, onOpenEvent = { opened.add(it) })
    }
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

    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.RESULT).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(JoinEventScreenTestTags.RESULT_NAME, useUnmergedTree = true)
        .assertTextEquals("Vidy Festival")
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
  fun theCodeFieldIsDisabledWhileTheJoinIsRunning() {
    content(JoinEventUiState(code = "VIDY26", found = found, isJoining = true))

    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.CODE_INPUT).assertIsNotEnabled()
  }

  @Test
  fun anErrorMessageIsShownUnderTheCode() {
    content(JoinEventUiState(code = "VIDY26", found = found, errorMsg = "offline"))

    composeTestRule
        .onNodeWithTag(JoinEventScreenTestTags.ERROR_MESSAGE)
        .assertIsDisplayed()
        .assertTextEquals("offline")
  }

  @Test
  fun backCallsOnBack() {
    var backs = 0
    content(JoinEventUiState(), onBack = { backs++ })

    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.BACK).performClick()

    assertEquals(1, backs)
  }

  @Test
  fun oneMemberIsWrittenInTheSingular() {
    content(JoinEventUiState(code = "VIDY26", found = found.copy(members = 1)))

    composeTestRule
        .onNodeWithTag(JoinEventScreenTestTags.RESULT_LINE, useUnmergedTree = true)
        .assertTextEquals("Sat 17 Oct, 1 member")
  }

  @Test
  fun severalMembersAreWrittenInThePlural() {
    content(JoinEventUiState(code = "VIDY26", found = found.copy(members = 3)))

    composeTestRule
        .onNodeWithTag(JoinEventScreenTestTags.RESULT_LINE, useUnmergedTree = true)
        .assertTextEquals("Sat 17 Oct, 3 members")
  }

  @Test
  fun tappingACellFocusesTheCodeField() {
    content(JoinEventUiState())

    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.cell(3)).performClick()

    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.CODE_INPUT).assertIsFocused()
  }

  @Test
  fun theCodeFieldHasFocusWhenTheScreenOpens() {
    screen(mutableListOf())

    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.CODE_INPUT).assertIsFocused()
  }

  @Test
  fun aValidCodeOpensTheEventOnceJoined() {
    val opened = mutableListOf<String>()
    screen(opened)

    composeTestRule
        .onNodeWithTag(JoinEventScreenTestTags.CODE_INPUT)
        .performTextInput(joinCodeOf(event))
    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.RESULT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.JOIN).performClick()

    composeTestRule.waitUntil(5_000) { opened.isNotEmpty() }
    assertEquals(event.eventId, opened.first())
  }

  @Test
  fun theEventIsOpenedExactlyOnce() {
    val opened = mutableListOf<String>()
    screen(opened)

    composeTestRule
        .onNodeWithTag(JoinEventScreenTestTags.CODE_INPUT)
        .performTextInput(joinCodeOf(event))
    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.JOIN).performClick()
    composeTestRule.waitUntil(5_000) { opened.isNotEmpty() }
    composeTestRule.waitForIdle()

    assertEquals(listOf(event.eventId), opened)
  }

  @Test
  fun leavingAndReenteringTheScreenDoesNotOpenTheEventAgain() {
    val opened = mutableListOf<String>()
    val user = User(uid = "u1")
    val viewModel =
        JoinEventViewModel(user, EventsRepositoryLocal(listOf(event)), InMemoryUsers(listOf(user)))
    var shown by mutableStateOf(true)
    composeTestRule.setContent {
      if (shown) JoinEventScreen(viewModel = viewModel, onOpenEvent = { opened.add(it) })
    }
    composeTestRule
        .onNodeWithTag(JoinEventScreenTestTags.CODE_INPUT)
        .performTextInput(joinCodeOf(event))
    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.JOIN).performClick()
    composeTestRule.waitUntil(5_000) { opened.isNotEmpty() }
    composeTestRule.waitForIdle()

    shown = false
    composeTestRule.waitForIdle()
    shown = true
    composeTestRule.waitForIdle()

    assertEquals(listOf(event.eventId), opened)
  }

  @Test
  fun eachCodeCellIsAButtonNamedByItsPosition() {
    content(JoinEventUiState())

    for (i in 0 until 6) {
      composeTestRule
          .onNodeWithTag(JoinEventScreenTestTags.cell(i))
          .assertContentDescriptionEquals("Code character ${i + 1}")
          .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }
  }

  @Test
  fun theCodeRowAndTheHiddenFieldAreDescribedForAScreenReader() {
    composeTestRule.setContent {
      var code by remember { mutableStateOf("") }
      JoinEventContent(JoinEventUiState(code = code), {}, { code = it }, {})
    }

    composeTestRule.onNode(hasContentDescription("Code is empty")).assertExists()
    composeTestRule.onNode(hasContentDescription("Join code")).assertExists()

    composeTestRule.onNodeWithTag(JoinEventScreenTestTags.CODE_INPUT).performTextInput("AB12")

    composeTestRule.onNode(hasContentDescription("Code so far: AB12")).assertExists()
  }
}
