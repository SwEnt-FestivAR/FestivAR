// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.festivar.R
import com.android.festivar.model.event.JOIN_CODE_LENGTH
import com.android.festivar.ui.theme.AppTheme

private val ScreenBackground = Color(0xFFFFFEF9)
private val PrimaryButton = Color(0xFFC7F21A)

/** Test tags of the join event screen. */
object JoinEventScreenTestTags {
  const val BACK = "joinEventBack"
  const val TITLE = "joinEventTitle"
  const val CODE_INPUT = "joinEventCodeInput"
  const val RESULT = "joinEventResult"
  const val RESULT_NAME = "joinEventResultName"
  const val RESULT_LINE = "joinEventResultLine"
  const val NOT_FOUND = "joinEventNotFound"
  const val JOIN = "joinEventJoin"
  const val ERROR_MESSAGE = "joinEventError"

  fun cell(i: Int) = "codeCell$i"
}

/**
 * Joins an event by the six character code its organizer shared.
 *
 * Collects the state of [viewModel], hands it to [JoinEventContent], and calls [onOpenEvent] with
 * the event id once the join succeeded.
 */
@Composable
fun JoinEventScreen(
    viewModel: JoinEventViewModel,
    onBack: () -> Unit = {},
    onOpenEvent: (eventId: String) -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  LaunchedEffect(uiState.joinedEventId) { uiState.joinedEventId?.let(onOpenEvent) }
  JoinEventContent(
      state = uiState,
      onBack = onBack,
      onCode = viewModel::setCode,
      onJoin = viewModel::join,
      requestFocusOnEntry = true,
  )
}

/**
 * The stateless join screen: six code cells over one hidden text field, the event found for the
 * code (or a not found line), and the Join button. The code field takes focus on entry only when
 * [requestFocusOnEntry] is set, which [JoinEventScreen] does.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinEventContent(
    state: JoinEventUiState,
    onBack: () -> Unit,
    onCode: (String) -> Unit,
    onJoin: () -> Unit,
    requestFocusOnEntry: Boolean = false,
) {
  val focus = remember { FocusRequester() }
  LaunchedEffect(Unit) { if (requestFocusOnEntry) focus.requestFocus() }
  val keyboard = LocalSoftwareKeyboardController.current
  Scaffold(
      containerColor = ScreenBackground,
      topBar = {
        TopAppBar(
            title = {},
            navigationIcon = {
              IconButton(
                  onClick = onBack,
                  modifier = Modifier.testTag(JoinEventScreenTestTags.BACK),
              ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Navigate back",
                )
              }
            },
        )
      },
      bottomBar = {
        Button(
            onClick = onJoin,
            enabled = state.found != null && !state.isJoining,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .height(48.dp)
                    .testTag(JoinEventScreenTestTags.JOIN),
            shape = RoundedCornerShape(8.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = PrimaryButton,
                    contentColor = Color(0xFF202020),
                    disabledContainerColor = Color(0xFFE6E4DE),
                    disabledContentColor = Color(0xFF99978F),
                ),
        ) {
          Text(text = "Join as a volunteer", fontWeight = FontWeight.Bold)
        }
      },
  ) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
      Text(
          text = "Join by code",
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.testTag(JoinEventScreenTestTags.TITLE),
      )
      Spacer(Modifier.height(8.dp))
      Text(
          text = "Enter the six character code the organizer shared with you.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(24.dp))
      Box {
        BasicTextField(
            value = state.code,
            onValueChange = onCode,
            enabled = !state.isJoining,
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii,
                    autoCorrectEnabled = false,
                ),
            modifier =
                Modifier.matchParentSize()
                    .alpha(0f)
                    .focusRequester(focus)
                    .testTag(JoinEventScreenTestTags.CODE_INPUT),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          for (i in 0 until JOIN_CODE_LENGTH) {
            val active = i == state.code.length
            Box(
                Modifier.weight(1f)
                    .height(60.dp)
                    .border(
                        if (active) 2.dp else 1.dp,
                        if (active) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(12.dp),
                    )
                    .clickable {
                      focus.requestFocus()
                      keyboard?.show()
                    }
                    .testTag(JoinEventScreenTestTags.cell(i)),
                contentAlignment = Alignment.Center,
            ) {
              Text(
                  state.code.getOrNull(i)?.toString().orEmpty(),
                  style = MaterialTheme.typography.headlineMedium,
              )
            }
          }
        }
      }
      Spacer(Modifier.height(16.dp))
      val found = state.found
      if (found != null) {
        FoundCard(found)
      } else if (state.notFound) {
        Text(
            "No event matches this code.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag(JoinEventScreenTestTags.NOT_FOUND),
        )
      }
      state.errorMsg?.let {
        Spacer(Modifier.height(8.dp))
        Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag(JoinEventScreenTestTags.ERROR_MESSAGE),
        )
      }
    }
  }
}

@Composable
private fun FoundCard(found: FoundEventUi) {
  Column(
      Modifier.fillMaxWidth()
          .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
          .padding(16.dp)
          .testTag(JoinEventScreenTestTags.RESULT)
  ) {
    Text(
        found.name,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.testTag(JoinEventScreenTestTags.RESULT_NAME),
    )
    Text(
        "${found.dateLine}, " +
            pluralStringResource(R.plurals.join_event_members, found.members, found.members),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(JoinEventScreenTestTags.RESULT_LINE),
    )
  }
}

@Preview
@Composable
private fun JoinEventContentPreview() {
  AppTheme {
    JoinEventContent(
        state =
            JoinEventUiState(
                code = "VIDY26",
                found = FoundEventUi("e1", "Vidy Festival", "Sat 17 Oct", 12),
            ),
        onBack = {},
        onCode = {},
        onJoin = {},
    )
  }
}
