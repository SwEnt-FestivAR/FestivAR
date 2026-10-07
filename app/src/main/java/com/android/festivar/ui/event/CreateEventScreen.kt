package com.android.festivar.ui.event

// Co-authored-by: Copilot App

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val ScreenBackground = Color(0xFFFFFEF9)
private val FieldBorder = Color(0xFFD7D5CC)
private val PrimaryButton = Color(0xFFC7F21A)
private val SecondaryText = Color(0xFF77766F)

object CreateEventScreenTestTags {
  const val NAVIGATION_BUTTON = "navigationButton"
  const val NAME_FIELD = "nameField"
  const val START_DATE_FIELD = "startDateField"
  const val END_DATE_FIELD = "endDateField"
  const val LOCATION_FIELD = "locationField"
  const val DESCRIPTION_FIELD = "descriptionField"
  const val CREATE_BUTTON = "createButton"
}

/** A screen for creating events. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    viewModel: CreateEventViewModel = viewModel(),
    onBack: () -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsState()

  Scaffold(
      containerColor = ScreenBackground,
      topBar = { CreateEventTopBar(onBack = onBack) },
      bottomBar = {
        Button(
            onClick = { viewModel.createEvent() },
            enabled = viewModel.formIsComplete(),
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
          Text(
              text = "Create",
              fontWeight = FontWeight.Bold,
          )
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
      EventInput(
          value = uiState.name,
          onValueChange = { viewModel.updateName(it) },
          placeholder = "Event name",
          modifier = Modifier.testTag(CreateEventScreenTestTags.NAME_FIELD),
      )
      Spacer(modifier = Modifier.height(14.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DateTimeInput(
            value = uiState.startDate.toDisplayDate(),
            onClick = { viewModel.updateActiveDatePicker(DateField.START) },
            placeholder = "Starts",
            modifier = Modifier.weight(1f).testTag(CreateEventScreenTestTags.START_DATE_FIELD),
        )
        DateTimeInput(
            value = uiState.endDate.toDisplayDate(),
            onClick = { viewModel.updateActiveDatePicker(DateField.END) },
            placeholder = "Ends",
            modifier = Modifier.weight(1f).testTag(CreateEventScreenTestTags.END_DATE_FIELD),
        )
      }
      Spacer(modifier = Modifier.height(14.dp))

      EventInput(
          value = uiState.venue,
          onValueChange = { viewModel.updateVenue(it) },
          placeholder = "Venue",
          modifier = Modifier.testTag(CreateEventScreenTestTags.LOCATION_FIELD),
      )
      Spacer(modifier = Modifier.height(8.dp))

      EventInput(
          value = uiState.notes,
          onValueChange = { viewModel.updateNotes(it) },
          placeholder = "Notes for the team, optional",
          minHeight = 56.dp,
          singleLine = false,
          modifier = Modifier.testTag(CreateEventScreenTestTags.DESCRIPTION_FIELD),
      )
      Spacer(modifier = Modifier.height(14.dp))
    }
  }

  uiState.activeDatePicker?.let { dateField ->
    val initialDateMillis = if (dateField == DateField.START) uiState.startDateMillis else uiState.endDateMillis
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

    DatePickerDialog(
        onDismissRequest = { viewModel.updateActiveDatePicker(null) },
        confirmButton = {
          Button(
              onClick = {
                val selectedDateMillis = datePickerState.selectedDateMillis
                if (dateField == DateField.START) {
                  viewModel.updateStartDateMillis(selectedDateMillis!!)
                  viewModel.updateStartDate(selectedDateMillis.toZonedDateTime())
                } else {
                  viewModel.updateEndDateMillis(selectedDateMillis!!)
                  viewModel.updateEndDate(selectedDateMillis.toZonedDateTime())
                }
                viewModel.updateActiveDatePicker(null)
              },
          ) {
            Text("OK")
          }
        },
        dismissButton = { Button(onClick = { viewModel.updateActiveDatePicker(null) }) { Text("Cancel") } },
    ) {
      DatePicker(state = datePickerState)
    }
  }
}

private fun Long.toZonedDateTime(
    zone: ZoneId = ZoneId.systemDefault(),
): ZonedDateTime = Instant.ofEpochMilli(this).atZone(zone)

private fun ZonedDateTime?.toDisplayDate(
    locale: Locale = Locale.getDefault(),
): String =
    this?.let {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale)
            .format(it)
    } ?: ""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateEventTopBar(onBack: () -> Unit) {
  TopAppBar(
      title = {
        Text(
            text = "New event",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
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

@Composable
private fun EventInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = 40.dp,
    singleLine: Boolean = true,
) {
  val shape = RoundedCornerShape(8.dp)
  BasicTextField(
      value = value,
      onValueChange = onValueChange,
      modifier =
          modifier
              .fillMaxWidth()
              .height(minHeight)
              .clip(shape)
              .background(Color.White)
              .border(1.dp, FieldBorder, shape),
      singleLine = singleLine,
      readOnly = readOnly,
      enabled = enabled,
      textStyle =
          TextStyle(
              color = Color(0xFF30302C),
              fontSize = 13.sp,
          ),
      decorationBox = { innerTextField ->
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
        ) {
          if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = SecondaryText,
                fontSize = 11.sp,
            )
          }
          innerTextField()
        }
      },
  )
}

@Composable
private fun DateTimeInput(
    value: String,
    onClick: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
  Box(
      modifier =
          modifier
              .clip(RoundedCornerShape(8.dp))
              .background(Color.White)
              .clickable(onClick = onClick),
  ) {
    EventInput(
        value = value,
        onValueChange = {},
        placeholder = placeholder,
        readOnly = true,
        enabled = false,
    )
  }
}

@Preview
@Composable
fun CreateEventScreenPreview() {
  CreateEventScreen()
}
