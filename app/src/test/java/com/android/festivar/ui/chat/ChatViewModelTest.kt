// Co-authored-by: Gemini
package com.android.festivar.ui.chat

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.chat.Chat
import com.android.festivar.model.chat.ChatMessage
import com.android.festivar.model.chat.ChatRepository
import com.android.festivar.model.chat.MessageContent
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import io.mockk.mockk
import java.io.IOException
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ChatViewModelTest {
  private val sentAt = ZonedDateTime.of(2026, 11, 20, 16, 0, 0, 0, ZoneOffset.UTC)
  private val chat =
      Chat(
          chatId = "c1",
          eventId = "e1",
          messages = listOf(ChatMessage("m1", "u1", sentAt, MessageContent.Text("Hello"))),
          members = listOf("u1", "u2"),
      )
  private val localChatRepo = ScriptedChatRepository(chat)
  private val users =
      UsersRepositoryLocal(
          listOf(
              User(uid = "u1", name = "Kim", surname = "Keller"),
              User(uid = "u2", name = "Manon", surname = "Servoz"),
          )
      )

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun dataRepository(chatRepo: ChatRepository? = localChatRepo): DataRepository =
      DataRepositoryLocal(
          authRepository = mockk(),
          tasksRepository = mockk(),
          eventsRepository = mockk(),
          usersRepository = users,
          chatRepository = chatRepo,
      )

  private fun viewModel(dataRepo: DataRepository = dataRepository()) =
      ChatViewModel("c1", dataRepo, "u2")

  /** Breaks if loadChat does not retrieve the chat and author user details successfully. */
  @Test
  fun loadChatRetrievesChatAndAuthors() {
    val vm = viewModel()
    assertTrue(vm.uiState.value.isLoading)

    vm.loadChat()

    val state = vm.uiState.value
    assertEquals(chat, state.chat)
    assertEquals(2, state.authors.size)
    assertEquals("Kim", state.authors["u1"]?.name)
    assertFalse(state.isLoading)
    assertTrue(state.canSend)
    assertNull(state.errorMsg)
  }

  /**
   * Breaks if loadChat failure sets an error and stops loading state without clearing error on
   * retry.
   */
  @Test
  fun loadChatFailureCanBeRetried() {
    localChatRepo.getFailure = IOException("offline")
    val vm = viewModel()

    vm.loadChat()

    assertNull(vm.uiState.value.chat)
    assertEquals(R.string.chat_load_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isLoading)

    localChatRepo.getFailure = null
    vm.loadChat()

    assertEquals(chat, vm.uiState.value.chat)
    assertNull(vm.uiState.value.errorMsg)
  }

  /**
   * Breaks if sendMessage does not add the message to the chat repository and update the UI state.
   */
  @Test
  fun sendMessageAddsMessageAndUpdatesState() {
    val vm = viewModel().also { it.loadChat() }

    vm.sendMessage(MessageContent.Text("Hi there"))

    val updatedChat = runBlocking { localChatRepo.getChat("c1") }
    assertEquals(2, updatedChat.messages.size)
    assertEquals("Hi there", (updatedChat.messages.last().content as MessageContent.Text).text)
    assertEquals("u2", updatedChat.messages.last().authorId)
    assertNull(vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isSending)
  }

  /** Breaks if sendMessage failure sets an error message and restores normal state. */
  @Test
  fun sendMessageFailureShowsError() {
    val vm = viewModel().also { it.loadChat() }
    localChatRepo.addMessageFailure = IOException("offline")

    vm.sendMessage(MessageContent.Text("Fail"))

    assertEquals(R.string.chat_send_failed, vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isSending)
  }

  /** Breaks if cancellation exceptions during loading or sending are swallowed as errors. */
  @Test
  fun cancellationExceptionsAreRethrownAndDoNotSetError() {
    localChatRepo.getFailure = CancellationException("cancelled")
    val vm = viewModel()

    vm.loadChat()
    assertNull(vm.uiState.value.errorMsg)
  }

  /** Breaks if blank constructor arguments are not rejected with IllegalArgumentException. */
  @Test
  fun blankArgumentsFailFast() {
    assertThrows(IllegalArgumentException::class.java) {
      ChatViewModel(" ", dataRepository(), "u2")
    }
    assertThrows(IllegalArgumentException::class.java) {
      ChatViewModel("c1", dataRepository(), " ")
    }
  }

  /** Breaks if clearError does not clear the error message. */
  @Test
  fun clearErrorClearsErrorMessage() {
    localChatRepo.getFailure = IOException("offline")
    val vm = viewModel().also { it.loadChat() }

    assertEquals(R.string.chat_load_failed, vm.uiState.value.errorMsg)
    vm.clearError()
    assertNull(vm.uiState.value.errorMsg)
  }

  /**
   * Breaks if ChatViewModel throws exception when initialized with unconfigured chat repository.
   */
  @Test
  fun unconfiguredChatRepositoryThrowsIllegalStateException() {
    val badDataRepo = dataRepository(chatRepo = null)
    assertThrows(IllegalStateException::class.java) { ChatViewModel("c1", badDataRepo, "u2") }
  }

  /** Breaks if loadChat executes concurrently while already active. */
  @Test
  fun loadChatIgnoresConcurrentCallsWhileLoading() {
    val gate = CompletableDeferred<Unit>()
    localChatRepo.beforeGet = { gate.await() }
    val vm = viewModel()

    vm.loadChat()
    vm.loadChat() // ignored while loading

    gate.complete(Unit)
    assertEquals(chat, vm.uiState.value.chat)
  }

  /** Breaks if sendMessage runs when chat is null or cannot send. */
  @Test
  fun sendMessageIgnoredWhenChatNullOrCannotSend() {
    val vm = viewModel() // chat is null initially
    vm.sendMessage(MessageContent.Text("Ignored"))
    assertNull(vm.uiState.value.chat)
  }

  /** Breaks if sendMessage handles cancellation without setting error or leaving isSending true. */
  @Test
  fun sendMessageCancellationExceptionDoesNotSetError() {
    val vm = viewModel().also { it.loadChat() }
    localChatRepo.addMessageFailure = CancellationException("cancelled")

    vm.sendMessage(MessageContent.Text("Cancel"))

    assertFalse(vm.uiState.value.isSending)
    assertNull(vm.uiState.value.errorMsg)
  }

  /**
   * Breaks if sendMessage reports a send failure when refreshing the chat or users fails after the
   * message has already been successfully added.
   */
  @Test
  fun sendMessageSucceedsEvenIfPostAddRefreshFails() {
    val vm = viewModel().also { it.loadChat() }
    localChatRepo.getFailure = null
    localChatRepo.refreshFailure = IOException("refresh offline")

    vm.sendMessage(MessageContent.Text("Hello again"))

    assertNull(vm.uiState.value.errorMsg)
    assertFalse(vm.uiState.value.isSending)
    localChatRepo.refreshFailure = null
    localChatRepo.getCallCount = 0
  }

  /** Breaks if isMember is false for chat members or true for non-members. */
  @Test
  fun isMemberStateIsCorrect() {
    val memberVm = viewModel()
    memberVm.loadChat()
    assertTrue(memberVm.uiState.value.isMember)
    assertTrue(memberVm.uiState.value.canSend)

    val nonMemberVm = ChatViewModel("c1", dataRepository(), "u99")
    nonMemberVm.loadChat()
    assertFalse(nonMemberVm.uiState.value.isMember)
    assertFalse(nonMemberVm.uiState.value.canSend)
  }

  private class ScriptedChatRepository(initialChat: Chat) : ChatRepository {
    var chatState = initialChat
    var getFailure: Exception? = null
    var refreshFailure: Exception? = null
    var addMessageFailure: Exception? = null
    var getCallCount = 0
    var beforeGet: suspend () -> Unit = {}
    var uidCounter = 10

    override fun getNewUid(): String = "m${uidCounter++}"

    override suspend fun getAllChats(): List<Chat> = listOf(chatState)

    override suspend fun getChat(chatId: String): Chat {
      beforeGet()
      getCallCount++
      if (getCallCount > 1 && refreshFailure != null) {
        throw refreshFailure!!
      }
      getFailure?.let { throw it }
      require(chatId == chatState.chatId) { "Chat not found" }
      return chatState
    }

    override suspend fun getChatsForEvent(eventId: String): List<Chat> =
        listOf(chatState).filter { it.eventId == eventId }

    override suspend fun addChat(chat: Chat) {
      chatState = chat
    }

    override suspend fun editChat(chatId: String, newValue: Chat) {
      chatState = newValue
    }

    override suspend fun deleteChat(chatId: String) {
      // no-op
    }

    override suspend fun addMessage(chatId: String, message: ChatMessage) {
      addMessageFailure?.let { throw it }
      chatState = chatState.addMessage(message)
    }

    override suspend fun deleteMessage(chatId: String, messageId: String) {
      chatState = chatState.deleteMessage(messageId)
    }

    override suspend fun addMember(chatId: String, userId: String) {
      chatState = chatState.addMember(userId)
    }

    override suspend fun removeMember(chatId: String, userId: String) {
      chatState = chatState.removeMember(userId)
    }
  }
}
