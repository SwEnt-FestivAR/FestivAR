package com.android.festivar.ui.event

// Co-authored-by: Copilot App
// Co-authored-by: Claude Fable 5.1 <noreply@anthropic.com>

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.ui.components.FestivarDatePickerDialog
import com.android.festivar.ui.components.FestivarFieldError
import com.android.festivar.ui.components.FestivarPickerField
import com.android.festivar.ui.components.FestivarTextField
import com.android.festivar.ui.theme.AppTheme
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val ScreenBackground = Color(0xFFFFFEF9)
private val PrimaryButton = Color(0xFFC7F21A)

object CreateEventScreenTestTags {
  const val NAVIGATION_BUTTON = "navigationButton"
  const val TITLE = "title"
  const val NAME_FIELD = "nameField"
  const val START_DATE_FIELD = "startDateField"
  const val END_DATE_FIELD = "endDateField"
  const val LOCATION_FIELD = "locationField"
  const val DESCRIPTION_FIELD = "descriptionField"
  const val NAME_ERROR = "nameError"
  const val ENDS_ERROR = "endsError"
  const val ADDRESS_ERROR = "addressError"
  const val CREATE_BUTTON = "createButton"
  const val SAVING = "createEventSaving"
  const val SAVE_ERROR = "createEventSaveError"
}

/**
 * A screen for creating events.
 *
 * @param onCreated Called once the event is saved, so the caller can leave the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    viewModel: CreateEventViewModel = viewModel(),
    onBack: () -> Unit = {},
    onCreated: () -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsState()
  LaunchedEffect(uiState.createdEventId) { if (uiState.createdEventId != null) onCreated() }

  Scaffold(
      containerColor = ScreenBackground,
      topBar = { CreateEventTopBar(onBack = onBack) },
      bottomBar = {
        Column {
          uiState.errorMsg?.let {
            FestivarFieldError(
                message = stringResource(it),
                modifier = Modifier.testTag(CreateEventScreenTestTags.SAVE_ERROR),
            )
          }
          Button(
              onClick = {
                if (viewModel.validEvent()) viewModel.createEvent() else viewModel.setErrors()
              },
              enabled = !uiState.isSaving,
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(horizontal = 12.dp, vertical = 12.dp)
                      .height(48.dp)
                      .testTag(CreateEventScreenTestTags.CREATE_BUTTON),
              shape = RoundedCornerShape(8.dp),
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = PrimaryButton,
                      contentColor = Color(0xFF202020),
                      disabledContainerColor = Color(0xFFE6E4DE),
                      disabledContentColor = Color(0xFF99978F),
                  ),
          ) {
            if (uiState.isSaving) {
              CircularProgressIndicator(
                  modifier = Modifier.size(16.dp).testTag(CreateEventScreenTestTags.SAVING),
                  color = Color(0xFF99978F),
                  strokeWidth = 2.dp,
              )
            } else {
              Text(text = "Create event", fontWeight = FontWeight.Bold)
            }
          }
        }
      },
  ) { padding ->
    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
    ) {
      FestivarTextField(
          value = uiState.name,
          onValueChange = {
            if (uiState.errorName) {
              viewModel.removeErrorName()
            }
            viewModel.updateName(it)
          },
          label = "Event name",
          isError = uiState.errorName,
          modifier = Modifier.testTag(CreateEventScreenTestTags.NAME_FIELD),
      )
      if (uiState.errorName) {
        FestivarFieldError(
            message = "Give the event a name",
            modifier = Modifier.testTag(CreateEventScreenTestTags.NAME_ERROR),
        )
      }
      Spacer(modifier = Modifier.height(14.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FestivarPickerField(
            value = uiState.startDate.toDisplayDate(),
            onClick = {
              if (uiState.errorDate) {
                viewModel.removeErrorDate()
              }
              viewModel.updateActiveDatePicker(DateField.START)
            },
            label = "Starts",
            isError = uiState.errorDate && uiState.startDate == null,
            modifier = Modifier.weight(1f).testTag(CreateEventScreenTestTags.START_DATE_FIELD),
        )
        FestivarPickerField(
            value = uiState.endDate.toDisplayDate(),
            onClick = {
              if (uiState.errorDate) {
                viewModel.removeErrorDate()
              }
              viewModel.updateActiveDatePicker(DateField.END)
            },
            label = "Ends",
            isError = uiState.errorDate,
            modifier = Modifier.weight(1f).testTag(CreateEventScreenTestTags.END_DATE_FIELD),
        )
      }
      if (uiState.errorDate) {
        FestivarFieldError(
            message =
                if (uiState.startDate == null || uiState.endDate == null) "Say when it happens"
                else "Ends before it starts",
            modifier = Modifier.testTag(CreateEventScreenTestTags.ENDS_ERROR),
        )
      }
      Spacer(modifier = Modifier.height(14.dp))

      FestivarTextField(
          value = uiState.venue,
          onValueChange = {
            if (uiState.errorVenue) {
              viewModel.removeErrorVenue()
            }
            viewModel.updateVenue(it)
          },
          label = "Venue",
          isError = uiState.errorVenue,
          modifier = Modifier.testTag(CreateEventScreenTestTags.LOCATION_FIELD),
      )
      if (uiState.errorVenue) {
        FestivarFieldError(
            message = "Say where it happens",
            modifier = Modifier.testTag(CreateEventScreenTestTags.ADDRESS_ERROR),
        )
      }
      Spacer(modifier = Modifier.height(8.dp))

      FestivarTextField(
          value = uiState.notes,
          onValueChange = { viewModel.updateNotes(it) },
          label = "Notes for the team, optional",
          singleLine = false,
          modifier = Modifier.height(56.dp).testTag(CreateEventScreenTestTags.DESCRIPTION_FIELD),
      )
      Spacer(modifier = Modifier.height(14.dp))
    }
  }

  uiState.activeDatePicker?.let { dateField ->
    FestivarDatePickerDialog(
        initial =
            if (dateField == DateField.START) {
              uiState.startDate?.toLocalDate()
            } else {
              uiState.endDate?.toLocalDate()
            },
        onConfirm = { selectedDate ->
          // The form takes days: an event starts when its first day starts and ends when its last
          // day ends, so a one day event is valid and stays ongoing until its midnight.
          val zone = ZoneId.systemDefault()
          if (dateField == DateField.START) {
            val start = selectedDate.atStartOfDay(zone)
            viewModel.updateStartDateMillis(start.toInstant().toEpochMilli())
            viewModel.updateStartDate(start)
          } else {
            val end = selectedDate.plusDays(1).atStartOfDay(zone).minusSeconds(1)
            viewModel.updateEndDateMillis(end.toInstant().toEpochMilli())
            viewModel.updateEndDate(end)
          }
          viewModel.updateActiveDatePicker(null)
        },
        onDismiss = { viewModel.updateActiveDatePicker(null) },
    )
  }
}

private fun ZonedDateTime?.toDisplayDate(
    locale: Locale = Locale.getDefault(),
): String =
    this?.let {
      DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(it)
    } ?: ""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateEventTopBar(onBack: () -> Unit) {
  TopAppBar(
      title = {
        Text(
            text = "Create event",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag(CreateEventScreenTestTags.TITLE),
        )
      },
      navigationIcon = {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag(CreateEventScreenTestTags.NAVIGATION_BUTTON),
        ) {
          Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Navigate back",
          )
        }
      },
      actions = {},
  )
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview
@Composable
fun CreateEventScreenPreview() {
  AppTheme { CreateEventScreen(CreateEventViewModel(EventsRepositoryLocal())) }
}
