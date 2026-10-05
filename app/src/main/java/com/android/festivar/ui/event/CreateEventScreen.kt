package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A Screen for creating events. */
@Composable
fun CreateEventScreen(
    onBack: () -> Unit = {},
    onCreate: () -> Unit = {},
) {
  var name by rememberSaveable { mutableStateOf("") }
  var startDate by rememberSaveable { mutableStateOf("") }
  var endDate by rememberSaveable { mutableStateOf("") }
  var address by rememberSaveable { mutableStateOf("") }

  val formIsComplete =
      name.isNotBlank() && startDate.isNotBlank() && endDate.isNotBlank() && address.isNotBlank()

  Scaffold(
      topBar = { CreateEventTopBar(onBack = onBack) },
      content = { padding ->
        Column(
            modifier = Modifier.fillMaxHeight().fillMaxWidth().background(Color(0xFFFFF8FF)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Column(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 64.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Spacer(modifier = Modifier.height(64.dp))
            Text(
                text = "Create Event",
                color = Color(0xFF211F23),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(32.dp))

            EventInput(
                value = name,
                onValueChange = { name = it },
                placeholder = "Name"
            )
            Spacer(modifier = Modifier.height(12.dp))
            EventInput(
                value = startDate,
                onValueChange = { startDate = it },
                placeholder = "Start date"
            )
            Spacer(modifier = Modifier.height(12.dp))
            EventInput(
                value = endDate,
                onValueChange = { endDate = it },
                placeholder = "End Date"
            )
            Spacer(modifier = Modifier.height(12.dp))
            EventInput(
                value = address,
                onValueChange = { address = it },
                placeholder = "Address"
            )
          }

          Spacer(modifier = Modifier.weight(1f))
          Button(
              onClick = onCreate,
              enabled = formIsComplete,
              modifier =
                  Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(42.dp),
              shape = RoundedCornerShape(8.dp),
              colors =
                  ButtonDefaults.buttonColors(
                      disabledContainerColor = Color(0xFFE6E0E7),
                      disabledContentColor = Color(0xFF9A959C),
                      containerColor = MaterialTheme.colorScheme.primary,
                  ),
          ) {
            Text(text = "Create", fontSize = 12.sp)
          }
          Spacer(modifier = Modifier.height(16.dp))
        }
      },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateEventTopBar(onBack: () -> Unit) {
  TopAppBar(
      title = {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
          Text(text = "FestivAR")
        }
      },
      navigationIcon = {
        IconButton(
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent),
        ) {
          Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Navigate back",
          )
        }
      },
      actions = {
        IconButton(
            onClick = {},
        ) {
          Text("A")
        }
      },
  )
}

@Composable
private fun EventInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
  val shape = RoundedCornerShape(8.dp)
  BasicTextField(
      value = value,
      onValueChange = onValueChange,
      modifier =
          Modifier.fillMaxWidth()
              .height(40.dp)
              .clip(shape)
              .background(Color(0xFFE4DCFF)),
      singleLine = true,
      textStyle = TextStyle(color = Color(0xFF554C78), fontSize = 12.sp),
      cursorBrush = SolidColor(Color(0xFF7137F2)),
      decorationBox = { innerTextField ->
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
          if (value.isEmpty()) {
            Text(text = placeholder, color = Color(0xFF554C78), fontSize = 12.sp)
          }
          innerTextField()
        }
      },
  )
}

@Preview
@Composable
fun CreateEventScreenPreview() {
  CreateEventScreen()
}
