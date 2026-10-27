// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.user.User
import com.android.festivar.ui.theme.AppTheme

object AvatarTestTags {
  const val PERSON_ICON = "avatarPersonIcon"
}

/**
 * Initials shown in the avatar of a [user]: the first letter of its name and of its surname, e.g.
 * "SK" for Sara Keller, or the first two letters of its name if it has no surname, e.g. "SA" for
 * Sara (same for a surname without a name). `null` if both are blank, so that the avatar shows a
 * person icon instead.
 */
internal fun initials(user: User): String? {
  val name = user.name.trim()
  val surname = user.surname.trim()
  val letters =
      when {
        name.isNotEmpty() && surname.isNotEmpty() -> "${name.first()}${surname.first()}"
        name.isNotEmpty() -> name.take(2)
        surname.isNotEmpty() -> surname.take(2)
        else -> null
      }
  return letters?.uppercase()
}

/**
 * Round avatar of the design, showing the [initials] of the [user], or a person icon if none.
 *
 * @param user The [User] shown by the avatar.
 * @param modifier The [Modifier] applied to the avatar, before its default 32.dp size, so that a
 *   size given by the caller wins.
 * @param onClick Called when the user clicks on the avatar. Not clickable if `null`.
 */
@Composable
fun Avatar(user: User, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
  val initials = initials(user)
  Box(
      contentAlignment = Alignment.Center,
      modifier =
          modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceContainerHighest)
              .border(2.dp, MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape)
              .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
  ) {
    if (initials == null) {
      Icon(
          painter = painterResource(R.drawable.ic_person),
          contentDescription = "User avatar",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp).testTag(AvatarTestTags.PERSON_ICON),
      )
    } else {
      Text(
          text = initials,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun AvatarPreview() {
  AppTheme {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
      Avatar(user = User("u1", name = "Alice", surname = "Keller"))
      Avatar(user = User("u2", name = "Sara"))
      Avatar(user = User("u3"))
    }
  }
}
