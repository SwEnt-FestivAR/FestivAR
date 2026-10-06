package com.android.festivar.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * One task in a list: pennant bullet, title, where and what is needed, the time first on the right,
 * then a "1 of 2" progress. The caller decides every word.
 *
 * @param subtitle what the row says under the title; hidden when empty.
 * @param time the start, already formatted; hidden when empty.
 * @param trailing what sits under the time, such as "1 of 2".
 */
@Composable
fun TaskRow(
    title: String,
    subtitle: String,
    time: String,
    trailing: String,
    pinKind: PinKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Row(
      modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
      verticalAlignment = Alignment.Top,
  ) {
    PennantIcon(kind = pinKind, size = 20.dp, modifier = Modifier.padding(top = 2.dp))
    Spacer(Modifier.width(12.dp))
    Column(Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleSmall)
      if (subtitle.isNotEmpty()) {
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    Spacer(Modifier.width(12.dp))
    Column(horizontalAlignment = Alignment.End) {
      if (time.isNotEmpty()) {
        Text(time, style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"))
        Spacer(Modifier.height(4.dp))
      }
      Text(
          trailing,
          style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
