// Co-authored-by: Gemini
package com.android.festivar.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.festivar.R
import com.android.festivar.model.chat.Chat
import com.android.festivar.model.chat.ChatMessage
import com.android.festivar.model.chat.MessageContent
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.datetime.formatDate
import com.android.festivar.ui.components.datetime.formatTime
import com.android.festivar.ui.theme.AppTheme
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

object ChatScreenTestTags {
  const val SCREEN = "chatScreen"
  const val BACK_BUTTON = "chatScreenBackButton"
  const val TITLE = "chatScreenTitle"
  const val LOADING = "chatScreenLoading"
  const val ERROR = "chatScreenError"
  const val RETRY = "chatScreenRetry"
  const val MESSAGE_LIST = "chatScreenMessageList"
  const val INPUT_FIELD = "chatScreenInputField"
  const val SEND_BUTTON = "chatScreenSendButton"
  const val EMPTY_CHAT = "chatScreenEmpty"

  fun dateHeader(dateString: String) = "chatScreenDateHeader:$dateString"
}

/**
 * Screen displaying the conversation for a chat, driven by [ChatViewModel].
 *
 * @param viewModel The [ChatViewModel] managing chat loading and sending messages.
 * @param modifier The [Modifier] to apply to the screen layout.
 * @param onBack Callback invoked when the user taps the back button.
 * @param chatTitle The title displayed in the top bar (defaults to "General").
 */
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    chatTitle: String = stringResource(R.string.chat_default_title),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.loadChat() }

  ChatContent(
      state = uiState,
      currentUserId = viewModel.currentUserId,
      modifier = modifier,
      chatTitle = chatTitle,
      onBack = onBack,
      onSendMessage = { text -> viewModel.sendMessage(MessageContent.Text(text)) },
      onRetry = viewModel::loadChat,
  )
}

/**
 * Stateless content composable for [ChatScreen].
 *
 * @param state Current [ChatUiState] to render.
 * @param currentUserId Unique identifier of the logged-in user.
 * @param modifier The [Modifier] applied to the container.
 * @param chatTitle Title shown in the top navigation bar.
 * @param onBack Callback invoked on back button tap.
 * @param onSendMessage Callback invoked when the user sends a message.
 * @param onRetry Callback invoked to retry loading the chat.
 * @param clockToday Current date used for relative "TODAY" date headers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatContent(
    state: ChatUiState,
    currentUserId: String,
    modifier: Modifier = Modifier,
    chatTitle: String = stringResource(R.string.chat_default_title),
    onBack: () -> Unit = {},
    onSendMessage: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    clockToday: LocalDate = LocalDate.now(),
) {
  Scaffold(
      modifier = modifier.testTag(ChatScreenTestTags.SCREEN),
      containerColor = MaterialTheme.colorScheme.background,
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = chatTitle,
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.testTag(ChatScreenTestTags.TITLE),
              )
            },
            navigationIcon = {
              IconButton(
                  onClick = onBack,
                  modifier = Modifier.testTag(ChatScreenTestTags.BACK_BUTTON),
              ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.chat_back),
                )
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
        )
      },
      bottomBar = {
        ChatInputBar(
            canSend = state.canSend,
            onSendMessage = onSendMessage,
        )
      },
  ) { innerPadding ->
    Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding).imePadding(),
    ) {
      state.errorMsg?.let { errorRes ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
              text = stringResource(errorRes),
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.weight(1f).testTag(ChatScreenTestTags.ERROR),
          )
          TextButton(
              onClick = onRetry,
              modifier = Modifier.testTag(ChatScreenTestTags.RETRY),
          ) {
            Text(stringResource(R.string.chat_retry))
          }
        }
      }

      if (state.isLoading && (state.chat == null)) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(
              modifier = Modifier.size(32.dp).testTag(ChatScreenTestTags.LOADING),
          )
        }
      } else
          state.chat?.let { chat ->
            if (chat.messages.isEmpty()) {
              Box(
                  modifier = Modifier.fillMaxSize().padding(32.dp),
                  contentAlignment = Alignment.Center,
              ) {
                Text(
                    text = stringResource(R.string.chat_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(ChatScreenTestTags.EMPTY_CHAT),
                )
              }
            } else {
              val listState = rememberLazyListState()

              val groupedMessages =
                  remember(chat.messages) { chat.messages.groupBy { it.sentAt.toLocalDate() } }

              LazyColumn(
                  state = listState,
                  modifier = Modifier.fillMaxSize().testTag(ChatScreenTestTags.MESSAGE_LIST),
                  contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                groupedMessages.forEach { (date, messagesInDay) ->
                  item(key = "date_$date") {
                    val headerText =
                        if (date == clockToday) {
                          stringResource(R.string.chat_today)
                        } else {
                          formatDate(date).uppercase()
                        }
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .testTag(ChatScreenTestTags.dateHeader(date.toString())),
                        contentAlignment = Alignment.Center,
                    ) {
                      Text(
                          text = headerText,
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          fontWeight = FontWeight.SemiBold,
                      )
                    }
                  }

                  items(
                      items = messagesInDay,
                      key = { it.messageId },
                  ) { message ->
                    val isCurrentUser = message.authorId == currentUserId
                    val authorUser = state.authors[message.authorId]
                    val authorName =
                        if (isCurrentUser) {
                          stringResource(R.string.chat_author_you)
                        } else {
                          authorUser?.name ?: message.authorId
                        }
                    val formattedTime = formatTime(message.sentAt.toLocalTime())

                    ChatMessageRow(
                        message = message,
                        authorName = authorName,
                        authorUser = authorUser,
                        isCurrentUser = isCurrentUser,
                        formattedTime = formattedTime,
                    )
                  }
                }
              }
            }
          }
    }
  }
}

/**
 * Bottom bar component for entering and sending text messages.
 *
 * @param canSend Whether sending messages is enabled in current state.
 * @param onSendMessage Callback invoked when sending text.
 */
@Composable
private fun ChatInputBar(
    canSend: Boolean,
    onSendMessage: (String) -> Unit,
) {
  var textInput by remember { mutableStateOf("") }
  val sendEnabled = canSend && textInput.isNotBlank()

  Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    OutlinedTextField(
        value = textInput,
        onValueChange = { textInput = it },
        placeholder = { Text(stringResource(R.string.chat_input_placeholder)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag(ChatScreenTestTags.INPUT_FIELD),
        shape = RoundedCornerShape(24.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            ),
        trailingIcon = {
          IconButton(
              onClick = {
                if (sendEnabled) {
                  onSendMessage(textInput)
                  textInput = ""
                }
              },
              enabled = sendEnabled,
              modifier = Modifier.testTag(ChatScreenTestTags.SEND_BUTTON),
          ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.chat_send),
            )
          }
        },
    )
  }
}

@Preview(showBackground = true)
@Composable
internal fun ChatScreenPreview() {
  val userCamille = User("u1", name = "Camille", surname = "Durand")
  val userManon = User("u2", name = "Manon", surname = "Servoz")

  val sampleChat =
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
                  ChatMessage(
                      messageId = "m3",
                      authorId = "u1",
                      sentAt = ZonedDateTime.of(2026, 10, 17, 14, 0, 0, 0, ZoneOffset.UTC),
                      content = MessageContent.Text("Parfait ! Je prépare les chaises."),
                  ),
                  ChatMessage(
                      messageId = "m4",
                      authorId = "u2",
                      sentAt = ZonedDateTime.of(2026, 10, 17, 14, 1, 0, 0, ZoneOffset.UTC),
                      content = MessageContent.Text("Super, à tout l'heure !"),
                  ),
              ),
          members = listOf("u1", "u2"),
      )

  AppTheme {
    ChatContent(
        state =
            ChatUiState(
                chat = sampleChat,
                authors = mapOf("u1" to userCamille, "u2" to userManon),
                isLoading = false,
            ),
        currentUserId = "u2",
        chatTitle = "General",
        clockToday = LocalDate.of(2026, 10, 17),
    )
  }
}
