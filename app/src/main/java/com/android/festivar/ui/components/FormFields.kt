// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.ui.theme.AppTheme

private val FieldShape = RoundedCornerShape(12.dp)

/** Default maximum number of characters of a [FestivarTextField], enough for a title or a name. */
const val DEFAULT_MAX_LENGTH = 80

/** Maximum number of characters of a long, multi-line [FestivarTextField], e.g. a description. */
const val LONG_TEXT_MAX_LENGTH = 500

/** Test tags of [FestivarTextField]. */
object FestivarTextFieldTestTags {
  /** The "length/maxLength" counter under the field, shown when `showCounter` is true. */
  const val COUNTER = "festivarTextFieldCounter"
}

/**
 * Fits the edit of a text field from [old] to [new] in [maxLength] characters by cutting only what
 * was typed or pasted, so the text before and after the cursor is never lost. An [old] text already
 * longer than [maxLength] (saved before the limit) is kept, but cannot grow.
 *
 * The typed or pasted part is what lies between the start and the end that [new] shares with [old].
 * For example, with a limit of 5, typing "X" in the middle of "abcde" keeps "abcde", and pasting
 * "0123456789" over the whole of "abc" gives "01234".
 */
internal fun limitEdit(old: String, new: String, maxLength: Int): String {
  val limit = maxOf(maxLength, old.length)
  if (new.length <= limit) return new
  val start = new.commonPrefixWith(old).length
  val end = minOf(new.commonSuffixWith(old).length, old.length - start)
  val typed = new.substring(start, new.length - end)
  val room = limit - start - end
  // An emoji or other character outside the BMP takes two chars: never keep only its first half.
  val cut = if (room > 0 && typed[room - 1].isHighSurrogate()) room - 1 else room
  return new.take(start) + typed.take(cut) + new.takeLast(end)
}

/**
 * Colors of the fields. Material draws the focused border, label and cursor in `primary`, the lime
 * of the app, which is too flashy and hard to read on the light background. They use the dark green
 * of the theme instead; the other states keep the Material defaults.
 */
@Composable
private fun festivarFieldColors(): TextFieldColors {
  val accent = MaterialTheme.colorScheme.onPrimaryContainer
  return OutlinedTextFieldDefaults.colors(
      focusedBorderColor = accent,
      focusedLabelColor = accent,
      cursorColor = accent,
  )
}

/**
 * Outlined text field in the FestivAR style, with its label sitting on the border.
 *
 * Colors and text style come from the app theme. Pass a test tag through [modifier]. Show an error
 * message under the field with [FestivarFieldError], and set [isError] to draw the field as
 * invalid.
 *
 * @param enabled Whether the field can be edited. A disabled one is greyed out and takes no input.
 * @param keyboardOptions The keyboard to show, e.g. `KeyboardOptions(keyboardType =
 *   KeyboardType.Email)`.
 * @param visualTransformation How the text is displayed, e.g. [PasswordVisualTransformation] to
 *   hide it.
 * @param trailingIcon Content at the end of the field, e.g. a show/hide password button.
 * @param maxLength Maximum number of characters. Text typed or pasted past it is cut off, see
 *   [limitEdit]. A longer text the field already holds is kept, and the field shows as invalid.
 * @param showCounter Whether to show a "length/maxLength" counter under the field.
 */
@Composable
fun FestivarTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    maxLength: Int = DEFAULT_MAX_LENGTH,
    showCounter: Boolean = false,
) {
  OutlinedTextField(
      value = value,
      onValueChange = { onValueChange(limitEdit(value, it, maxLength)) },
      modifier = modifier.fillMaxWidth(),
      enabled = enabled,
      label = { Text(label) },
      textStyle = MaterialTheme.typography.bodyLarge,
      singleLine = singleLine,
      isError = isError || value.length > maxLength,
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
      visualTransformation = visualTransformation,
      trailingIcon = trailingIcon,
      supportingText =
          if (showCounter) {
            {
              Text(
                  text = "${value.length}/$maxLength",
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.testTag(FestivarTextFieldTestTags.COUNTER),
              )
            }
          } else null,
      colors = festivarFieldColors(),
      shape = FieldShape,
  )
}

/**
 * Field that looks like [FestivarTextField] but is not typed into: tapping it calls [onClick], to
 * open a picker. It shows [value] (empty while nothing is picked).
 */
@Composable
fun FestivarPickerField(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
  val currentOnClick by rememberUpdatedState(onClick)
  val interactionSource = remember { MutableInteractionSource() }
  // A read-only text field has no click callback, but it still reports the press on its source.
  LaunchedEffect(interactionSource) {
    interactionSource.interactions.collect { interaction ->
      if (interaction is PressInteraction.Release) currentOnClick()
    }
  }

  OutlinedTextField(
      value = value,
      onValueChange = {},
      modifier = modifier.fillMaxWidth(),
      readOnly = true,
      label = { Text(label) },
      textStyle = MaterialTheme.typography.bodyLarge,
      singleLine = true,
      isError = isError,
      colors = festivarFieldColors(),
      shape = FieldShape,
      interactionSource = interactionSource,
  )
}

/**
 * Supporting line shown under a field that has an error, as in the Figma "Supporting text": Body
 * small in the theme error color, inset like the field's own text.
 */
@Composable
fun FestivarFieldError(message: String, modifier: Modifier = Modifier) {
  Text(
      text = message,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.error,
      modifier = modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp),
  )
}

/** What is wrong with a field of a form, shown as a message under that field. */
enum class FieldError {
  /** The title was edited and is now blank. */
  EMPTY_TITLE,

  /** The event name was edited and is now blank. */
  EMPTY_EVENT_NAME,

  /** The announcement title was edited and is now blank. */
  EMPTY_ANNOUNCEMENT_TITLE,

  /** The announcement content was edited and is now blank. */
  EMPTY_ANNOUNCEMENT_CONTENT,

  /** A time was picked without its date. */
  MISSING_DATE,

  /** The end is before the start. */
  END_BEFORE_START,

  /** The estimated time is not a number of minutes, at least 1. */
  INVALID_ESTIMATED_TIME,

  /** The selected DateTime range is in the past. */
  OUTDATED,

  /** Fewer people than are already assigned. */
  TOO_FEW_SLOTS,
}

/** The string resource of the message shown under a field for [error]. */
@StringRes
fun fieldErrorMessageRes(error: FieldError): Int =
    when (error) {
      FieldError.EMPTY_TITLE -> R.string.field_error_empty_title
      FieldError.EMPTY_EVENT_NAME -> R.string.field_error_empty_event_name
      FieldError.EMPTY_ANNOUNCEMENT_TITLE -> R.string.field_error_empty_announcement_title
      FieldError.EMPTY_ANNOUNCEMENT_CONTENT -> R.string.field_error_empty_announcement_content
      FieldError.MISSING_DATE -> R.string.field_error_missing_date
      FieldError.END_BEFORE_START -> R.string.field_error_end_before_start
      FieldError.INVALID_ESTIMATED_TIME -> R.string.field_error_invalid_estimated_time
      FieldError.OUTDATED -> R.string.field_error_outdated
      FieldError.TOO_FEW_SLOTS -> R.string.field_error_too_few_slots
    }

/**
 * Shows [field] with the message for [error] under it, when there is one.
 *
 * @param errorTag Test tag of the error message.
 */
@Composable
fun FestivarFieldWithError(
    error: FieldError?,
    errorTag: String,
    modifier: Modifier = Modifier,
    field: @Composable () -> Unit,
) {
  Column(modifier = modifier) {
    field()
    error?.let {
      FestivarFieldError(
          message = stringResource(fieldErrorMessageRes(it)),
          modifier = Modifier.testTag(errorTag),
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
internal fun FormFieldsPreview() {
  AppTheme {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
      FestivarTextField(value = "", onValueChange = {}, label = "Title")
      FestivarTextField(value = "Run power to stage", onValueChange = {}, label = "Title")
      FestivarTextField(
          value = "secret",
          onValueChange = {},
          label = "Password",
          visualTransformation = PasswordVisualTransformation(),
      )
      FestivarTextField(
          value = "Bring the cables from the storage room to the main stage.",
          onValueChange = {},
          label = "Description",
          singleLine = false,
          maxLength = LONG_TEXT_MAX_LENGTH,
          showCounter = true,
      )
      Column {
        FestivarTextField(value = "", onValueChange = {}, label = "Title", isError = true)
        FestivarFieldError(message = "Title cannot be empty")
      }
      FestivarPickerField(value = "Sat 17 Oct", label = "Start date", onClick = {})
    }
  }
}
