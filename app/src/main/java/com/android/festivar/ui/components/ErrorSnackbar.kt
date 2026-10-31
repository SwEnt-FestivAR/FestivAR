package com.android.festivar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.festivar.R

internal object ErrorSnackbarTestTags {
  const val ERROR_MESSAGE = "loginError"
}

@Composable
fun ErrorSnackbar(onDismiss: () -> Unit, message: String) {
  Snackbar(
      modifier = Modifier.testTag(ErrorSnackbarTestTags.ERROR_MESSAGE),
      action = {
        Box(contentAlignment = Alignment.Center) {
          TextButton(onClick = onDismiss) {
            Text(
                text = stringResource(R.string.signup_snackbar_ok),
                color = MaterialTheme.colorScheme.inversePrimary,
            )
          }
        }
      },
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
          painter = painterResource(R.drawable.ic_pennant),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.inverseOnSurface,
      )
      Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}
