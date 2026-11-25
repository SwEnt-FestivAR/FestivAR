// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.festivar.R

object RetryButtonTestTags {
  const val BUTTON = "retryButton"
}

/**
 * The button that runs a failed action again, for example under the error of a load that failed: a
 * filled button with the text "Retry". It is shared so that Retry looks the same on every screen.
 *
 * @param onClick Called when the user taps the button.
 * @param modifier Applied to the button.
 */
@Composable
fun RetryButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
  Button(
      onClick = onClick,
      shape = RoundedCornerShape(12.dp),
      modifier = modifier.testTag(RetryButtonTestTags.BUTTON),
  ) {
    Text(stringResource(R.string.action_retry))
  }
}
