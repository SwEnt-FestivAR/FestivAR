// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.ui.chat.overview

import com.android.festivar.R
import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.chat.Chat
import com.android.festivar.model.chat.ChatMessage
import com.android.festivar.model.chat.ChatRepository
import com.android.festivar.model.chat.MessageContent
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import io.mockk.mockk
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatsOverviewViewModelTest {
  // The messages are stored in UTC and shown in the zone of the clock, nine hours ahead.
  private val zone = ZoneId.of("Asia/Tokyo")
  private val clock = Clock.fixed(utc(13).toInstant(), zone)

  private val me = User(ME, "Mia", "Keller")
  private val alice = User("alice", "Alice", "Martin")
  private val bob = User("bob", "Bob", "Dupont")
  private val event = Event(EVENT_ID, "owner", "Festival", startDate = utc(8), endDate = utc(18))

  // The general chat has the identifier of the event. The signed-in user is not in its members.
  private val general = chat(EVENT_ID, message("g1", "alice", 11), members = listOf("alice"))
  private val older = chat("older", message("o1", ME, 9))
  private val newer = chat("newer", message("n1", "alice", 8), message("n2", "bob", 10))
  private val empty = chat("empty")
  // Has the newest message of all, sent by somebody the users repository does not know.
  private val foreign = chat("foreign", message("f1", "ghost", 12), members = listOf("alice"))

  private val loaded =
      ChatsOverviewUIState("Festival", item(general, alice), listOf(item(older, me, own = true)))
  private val loadFailed = R.string.chats_overview_load_failed

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  /** Breaks if the first state is not loading, has an error or chats, or fetches on creation. */
  @Test
  fun initialStateIsLoadingWithoutErrorOrChatsAndFetchesNothing() {
    val repository = FakeChatRepository(listOf(general, older))

    val state = overviewViewModel(repository).uiState.value

    assertEquals(ChatsOverviewUIState(isLoading = true), state)
    assertTrue(state.hasNoChats)
    assertEquals(0, repository.calls)
  }

  /**
   * Breaks if the title, the general chat, the membership filter, the order of the user's chats,
   * the authors, the "own" flag or the zone of the last messages are off.
   */
  @Test
  fun refreshShowsTheGeneralChatAndTheChatsOfTheUserMostRecentFirst() = runTest {
    val state = loadedState(empty, older, foreign, general, newer)

    assertEquals(
        ChatsOverviewUIState(
            eventTitle = "Festival",
            generalChat = item(general, alice),
            yourChats =
                listOf(item(newer, bob), item(older, me, own = true), ChatsOverviewItem("empty")),
        ),
        state,
    )
    // 11:00 in UTC is 20:00 in Tokyo: the zone of the clock, and the same instant.
    assertEquals(today.atTime(20, 0).atZone(zone), state.generalChat?.lastMessage?.sentAt)
  }

  /**
   * Breaks if a failed refresh loses the title or the chats or shows no error, if a retry clears
   * the error when it starts, or if a successful retry keeps the error or the old chats.
   */
  @Test
  fun failedRefreshKeepsTheChatsAndTheErrorStaysUntilARetrySucceeds() = runTest {
    val repository = FakeChatRepository(listOf(general, older))
    val viewModel = overviewViewModel(repository)
    viewModel.refreshUIState()

    repository.failure = IllegalStateException("fetch failed")
    viewModel.refreshUIState()

    assertEquals(loaded.copy(errorMsg = loadFailed), viewModel.uiState.value)

    repository.failure = null
    repository.gated = true
    viewModel.refreshUIState()

    assertEquals(loaded.copy(isLoading = true, errorMsg = loadFailed), viewModel.uiState.value)

    repository.fetches.single().complete(listOf(newer))

    assertEquals(
        ChatsOverviewUIState("Festival", yourChats = listOf(item(newer, bob))),
        viewModel.uiState.value,
    )
  }

  /** Breaks if a data repository without chats crashes the refresh instead of failing it. */
  @Test
  fun refreshWithoutChatRepositoryShowsTheError() = runTest {
    val viewModel = overviewViewModel(chatRepository = null)

    viewModel.refreshUIState()

    assertEquals(ChatsOverviewUIState(errorMsg = loadFailed), viewModel.uiState.value)
  }

  /**
   * Breaks if a cancelled refresh that ends late, with chats or with a failure, stops the loading
   * or shows its chats or its error.
   */
  @Test
  fun onlyTheLatestRefreshReachesTheState() = runTest {
    val repository = FakeChatRepository(gated = true)
    val viewModel = overviewViewModel(repository)
    repeat(3) { viewModel.refreshUIState() }
    val (staleChats, staleFailure, latest) = repository.fetches

    staleChats.complete(listOf(general))

    assertEquals(ChatsOverviewUIState(isLoading = true), viewModel.uiState.value)

    staleFailure.completeExceptionally(IllegalStateException("stale failure"))

    assertEquals(ChatsOverviewUIState(isLoading = true), viewModel.uiState.value)

    latest.complete(listOf(general, older))

    assertEquals(loaded, viewModel.uiState.value)
  }

  /** Breaks if a CancellationException of the repository shows an error or blocks a refresh. */
  @Test
  fun aCancellationExceptionFromTheRepositoryIsNotALoadError() = runTest {
    val repository = FakeChatRepository(listOf(general, older))
    repository.failure = CancellationException("fetch cancelled")
    val viewModel = overviewViewModel(repository)

    viewModel.refreshUIState()

    assertNull(viewModel.uiState.value.errorMsg)

    repository.failure = null
    viewModel.refreshUIState()

    assertEquals(loaded, viewModel.uiState.value)
  }

  /** Breaks if the empty state only looks at the general chat or only at the user's chats. */
  @Test
  fun hasNoChatsOnlyWithoutGeneralChatAndWithoutChatOfTheUser() = runTest {
    assertTrue(loadedState().hasNoChats)
    assertFalse(loadedState(general).hasNoChats)
    assertFalse(loadedState(older).hasNoChats)
  }

  private fun overviewViewModel(chatRepository: ChatRepository?) =
      ChatsOverviewViewModel(
          EVENT_ID,
          DataRepositoryLocal(
              FakeAuthRepository(),
              eventsRepository = EventsRepositoryLocal(listOf(event)),
              usersRepository = UsersRepositoryLocal(listOf(me, alice, bob)),
              chatRepository = chatRepository,
          ),
          currentUserId = ME,
          clock = clock,
      )

  private fun loadedState(vararg chats: Chat): ChatsOverviewUIState {
    val viewModel = overviewViewModel(FakeChatRepository(chats.toList()))
    viewModel.refreshUIState()
    return viewModel.uiState.value
  }

  /** The row of [chat] in the overview, with its last message in the zone of the clock. */
  private fun item(chat: Chat, author: User, own: Boolean = false): ChatsOverviewItem {
    val last = chat.messages.last()
    val shown = last.copy(sentAt = last.sentAt.withZoneSameInstant(zone))
    return ChatsOverviewItem(chat.chatId, shown, author, own)
  }

  private fun chat(
      chatId: String,
      vararg messages: ChatMessage,
      members: List<String> = listOf(ME, "alice"),
  ) = Chat(chatId, EVENT_ID, messages.toList(), members)

  private fun message(id: String, authorId: String, hour: Int) =
      ChatMessage(id, authorId, utc(hour), MessageContent.Text(id))

  private fun utc(hour: Int): ZonedDateTime = today.atTime(hour, 0).atZone(ZoneOffset.UTC)

  /**
   * Serves [chats], or throws [failure], and counts the fetches. When [gated], each fetch waits
   * instead for its entry in [fetches], and still ends if it was cancelled in the meantime.
   */
  private class FakeChatRepository(
      private val chats: List<Chat> = emptyList(),
      var gated: Boolean = false,
  ) : ChatRepository by mockk() {
    var calls = 0
    var failure: Exception? = null
    val fetches = mutableListOf<CompletableDeferred<List<Chat>>>()

    override suspend fun getChatsForEvent(eventId: String): List<Chat> {
      calls++
      failure?.let { throw it }
      if (!gated) return chats
      val fetch = CompletableDeferred<List<Chat>>().also { fetches += it }
      return withContext(NonCancellable) { fetch.await() }
    }
  }

  private companion object {
    const val EVENT_ID = "event-1"
    const val ME = "me"
    val today: LocalDate = LocalDate.of(2026, 10, 17)
  }
}
