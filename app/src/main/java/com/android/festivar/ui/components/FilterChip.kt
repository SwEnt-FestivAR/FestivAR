// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.ui.theme.AppTheme

/** Filter chip of the design: ink when [selected], outlined otherwise, with a [count]. */
@Composable
fun FilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Surface(
      onClick = onClick,
      shape = CircleShape,
      color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
      border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = modifier.height(32.dp),
  ) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 12.dp),
    ) {
      Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color =
              if (selected) MaterialTheme.colorScheme.onSecondaryContainer
              else MaterialTheme.colorScheme.onSurface,
      )
      Text(
          text = count.toString(),
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color =
              if (selected) MaterialTheme.colorScheme.onSecondaryContainer
              else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun FilterChipPreview() {
  AppTheme {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
      FilterChip(label = "All", count = 12, selected = true, onClick = {})
      FilterChip(label = "Ongoing", count = 3, selected = false, onClick = {})
    }
  }
}
