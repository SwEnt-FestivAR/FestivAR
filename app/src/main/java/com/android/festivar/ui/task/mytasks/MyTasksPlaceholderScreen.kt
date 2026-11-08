// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.task.mytasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.ui.theme.AppTheme

object MyTasksPlaceholderTestTags {
  const val SCREEN = "myTasksPlaceholder"
}

/** Stands in for the My tasks screen of #397 so the tab has a destination; that PR replaces it. */
@Composable
fun MyTasksPlaceholderScreen() {
  Column(
      modifier = Modifier.fillMaxSize().padding(20.dp).testTag(MyTasksPlaceholderTestTags.SCREEN)
  ) {
    Text(
        text = stringResource(R.string.nav_my_tasks),
        style = MaterialTheme.typography.headlineMedium,
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun MyTasksPlaceholderScreenPreview() {
  AppTheme { MyTasksPlaceholderScreen() }
}
