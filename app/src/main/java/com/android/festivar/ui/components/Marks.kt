// Written with the help of an AI coding assistant and reviewed line by line by the author.
package com.android.festivar.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.festivar.R

/**
 * Which pin this is, which decides its colour and what a screen reader says for it. The lime is
 * only ever "mine" or "done".
 */
enum class PinKind(val description: Int) {
  STRUCTURE(R.string.pin_open),
  MINE(R.string.pin_mine),
  TAKEN(R.string.pin_taken),
  DONE(R.string.pin_done),
}

/** The pennant pin: the one motif. Map marker, task bullet, empty state. */
@Composable
fun PennantIcon(
    modifier: Modifier = Modifier,
    kind: PinKind = PinKind.STRUCTURE,
    size: Dp = 24.dp,
    contentDescription: String? = null,
) {
  val scheme = MaterialTheme.colorScheme
  val tint =
      when (kind) {
        PinKind.STRUCTURE -> scheme.onSurface
        PinKind.MINE -> scheme.primary
        PinKind.TAKEN -> scheme.tertiary
        PinKind.DONE -> scheme.onPrimaryContainer
      }
  Icon(
      imageVector = Pennant,
      contentDescription = contentDescription,
      tint = tint,
      modifier = modifier.size(size),
  )
}

// Pole, flag and ground ring. The black is a placeholder: Icon tints the whole vector.
private val Pennant: ImageVector =
    ImageVector.Builder("Pennant", 24.dp, 24.dp, 24f, 24f)
        .apply {
          path(
              stroke = SolidColor(Color.Black),
              strokeLineWidth = 2.4f,
              strokeLineCap = StrokeCap.Round,
          ) {
            moveTo(9.2f, 3.2f)
            verticalLineTo(18.8f)
          }
          path(
              fill = SolidColor(Color.Black),
              stroke = SolidColor(Color.Black),
              strokeLineWidth = 1.2f,
              strokeLineJoin = StrokeJoin.Round,
          ) {
            moveTo(9.2f, 3.2f)
            lineTo(19.4f, 7.9f)
            lineTo(9.2f, 12.6f)
            close()
          }
          path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.2f) {
            moveTo(4.6f, 19.6f)
            arcToRelative(4.6f, 2.3f, 0f, true, false, 9.2f, 0f)
            arcToRelative(4.6f, 2.3f, 0f, true, false, -9.2f, 0f)
            close()
          }
        }
        .build()
