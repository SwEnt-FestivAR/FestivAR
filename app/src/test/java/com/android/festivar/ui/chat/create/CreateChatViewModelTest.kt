// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.chat.create

import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.chat.Chat
import com.android.festivar.model.chat.ChatRepository
import com.android.festivar.model.chat.ChatRepositoryLocal
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.ui.chat.ChatFormState
import com.android.festivar.ui.components.FieldError
import java.io.IOException
import java.time.ZonedDateTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class CreateChatViewModelTest {
  private val owner = User("owner-1", name = "Marie", surname = "Daron")
  private val member = User("member-1", name = "Noah", surname = "Favre")
  private val otherMember = User("member-2", name = "Camille", surname = "Roy")
  private val event =
      Event(
          eventId = "event-1",
          ownerId = owner.uid,
          title = "Fête de l'Asso 2026",
          members = listOf(owner, member, otherMember),
          startDate = ZonedDateTime.parse("2026-11-06T16:00:00+01:00"),
          endDate = ZonedDateTime.parse("2026-11-07T04:00:00+01:00"),
      )

  private val eventsRepository = ScriptedEventsRepository(EventsRepositoryLocal(listOf(event)))
  private val chatRepository = ScriptedChatRepository(ChatRepositoryLocal())
  private val dataRepository =
      DataRepositoryLocal(
          authRepository = FakeAuthRepository(),
          eventsRepository = eventsRepository,
          chatRepository = chatRepository,
      )

  private fun viewModel(currentUserId: String = owner.uid) =
      CreateChatViewModel(event.eventId, currentUserId, dataRepository)

  private fun readyViewModel(name: String = "Bar crew"): CreateChatViewModel =
      viewModel().apply {
        load()
        idle()
        updateName(name)
        toggleMember(member.uid)
      }

  private fun idle() = shadowOf(Looper.getMainLooper()).idle()

  private class ScriptedEventsRepository(private val local: EventsRepositoryLocal) :
      EventsRepository by local {
    var beforeRead: suspend () -> Unit = {}

    override suspend fun getEvent(eventId: String): Event {
      beforeRead()
      return local.getEvent(eventId)
    }
  }

  private class ScriptedChatRepository(private val local: ChatRepositoryLocal) :
      ChatRepository by local {
    var beforeAdd: suspend () -> Unit = {}
    var addCount = 0

    override suspend fun addChat(chat: Chat) {
      addCount++
      beforeAdd()
      local.addChat(chat)
    }
  }

  /** Breaks if blank identifiers are accepted by the create chat ViewModel. */
  @Test
  fun blankIdsFailFast() {
    assertThrows(IllegalArgumentException::class.java) {
      CreateChatViewModel(" ", owner.uid, dataRepository)
    }
    assertThrows(IllegalArgumentException::class.java) {
      CreateChatViewModel(event.eventId, " ", dataRepository)
    }
  }

  /**
   * Breaks if load omits event members, fails to include the current user automatically, or leaves
   * the screen loading.
   */
  @Test
  fun loadExposesEventMembersAndIncludesCurrentUser() {
    val vm = viewModel()

    vm.load()
    idle()

    val state = vm.uiState.value
    assertEquals(event, state.event)
    assertEquals(listOf(owner, member, otherMember), state.members)
    assertEquals(owner.uid, state.currentUserId)
    assertEquals(1, state.selectedMemberCount)
    assertTrue(state.selectedMemberIds.isEmpty())
    assertFalse(state.isLoading)
    assertFalse(state.canCreate)
  }

  /** Breaks if the current user can be removed or a non-event user can be selected. */
  @Test
  fun onlyOtherEventMembersCanBeToggled() {
    val vm = viewModel().apply { load() }
    idle()

    vm.toggleMember(owner.uid)
    vm.toggleMember("outside-event")

    assertTrue(vm.uiState.value.selectedMemberIds.isEmpty())
    vm.toggleMember(member.uid)
    assertEquals(listOf(member.uid), vm.uiState.value.selectedMemberIds)
    vm.toggleMember(member.uid)
    assertTrue(vm.uiState.value.selectedMemberIds.isEmpty())
  }

  /**
   * Breaks if the shared form accepts a blank or overlong name, fails to trim it, or allows chat
   * creation without another selected event member.
   */
  @Test
  fun nameAndMemberSelectionControlCreateAvailability() {
    val vm = viewModel().apply { load() }
    idle()

    vm.updateName("  ")
    assertEquals(FieldError.EMPTY_CHAT_NAME, vm.uiState.value.form.nameError)
    vm.updateName("x".repeat(81))
    assertEquals(FieldError.TOO_LONG_CHAT_NAME, vm.uiState.value.form.nameError)
    assertFalse(vm.uiState.value.canCreate)

    vm.updateName("  Bar crew  ")
    assertEquals("Bar crew", vm.uiState.value.form.trimmedName)
    assertNull(vm.uiState.value.form.nameError)
    assertFalse(vm.uiState.value.canCreate)

    vm.toggleMember(member.uid)
    assertTrue(vm.uiState.value.canCreate)
  }

  /** Breaks if createChat stores untrimmed data or omits the event link or selected members. */
  @Test
  fun createStoresTheTrimmedChatAndLinksItToTheEvent() {
    val vm = readyViewModel("  Bar crew  ")

    vm.createChat()
    idle()

    val created = runBlocking { chatRepository.getChatsForEvent(event.eventId).single() }
    assertEquals("Bar crew", created.name)
    assertEquals(listOf(owner.uid, member.uid), created.members)
    assertTrue(
        runBlocking { eventsRepository.getEvent(event.eventId) }.chatIds.contains(created.chatId)
    )
    assertTrue(vm.uiState.value.isCreated)
    assertFalse(vm.uiState.value.isCreating)
    assertNull(vm.uiState.value.errorMsg)
  }

  /** Breaks if a second create tap writes another chat while the first write is pending. */
  @Test
  fun aSecondCreateWhileSavingIsIgnored() {
    val gate = CompletableDeferred<Unit>()
    chatRepository.beforeAdd = { gate.await() }
    val vm = readyViewModel()

    vm.createChat()
    idle()
    vm.createChat()

    assertTrue(vm.uiState.value.isCreating)
    assertEquals(1, chatRepository.addCount)

    gate.complete(Unit)
    idle()

    assertTrue(vm.uiState.value.isCreated)
    assertEquals(1, chatRepository.addCount)
  }

  /** Breaks if a failed event load stays loading or cannot be retried. */
  @Test
  fun aFailedLoadShowsAnErrorAndCanBeRetried() {
    eventsRepository.beforeRead = { throw IOException("offline") }
    val vm = viewModel()

    vm.load()
    idle()

    assertEquals(R.string.create_chat_error_load_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isLoading)

    eventsRepository.beforeRead = {}
    vm.load()
    idle()

    assertEquals(event, vm.uiState.value.event)
    assertNull(vm.uiState.value.errorMsg)
  }

  /** Breaks if a failed chat write is shown as success or leaves the form locked. */
  @Test
  fun aFailedCreateShowsAnErrorAndLeavesTheFormEditable() {
    chatRepository.beforeAdd = { throw IOException("offline") }
    val vm = readyViewModel()

    vm.createChat()
    idle()

    assertEquals(R.string.create_chat_error_save_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isCreating)
    assertFalse(vm.uiState.value.isCreated)
    assertTrue(vm.uiState.value.isEditable)
    assertTrue(
        runBlocking { eventsRepository.getEvent(event.eventId) }
            .chatIds
            .none { it != event.eventId }
    )
  }

  /** Breaks if a cancelled load becomes a user-visible load failure. */
  @Test
  fun aCancelledLoadShowsNoError() {
    val never = CompletableDeferred<Unit>()
    eventsRepository.beforeRead = { never.await() }
    val vm = viewModel().apply { load() }

    vm.viewModelScope.cancel()
    idle()

    assertNull(vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isLoading)
  }

  /** Breaks if the shared chat form's untouched state starts with a validation error. */
  @Test
  fun untouchedChatFormHasNoFieldError() {
    assertEquals(ChatFormState(), viewModel().uiState.value.form)
    assertNull(ChatFormState().nameError)
  }

  /**
   * Breaks if loading an event where the current user is not a member succeeds instead of failing.
   */
  @Test
  fun loadFailsWhenCurrentUserIsNotAnEventMember() {
    val vm = viewModel(currentUserId = "non-member")

    vm.load()
    idle()

    assertEquals(R.string.create_chat_error_load_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isLoading)
    assertNull(vm.uiState.value.event)
  }

  /** Breaks if form edits are applied while the form is not editable. */
  @Test
  fun applyEditIsIgnoredWhenNotEditable() {
    val vm = readyViewModel()
    vm.createChat()
    idle()
    assertTrue(vm.uiState.value.isCreated)
    assertFalse(vm.uiState.value.isEditable)

    vm.applyEdit { it.copy(name = "Changed Name") }

    assertEquals("Bar crew", vm.uiState.value.form.name)
  }

  /**
   * Breaks if members can be toggled before the event is loaded or when the form is not editable.
   */
  @Test
  fun toggleMemberIsIgnoredBeforeLoadOrWhenNotEditable() {
    val vm = viewModel()
    assertNull(vm.uiState.value.event)

    vm.toggleMember(member.uid)
    assertTrue(vm.uiState.value.selectedMemberIds.isEmpty())

    vm.load()
    idle()
    vm.updateName("Bar crew")
    vm.toggleMember(member.uid)
    assertEquals(listOf(member.uid), vm.uiState.value.selectedMemberIds)

    vm.createChat()
    idle()
    assertTrue(vm.uiState.value.isCreated)

    vm.toggleMember(member.uid)
    assertEquals(listOf(member.uid), vm.uiState.value.selectedMemberIds)
  }

  /** Breaks if creating a chat without a configured chat repository fails to set a save error. */
  @Test
  fun createChatFailsWhenChatRepositoryIsNull() {
    val noChatDataRepo =
        DataRepositoryLocal(
            authRepository = FakeAuthRepository(),
            eventsRepository = eventsRepository,
            chatRepository = null,
        )
    val vm = CreateChatViewModel(event.eventId, owner.uid, noChatDataRepo)

    vm.load()
    idle()
    vm.updateName("Bar crew")
    vm.toggleMember(member.uid)
    assertTrue(vm.uiState.value.canCreate)

    vm.createChat()
    idle()

    assertEquals(R.string.create_chat_error_save_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isCreating)
    assertFalse(vm.uiState.value.isCreated)
  }
}
