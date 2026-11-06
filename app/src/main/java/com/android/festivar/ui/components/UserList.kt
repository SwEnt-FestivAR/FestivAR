// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.user.User

/** Most tags a user can have in a [UserList], so that they leave room for its name. */
const val MAX_TAGS_PER_USER = 3

object UserListTestTags {
  const val LIST = "userList"
  const val TITLE = "userListTitle"
  const val SUBTITLE = "userListSubtitle"

  /**
   * TestTag of the header of the category [category] as the component creates more of them when it
   * is used.
   */
  fun header(category: String) = "userListHeader_$category"

  /**
   * TestTag of the row of the user with the given [uid] as the component creates more of them when
   * it is used. Making one for each [User] in a row.
   */
  fun row(uid: String) = "userListRow_$uid"
}

/**
 * Represents the tag that is assigned to a [User]. That can represent different things depending on
 * the screen that is considered: the [Task] is done; the [User] role; the [User] permissions. It is
 * free to implement what is needed.
 *
 * @property label The text of the tag.
 * @property containerColor The background of the tag. [Color.Unspecified] uses the ink of the
 *   design.
 * @property contentColor The color of the label. [Color.Unspecified] uses the ink of the design.
 */
data class UserTag(
    val label: String,
    val containerColor: Color = Color.Unspecified,
    val contentColor: Color = Color.Unspecified,
)

/**
 * A group of [users] shown under one header, or without header if [category] is `null`.
 *
 * @property category The category in which the users are.
 * @property users The users within the category.
 */
internal data class UserCategory(
    val category: String?,
    val users: List<User>,
)

/**
 * Splits [users] into the categories shown by [UserList].
 *
 * The categories keep the order in which they first appear in [users], and the users keep their
 * order inside each category. The users without category go in a last category named
 * [uncategorizedLabel], merged with a category of that name if there is one, that category appears
 * only if there is at least one [User] that has a category.
 *
 * @param users The users to split, in the order they are shown.
 * @param categoryOf Gives the category of a user, or `null` if it has none. If [categoryOf] itself
 *   is `null`, all the users go in a single category without header.
 * @param uncategorizedLabel The name of the last category, holding the users without category.
 * @return The categories in the order they are shown, none of them empty, unless [users] is empty
 *   and [categoryOf] is `null`, which gives one empty category without header.
 */
internal fun categorize(
    users: List<User>,
    categoryOf: ((User) -> String?)?,
    uncategorizedLabel: String,
): List<UserCategory> {
  // If there is no category announced, then all the users are just in the same one.
  if (categoryOf == null) return listOf(UserCategory(null, users))

  // Separates the users into their categories and keeps in a single category map the ones
  // that don't have a category initially.
  val (uncategorized, categorized) =
      users
          .groupBy { categoryOf(it) ?: uncategorizedLabel }
          .entries
          .partition { it.key == uncategorizedLabel }

  return (categorized + uncategorized).map { (category, members) ->
    UserCategory(category, members)
  }
}

/**
 * A scrollable list of users, optionally split into categories, as in the "Members" sheet of the
 * design. Use [UserListBottomSheet] to show it in a sheet that can be dragged and closed.
 *
 * @param users The users to show, in this order (within their category if [categoryOf] is given).
 * @param modifier The [Modifier] applied to the list.
 * @param title The title above the list, e.g. "Members". No title if `null`.
 * @param subtitle The line below the title, e.g. "12 members · Fête de l'Asso 2026". None if
 *   `null`.
 * @param categoryOf The category of each user, e.g. "Organizer" or "Volunteers". The users are
 *   grouped under one header per category ("ORGANIZER, 1"), in the order categories first appear in
 *   [users]. A user for whom it returns `null` goes in the last category, [uncategorizedLabel]. No
 *   categories at all if `null`.
 * @param uncategorizedLabel The name of the last category, holding the users without category.
 * @param supportingText The line below the name of a user, e.g. "Volunteer · 3 tasks". Only the
 *   name and surname are shown if it returns `null`, which is the default.
 * @param tags The tags shown at the end of the row of a user, each with its own colors. None by
 *   default. At most [MAX_TAGS_PER_USER] per user, or the list throws an
 *   [IllegalArgumentException].
 * @param onUserClick Called when a user row is clicked. Rows are not clickable if `null`.
 * @param listState The scroll state of the list, to read or set its scroll position.
 */
@Composable
fun UserList(
    users: List<User>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    title: String? = null,
    subtitle: String? = null,
    categoryOf: ((User) -> String?)? = null,
    uncategorizedLabel: String = stringResource(R.string.user_list_uncategorized),
    supportingText: (User) -> String? = { null },
    tags: (User) -> List<UserTag> = { emptyList() },
    onUserClick: ((User) -> Unit)? = null,
) {
  // Transforms the given user list into the necessary categories for the composable?
  val categories = categorize(users, categoryOf, uncategorizedLabel)

  LazyColumn(
      modifier = modifier.fillMaxWidth().testTag(UserListTestTags.LIST),
      state = listState,
      contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
  ) {
    if (title != null || subtitle != null) {
      item(key = "title") { UserListTitle(title = title, subtitle = subtitle) }
    }

    categories.forEach { category ->
      if (category.category != null) {
        item(key = "header_${category.category}") { UserListHeader(category) }
      }

      items(category.users, key = { "user_${it.uid}" }) { user ->
        UserRow(
            user = user,
            supportingText = supportingText(user),
            tags = tags(user),
            onClick = onUserClick?.let { onClick -> { onClick(user) } },
        )
      }
    }
  }
}

/**
 * [UserList] in a modal bottom sheet with a drag handle: it can be dragged up and down, and is
 * closed by dragging it down, tapping outside or going back.
 *
 * @param onDismissRequest Called when the user closes the sheet.
 * @param sheetState The state of the sheet, to open or close it from the caller.
 * @param users The users to show, in this order (within their category if [categoryOf] is given).
 * @param modifier The [Modifier] applied to the list.
 * @param title The title above the list, e.g. "Members". No title if `null`.
 * @param subtitle The line below the title, e.g. "12 members · Fête de l'Asso 2026". None if
 *   `null`.
 * @param categoryOf The category of each user, e.g. "Organizer" or "Volunteers". The users are
 *   grouped under one header per category ("ORGANIZER, 1"), in the order categories first appear in
 *   [users]. A user for whom it returns `null` goes in the last category, [uncategorizedLabel]. No
 *   categories at all if `null`.
 * @param uncategorizedLabel The name of the last category, holding the users without category.
 * @param supportingText The line below the name of a user, e.g. "Volunteer · 3 tasks". Only the
 *   name and surname are shown if it returns `null`, which is the default.
 * @param tags The tags shown at the end of the row of a user, each with its own colors. None by
 *   default. At most [MAX_TAGS_PER_USER] per user, or the list throws an
 *   [IllegalArgumentException].
 * @param onUserClick Called when a user row is clicked. Rows are not clickable if `null`.
 * @param listState The scroll state of the list, to read or set its scroll position.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListBottomSheet(
    users: List<User>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    listState: LazyListState = rememberLazyListState(),
    title: String? = null,
    subtitle: String? = null,
    categoryOf: ((User) -> String?)? = null,
    uncategorizedLabel: String = stringResource(R.string.user_list_uncategorized),
    supportingText: (User) -> String? = { null },
    tags: (User) -> List<UserTag> = { emptyList() },
    onUserClick: ((User) -> Unit)? = null,
) {

  ModalBottomSheet(
      onDismissRequest = onDismissRequest,
      modifier = modifier,
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surface,
  ) {
    UserList(
        users = users,
        listState = listState,
        title = title,
        subtitle = subtitle,
        categoryOf = categoryOf,
        uncategorizedLabel = uncategorizedLabel,
        supportingText = supportingText,
        tags = tags,
        onUserClick = onUserClick,
    )
  }
}

/** Title of the list with the line below it, each shown only if not `null`. */
@Composable
private fun UserListTitle(title: String?, subtitle: String?) {
  Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)) {
    if (title != null) {
      Text(
          text = title,
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.testTag(UserListTestTags.TITLE),
      )
    }

    if (subtitle != null) {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag(UserListTestTags.SUBTITLE),
      )
    }
  }
}

/** Header of a [category], e.g. "ORGANIZER, 1". */
@Composable
private fun UserListHeader(category: UserCategory) {

  Text(
      text = "${category.category.orEmpty().uppercase()}, ${category.users.size}",
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier =
          Modifier.fillMaxWidth()
              .padding(top = 24.dp, bottom = 4.dp)
              .testTag(UserListTestTags.header(category.category.orEmpty())),
  )
}

/**
 * Row of a [user]: its avatar, its name with the [supportingText] below, its [tags] at the end, and
 * a divider under it. Clickable only if [onClick] is not `null`.
 *
 * @throws IllegalArgumentException If a [User] has more than [MAX_TAGS_PER_USER] tags.
 */
@Composable
private fun UserRow(
    user: User,
    supportingText: String?,
    tags: List<UserTag>,
    onClick: (() -> Unit)?,
) {

  require(tags.size <= MAX_TAGS_PER_USER) {
    "A user can have at most $MAX_TAGS_PER_USER tags, but ${user.uid} has ${tags.size}."
  }

  Column(
      modifier =
          Modifier.fillMaxWidth()
              .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
              .testTag(UserListTestTags.row(user.uid))
  ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
    ) {
      Avatar(user = user, modifier = Modifier.size(48.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
            text = "${user.name} ${user.surname}".trim(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (supportingText != null) {
          Text(
              text = supportingText,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
          )
        }
      }

      if (tags.isNotEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { tags.forEach { UserTagPill(it) } }
      }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
  }
}

/** Pill of a [tag], in its colors, or in the ink of the design for the unspecified ones. */
@Composable
private fun UserTagPill(tag: UserTag) {
  val container = tag.containerColor.takeOrElse { MaterialTheme.colorScheme.secondaryContainer }
  val content = tag.contentColor.takeOrElse { MaterialTheme.colorScheme.onSecondaryContainer }

  Box(
      contentAlignment = Alignment.Center,
      modifier =
          Modifier.height(28.dp).background(container, CircleShape).padding(horizontal = 12.dp),
  ) {
    Text(
        text = tag.label,
        style = MaterialTheme.typography.labelLarge,
        color = content,
        maxLines = 1,
    )
  }
}
