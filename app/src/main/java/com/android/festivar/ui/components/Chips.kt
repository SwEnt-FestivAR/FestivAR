package com.android.festivar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A filter chip with an optional tabular count: "Open 3". Selected is ink on light. */
@Composable
fun CountChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
  val scheme = MaterialTheme.colorScheme
  FilterChip(
      selected = selected,
      onClick = onClick,
      modifier = modifier.height(32.dp),
      shape = CircleShape,
      label = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(label, style = MaterialTheme.typography.labelMedium)
          if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = if (selected) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
            )
          }
        }
      },
      colors =
          FilterChipDefaults.filterChipColors(
              selectedContainerColor = scheme.secondaryContainer,
              selectedLabelColor = scheme.onSecondaryContainer,
              labelColor = scheme.onSurface,
          ),
      border =
          FilterChipDefaults.filterChipBorder(
              enabled = true,
              selected = selected,
              borderColor = scheme.outline,
              selectedBorderColor = Color.Transparent,
          ),
  )
}

/** Small caps section label: "Now", "Morning". */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
  Text(
      text = text.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
  )
}

/** A 1dp rule in the hairline colour. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
  Box(modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
}
