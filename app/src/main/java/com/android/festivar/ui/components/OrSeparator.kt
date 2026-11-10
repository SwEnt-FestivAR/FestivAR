package com.android.festivar.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.festivar.R

/** Separator with "or" in the middle */
@Composable
fun OrSeparator(modifier: Modifier = Modifier) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier.fillMaxWidth(),
  ) {
    HorizontalDivider(
        modifier = Modifier.weight(1f),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
    Text(
        text = stringResource(R.string.login_or),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = 10.dp),
    )
    HorizontalDivider(
        modifier = Modifier.weight(1f),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
  }
}
