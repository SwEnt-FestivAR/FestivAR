// Written with the help of an AI coding assistant and reviewed line by line by the author.
package com.android.festivar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
