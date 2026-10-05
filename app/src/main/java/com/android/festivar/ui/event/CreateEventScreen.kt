package com.android.festivar.ui.event

// Co-authored-by: Copilot App

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

  Box(
      modifier = Modifier.fillMaxSize().background(Color(0xFFECECEC)).padding(horizontal = 12.dp),
  ) {
    Column(
        modifier =
            Modifier.fillMaxHeight().fillMaxWidth().background(Color(0xFFFFF8FF)).semantics {
              contentDescription = "Create event screen"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      CreateEventTopBar(onBack = onBack)

      Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
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
            placeholder = "Name",
            contentDescription = "Event name",
        )
        Spacer(modifier = Modifier.height(12.dp))
        EventInput(
            value = startDate,
            onValueChange = { startDate = it },
            placeholder = "Start date",
            contentDescription = "Event start date",
        )
        Spacer(modifier = Modifier.height(12.dp))
        EventInput(
            value = endDate,
            onValueChange = { endDate = it },
            placeholder = "End Date",
            contentDescription = "Event end date",
        )
        Spacer(modifier = Modifier.height(12.dp))
        EventInput(
            value = address,
            onValueChange = { address = it },
            placeholder = "Address",
            contentDescription = "Event address",
        )
      }

      Spacer(modifier = Modifier.weight(1f))
      Button(
          onClick = onCreate,
          enabled = formIsComplete,
          modifier =
              Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(42.dp).semantics {
                contentDescription = "Create event"
              },
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
  }
}

@Composable
private fun CreateEventTopBar(onBack: () -> Unit) {
  Row(
      modifier =
          Modifier.fillMaxWidth()
              .height(50.dp)
              .border(width = 2.dp, color = Color(0xFF853BFF))
              .padding(horizontal = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
        text = "\u2190",
        modifier =
            Modifier.size(28.dp).clickable(onClick = onBack).semantics {
              contentDescription = "Navigate back"
            },
        color = Color(0xFF211F23),
        fontSize = 24.sp,
    )
    FestivalMark()
    Box(
        modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF665A96)),
        contentAlignment = Alignment.Center,
    ) {
      Text(text = "A", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun FestivalMark() {
  Canvas(
      modifier = Modifier.size(32.dp).semantics { contentDescription = "Festival logo" },
  ) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension * 0.4f
    repeat(8) { index ->
      val angle = Math.toRadians(index * 45.0)
      val start =
          Offset(
              x = center.x + (radius * 0.35f * kotlin.math.cos(angle)).toFloat(),
              y = center.y + (radius * 0.35f * kotlin.math.sin(angle)).toFloat(),
          )
      val end =
          Offset(
              x = center.x + (radius * kotlin.math.cos(angle)).toFloat(),
              y = center.y + (radius * kotlin.math.sin(angle)).toFloat(),
          )
      drawLine(
          color = Color(0xFF7137F2),
          start = start,
          end = end,
          strokeWidth = 2.5.dp.toPx(),
          cap = StrokeCap.Round,
      )
    }
  }
}

@Composable
private fun EventInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    contentDescription: String,
) {
  TextField(
      value = value,
      onValueChange = onValueChange,
      modifier =
          Modifier.fillMaxWidth().height(40.dp).semantics {
            this.contentDescription = contentDescription
          },
      placeholder = { Text(text = placeholder, color = Color(0xFF554C78), fontSize = 12.sp) },
      singleLine = true,
      shape = RoundedCornerShape(8.dp),
      colors =
          TextFieldDefaults.colors(
              focusedContainerColor = Color(0xFFE4DCFF),
              unfocusedContainerColor = Color(0xFFE4DCFF),
              disabledContainerColor = Color(0xFFE4DCFF),
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
              disabledIndicatorColor = Color.Transparent,
              cursorColor = Color(0xFF7137F2),
          ),
  )
}


@Preview
@Composable
fun CreateEventScreenPreview() {
    CreateEventScreen()
}
