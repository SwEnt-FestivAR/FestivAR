// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event.details

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.festivar.R
import com.android.festivar.model.event.role.Role
import com.android.festivar.ui.components.PillStyle
import com.android.festivar.ui.components.StatusPill
import com.android.festivar.ui.components.UserTag

/** The name of the [Role], e.g. "Team lead", shown in its badge. */
@StringRes
internal fun Role.labelRes(): Int =
    when (this) {
      Role.OWNER -> R.string.event_details_role_owner
      Role.ADMIN -> R.string.event_details_role_admin
      Role.STAFF_MANAGER -> R.string.event_details_role_staff_manager
      Role.TEAM_LEAD -> R.string.event_details_role_team_lead
      Role.STAFF -> R.string.event_details_role_staff
      Role.VISITOR -> R.string.event_details_role_visitor
    }

/** The name of the category of the members with this [Role], e.g. "Team leads". */
@StringRes
internal fun Role.categoryRes(): Int =
    when (this) {
      Role.OWNER -> R.string.event_details_category_owner
      Role.ADMIN -> R.string.event_details_category_admin
      Role.STAFF_MANAGER -> R.string.event_details_category_staff_manager
      Role.TEAM_LEAD -> R.string.event_details_category_team_lead
      Role.STAFF -> R.string.event_details_category_staff
      Role.VISITOR -> R.string.event_details_category_visitor
    }

/** The style of the badge of this [Role], darker for the higher roles as in the design. */
internal fun Role.pillStyle(): PillStyle =
    when (this) {
      Role.OWNER -> PillStyle.INK
      Role.ADMIN,
      Role.STAFF_MANAGER -> PillStyle.LIME
      Role.TEAM_LEAD -> PillStyle.MUTED
      Role.STAFF,
      Role.VISITOR -> PillStyle.OUTLINED
    }

/** The badge of [role], as a [StatusPill]. */
@Composable
internal fun RoleBadge(role: Role) {
  StatusPill(label = stringResource(role.labelRes()), style = role.pillStyle())
}

/** The badge of [role] in a row of the members list, with the colors of its [RoleBadge]. */
@Composable
internal fun roleTag(role: Role): UserTag {
  val colors = MaterialTheme.colorScheme
  val (container, content) =
      when (role.pillStyle()) {
        PillStyle.INK -> colors.secondaryContainer to colors.onSecondaryContainer
        PillStyle.LIME -> colors.primaryContainer to colors.onPrimaryContainer
        PillStyle.MUTED -> colors.surfaceContainerHigh to colors.onSurface
        else -> colors.surfaceContainerLowest to colors.onSurface
      }
  return UserTag(stringResource(role.labelRes()), container, content)
}
