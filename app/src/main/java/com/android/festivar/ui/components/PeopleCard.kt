// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Fable 5.2 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.user.User

object PeopleCardTestTags {
  const val COUNT = "peopleCardCount"
  const val MORE = "peopleCardMore"
}

/**
 * A card for a group of [people]: their avatars, a [title] ("On this task", "Members"), how many
 * they are, and a chevron. Tapping it calls [onClick], which usually opens their list.
 *
 * @param subtitle The line below the [title], by default the number of [people] ("3 people").
 */
@Composable
fun PeopleCard(
    title: String,
    people: List<User>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = peopleCount(people.size),
) {
  PeopleRow(
      title = title,
      people = people,
      onClick = onClick,
      modifier =
          modifier.background(
              MaterialTheme.colorScheme.surfaceContainerLow,
              MaterialTheme.shapes.medium,
          ),
      subtitle = subtitle,
  )
}

/**
 * The content of a [PeopleCard] without its background, for a row that sits in a card shared with
 * other rows. It takes the same parameters as [PeopleCard].
 */
@Composable
fun PeopleRow(
    title: String,
    people: List<User>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = peopleCount(people.size),
) {
  Row(
      modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    PeopleStack(people)
    Column(Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(
          subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag(PeopleCardTestTags.COUNT),
      )
    }
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/** The first two [people] as avatars overlapping by 12dp, then "+N" for the others. */
@Composable
fun PeopleStack(people: List<User>, modifier: Modifier = Modifier) {
  val shown = people.take(2)
  val more = people.size - shown.size
  val slots = shown.size + if (more > 0) 1 else 0
  Box(modifier.width((32 + 20 * (slots - 1)).dp).height(32.dp)) {
    shown.forEachIndexed { index, user -> Avatar(user, Modifier.offset(x = (20 * index).dp)) }
    if (more > 0) {
      Box(
          contentAlignment = Alignment.Center,
          modifier =
              Modifier.offset(x = 40.dp)
                  .size(32.dp)
                  .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
      ) {
        Text(
            stringResource(R.string.people_more, more),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.testTag(PeopleCardTestTags.MORE),
        )
      }
    }
  }
}

/** "1 person", "3 people". */
@Composable
fun peopleCount(count: Int): String = pluralStringResource(R.plurals.people_count, count, count)
