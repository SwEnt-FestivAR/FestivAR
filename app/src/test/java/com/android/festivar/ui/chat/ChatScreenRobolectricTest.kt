// Co-authored-by: Gemini
package com.android.festivar.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.R
import com.android.festivar.model.chat.Chat
import com.android.festivar.model.chat.ChatMessage
import com.android.festivar.model.chat.ChatRepository
import com.android.festivar.model.chat.MessageContent
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.ui.theme.AppTheme
import io.mockk.mockk
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for [ChatScreen] running on Robolectric to ensure UI functionality and coverage. */
@RunWith(AndroidJUnit4::class)
class ChatScreenRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val userCamille = User("u1", name = "Camille", surname = "Durand")
  private val userVous = User("u2", name = "Manon", surname = "Servoz")

  private val chatDate = LocalDate.of(2026, 10, 17)
  private val sampleChat =
      Chat(
          chatId = "c1",
          eventId = "e1",
          messages =
              listOf(
                  ChatMessage(
                      messageId = "m1",
                      authorId = "u1",
                      sentAt = ZonedDateTime.of(2026, 10, 17, 13, 58, 0, 0, ZoneOffset.UTC),
                      content =
                          MessageContent.Text("On se retrouve à 18 h pour installer les tables ?"),
                  ),
                  ChatMessage(
                      messageId = "m2",
                      authorId = "u2",
                      sentAt = ZonedDateTime.of(2026, 10, 17, 13, 59, 0, 0, ZoneOffset.UTC),
                      content = MessageContent.Text("Oui, je m'occupe de la grande salle."),
                  ),
              ),
          members = listOf("u1", "u2"),
      )

  /** Breaks if the top bar fails to display the custom title or trigger the back callback. */
  @Test
  fun topBarDisplaysTitleAndTriggersBack() {
    var backClicked = false
    composeTestRule.setContent {
      AppTheme {
        ChatContent(
            state = ChatUiState(chat = sampleChat, isLoading = false),
            currentUserId = "u2",
            chatTitle = "General",
            onBack = { backClicked = true },
        )
      }
    }

    composeTestRule.onNodeWithText("General").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.BACK_BUTTON).performClick()
    assertTrue(backClicked)
  }

  /** Breaks if the screen fails to display a loading indicator when loading without data. */
  @Test
  fun displaysLoadingIndicatorWhenLoadingWithoutChat() {
    composeTestRule.setContent {
      AppTheme {
        ChatContent(
            state = ChatUiState(chat = null, isLoading = true),
            currentUserId = "u2",
        )
      }
    }

    composeTestRule.onNodeWithTag(ChatScreenTestTags.LOADING).assertIsDisplayed()
  }

  /** Breaks if an empty chat fails to display the empty chat placeholder message. */
  @Test
  fun displaysEmptyMessageWhenChatHasNoMessages() {
    composeTestRule.setContent {
      AppTheme {
        ChatContent(
            state = ChatUiState(chat = Chat("c1", "e1"), isLoading = false),
            currentUserId = "u2",
        )
      }
    }

    composeTestRule.onNodeWithTag(ChatScreenTestTags.EMPTY_CHAT).assertIsDisplayed()
  }

  /** Breaks if messages, date header, author labels, or message text are not rendered correctly. */
  @Test
  fun rendersMessagesAndDateHeadersCorrectly() {
    composeTestRule.setContent {
      AppTheme {
        ChatContent(
            state =
                ChatUiState(
                    chat = sampleChat,
                    authors = mapOf("u1" to userCamille, "u2" to userVous),
                    isLoading = false,
                ),
            currentUserId = "u2",
            clockToday = chatDate,
        )
      }
    }

    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.dateHeader(chatDate.toString()))
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("CAMILLE · 13:58").assertIsDisplayed()
    composeTestRule
        .onNodeWithText("On se retrouve à 18 h pour installer les tables ?")
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Oui, je m'occupe de la grande salle.").assertIsDisplayed()
  }

  /** Breaks if the user cannot type into the input field and submit a new text message. */
  @Test
  fun typingMessageEnablesSendButtonAndTriggersCallback() {
    var sentMessage: String? = null
    composeTestRule.setContent {
      AppTheme {
        ChatContent(
            state = ChatUiState(chat = sampleChat, isLoading = false),
            currentUserId = "u2",
            onSendMessage = { sentMessage = it },
        )
      }
    }

    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).assertIsNotEnabled()

    composeTestRule
        .onNodeWithTag(ChatScreenTestTags.INPUT_FIELD)
        .performTextInput("Chaque table à sa place !!!")

    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.SEND_BUTTON).performClick()

    assertEquals("Chaque table à sa place !!!", sentMessage)
  }

  /** Breaks if an error message and retry button are not shown when state has an error. */
  @Test
  fun displaysErrorAndTriggersRetry() {
    var retryClicked = false
    composeTestRule.setContent {
      AppTheme {
        ChatContent(
            state =
                ChatUiState(
                    chat = null,
                    isLoading = false,
                    errorMsg = R.string.chat_load_failed,
                ),
            currentUserId = "u2",
            onRetry = { retryClicked = true },
        )
      }
    }

    composeTestRule.onNodeWithTag(ChatScreenTestTags.ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChatScreenTestTags.RETRY).performClick()
    assertTrue(retryClicked)
  }

  /** Breaks if connecting ChatScreen to ViewModel fails to render and load data. */
  @Test
  fun chatScreenConnectsToViewModelCorrectly() {
    val usersRepo = UsersRepositoryLocal(listOf(userCamille, userVous))
    val localChatRepo = ScriptedChatRepository(sampleChat)
    val dataRepo =
        DataRepositoryLocal(
            authRepository = mockk(),
            usersRepository = usersRepo,
            chatRepository = localChatRepo,
        )
    val viewModel = ChatViewModel("c1", dataRepo, "u2")

    composeTestRule.setContent { AppTheme { ChatScreen(viewModel = viewModel) } }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("General").assertIsDisplayed()
    composeTestRule.onNodeWithText("CAMILLE · 13:58").assertIsDisplayed()
  }

  /** Breaks if the preview composable fails to render without throwing exceptions. */
  @Test
  fun chatScreenPreviewRendersSuccessfully() {
    composeTestRule.setContent { ChatScreenPreview() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("CAMILLE · 13:58").assertIsDisplayed()
  }

  private class ScriptedChatRepository(initialChat: Chat) : ChatRepository {
    var chatState = initialChat

    override fun getNewUid(): String = "m99"

    override suspend fun getAllChats(): List<Chat> = listOf(chatState)

    override suspend fun getChat(chatId: String): Chat = chatState

    override suspend fun getChatsForEvent(eventId: String): List<Chat> = listOf(chatState)

    override suspend fun addChat(chat: Chat) {
      chatState = chat
    }

    override suspend fun editChat(chatId: String, newValue: Chat) {
      chatState = newValue
    }

    override suspend fun deleteChat(chatId: String) {}

    override suspend fun addMessage(
        chatId: String,
        message: ChatMessage,
    ) {
      chatState = chatState.addMessage(message)
    }

    override suspend fun deleteMessage(
        chatId: String,
        messageId: String,
    ) {}

    override suspend fun addMember(
        chatId: String,
        userId: String,
    ) {}

    override suspend fun removeMember(
        chatId: String,
        userId: String,
    ) {}
  }
}
