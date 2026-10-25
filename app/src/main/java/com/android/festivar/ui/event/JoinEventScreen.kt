// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.festivar.R
import com.android.festivar.model.event.JOIN_CODE_LENGTH
import com.android.festivar.ui.components.formatDate
import com.android.festivar.ui.components.formatTime
import com.android.festivar.ui.theme.AppTheme
import java.time.ZoneId
import java.time.ZonedDateTime

/** Test tags of the join event screen. */
object JoinEventScreenTestTags {
  const val BACK = "joinEventBack"
  const val TITLE = "joinEventTitle"
  const val CODE_INPUT = "joinEventCodeInput"
  const val SEVERAL = "joinEventSeveral"
  const val NOT_FOUND = "joinEventNotFound"
  const val ERROR_MESSAGE = "joinEventError"

  fun cell(i: Int) = "joinEventCodeCell$i"

  fun result(eventId: String) = "joinEventResult$eventId"

  fun resultName(eventId: String) = "joinEventResultName$eventId"

  fun resultLine(eventId: String) = "joinEventResultLine$eventId"

  fun join(eventId: String) = "joinEventJoin$eventId"
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
  LaunchedEffect(uiState.joinedEventId) {
    uiState.joinedEventId?.let {
      onOpenEvent(it)
      viewModel.onEventOpened()
    }
  }
  JoinEventContent(
      state = uiState,
      onBack = onBack,
      onCode = viewModel::setCode,
      onJoin = viewModel::join,
      requestFocusOnEntry = true,
  )
}

/**
 * The stateless join screen: six code cells over one hidden text field, then one card per event the
 * code matched (or a not found line), each with its own Join button since two events can share a
 * code. [onJoin] gets the id of the tapped card. The code field takes focus on entry only when
 * [requestFocusOnEntry] is set, which [JoinEventScreen] does.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinEventContent(
    state: JoinEventUIState,
    onBack: () -> Unit,
    onCode: (String) -> Unit,
    onJoin: (eventId: String) -> Unit,
    requestFocusOnEntry: Boolean = false,
) {
  val focus = remember { FocusRequester() }
  LaunchedEffect(Unit) { if (requestFocusOnEntry) focus.requestFocus() }
  val keyboard = LocalSoftwareKeyboardController.current
  // A failed join leaves the field disabled then enabled again, which drops its focus and closes
  // the keyboard; give both back so the person can retry without tapping a cell.
  LaunchedEffect(state.isJoining, state.errorMsg) {
    if (!state.isJoining && state.errorMsg != null) {
      focus.requestFocus()
      keyboard?.show()
    }
  }
  val codeFieldLabel = stringResource(R.string.join_event_code_field)
  val codeSoFar =
      if (state.code.isEmpty()) stringResource(R.string.join_event_code_empty)
      else stringResource(R.string.join_event_code_so_far, state.code)
  Scaffold(
      containerColor = MaterialTheme.colorScheme.surface,
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
                    contentDescription = stringResource(R.string.join_event_back),
                )
              }
            },
        )
      },
  ) { padding ->
    Column(
        Modifier.fillMaxSize()
            .padding(padding)
            .padding(horizontal = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
      Text(
          text = stringResource(R.string.join_event_title),
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.testTag(JoinEventScreenTestTags.TITLE),
      )
      Spacer(Modifier.height(8.dp))
      Text(
          text = stringResource(R.string.join_event_subtitle),
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
                    .semantics { contentDescription = codeFieldLabel }
                    .testTag(JoinEventScreenTestTags.CODE_INPUT),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.semantics { contentDescription = codeSoFar },
        ) {
          for (i in 0 until JOIN_CODE_LENGTH) {
            val active = i == state.code.length
            val cellLabel = stringResource(R.string.join_event_code_cell, i + 1)
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
                    .semantics {
                      role = Role.Button
                      contentDescription = cellLabel
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
      if (state.matches.size > 1) {
        Text(
            stringResource(R.string.join_event_several),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(JoinEventScreenTestTags.SEVERAL),
        )
        Spacer(Modifier.height(8.dp))
      }
      state.matches.forEach { found ->
        FoundCard(found, enabled = !state.isJoining, onJoin = { onJoin(found.id) })
        Spacer(Modifier.height(12.dp))
      }
      if (state.notFound) {
        Text(
            stringResource(R.string.join_event_not_found),
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

/** One matched event: its name, start and member count, and the button that joins it. */
@Composable
private fun FoundCard(found: FoundEventUi, enabled: Boolean, onJoin: () -> Unit) {
  Column(
      Modifier.fillMaxWidth()
          .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
          .padding(16.dp)
          .testTag(JoinEventScreenTestTags.result(found.id))
  ) {
    Text(
        found.name,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.testTag(JoinEventScreenTestTags.resultName(found.id)),
    )
    Text(
        stringResource(
            R.string.join_event_found_line,
            formatDate(found.start.toLocalDate()),
            formatTime(found.start.toLocalTime()),
            pluralStringResource(R.plurals.join_event_members, found.members, found.members),
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(JoinEventScreenTestTags.resultLine(found.id)),
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = onJoin,
        enabled = enabled,
        modifier =
            Modifier.fillMaxWidth().height(48.dp).testTag(JoinEventScreenTestTags.join(found.id)),
        shape = RoundedCornerShape(8.dp),
    ) {
      Text(text = stringResource(R.string.join_event_join), fontWeight = FontWeight.Bold)
    }
  }
}

@Preview
@Composable
private fun JoinEventContentPreview() {
  val previewStart = ZonedDateTime.of(2026, 10, 17, 10, 0, 0, 0, ZoneId.of("Europe/Zurich"))
  AppTheme {
    JoinEventContent(
        state =
            JoinEventUIState(
                code = "VIDY26",
                matches =
                    listOf(
                        FoundEventUi("e1", "Vidy Festival", previewStart, 12),
                        FoundEventUi("e2", "Vidy Afterparty", previewStart, 3),
                    ),
            ),
        onBack = {},
        onCode = {},
        onJoin = {},
    )
  }
}
