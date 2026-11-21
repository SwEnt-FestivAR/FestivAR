package com.android.festivar.ui.event.details

// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.android.festivar.R
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.role.Role
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.FestivarConfirmDialog
import com.android.festivar.ui.components.MemberDialog
import com.android.festivar.ui.components.MemberDialogAction
import com.android.festivar.ui.components.OptionPickerDialog
import com.android.festivar.ui.components.PeopleRow
import com.android.festivar.ui.components.UserListBottomSheet

/**
 * The "Members" card of the [event], which opens the list of its members grouped by role, from the
 * owner to the visitors, each with the badge of their role and their number of tasks.
 *
 * Tapping a member opens their [MemberActions].
 *
 * @param startOpen whether the list is open at first, which previews use to show it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MembersEntry(
    event: Event,
    state: EventDetailsUIState,
    actions: EventDetailsActions,
    startOpen: Boolean = false,
) {
  var showMembers by rememberSaveable { mutableStateOf(startOpen) }
  // The member whose dialog is open.
  var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

  val members = event.membersByRole()
  val title = stringResource(R.string.event_details_members)
  val count = pluralStringResource(R.plurals.join_event_members, members.size, members.size)

  PeopleRow(
      title = title,
      people = members,
      subtitle = count,
      onClick = { showMembers = true },
      modifier = Modifier.testTag(EventDetailsScreenTestTags.MEMBERS_CARD),
  )

  if (showMembers) {
    val categories = Role.entries.associateWith { stringResource(it.categoryRes()) }
    val tags = Role.entries.associateWith { roleTag(it) }
    val resources = LocalContext.current.resources
    UserListBottomSheet(
        users = members,
        onDismissRequest = { showMembers = false },
        title = title,
        subtitle = stringResource(R.string.event_details_members_subtitle, count, event.title),
        categoryOf = { user -> categories.getValue(event.roleOf(user.uid)) },
        supportingText = { user ->
          val tasks = event.tasksOf(user.uid)
          resources.getQuantityString(R.plurals.event_details_member_tasks, tasks, tasks)
        },
        tags = { user -> listOf(tags.getValue(event.roleOf(user.uid))) },
        onUserClick = { user -> selectedId = user.uid },
        modifier = Modifier.testTag(EventDetailsScreenTestTags.MEMBERS_SHEET),
    )
  }

  // A member who left meanwhile (e.g. was just kicked) closes their dialogs.
  val member = event.members.find { it.uid == selectedId } ?: return
  MemberActions(
      member = member,
      event = event,
      state = state,
      actions = actions,
      onClose = { selectedId = null },
  )
}

/**
 * The dialogs of one [member] of the [event]: a [MemberDialog] offering the actions the current
 * user can do on them (see [EventDetailsPermissions.onMember]), which are changing their role,
 * through an [OptionPickerDialog] of the roles they can give, or kicking them. Both ask for a
 * confirmation before calling [actions].
 *
 * @param onClose Called to close every dialog of the member, after a confirmed action or when the
 *   member dialog is dismissed.
 */
@Composable
private fun MemberActions(
    member: User,
    event: Event,
    state: EventDetailsUIState,
    actions: EventDetailsActions,
    onClose: () -> Unit,
) {
  // What is being done to the member, from the member dialog to a confirmation.
  var pickingRole by rememberSaveable(member.uid) { mutableStateOf(false) }
  var pendingRole by rememberSaveable(member.uid) { mutableStateOf<Role?>(null) }
  var confirmingKick by rememberSaveable(member.uid) { mutableStateOf(false) }

  val role = event.roleOf(member.uid)
  val allowed = state.permissions.onMember(member.uid)
  val name = "${member.name} ${member.surname}".trim().ifEmpty { member.uid }

  when {
    pendingRole != null -> {
      val newRole = pendingRole!!
      FestivarConfirmDialog(
          title =
              stringResource(
                  R.string.event_details_change_role_confirm_title,
                  member.name.ifEmpty { name },
                  stringResource(newRole.labelRes()),
              ),
          message =
              stringResource(R.string.event_details_change_role_confirm_message, name, event.title),
          confirmText = stringResource(R.string.event_details_change_role),
          dismissText = stringResource(R.string.action_cancel),
          onConfirm = {
            actions.onChangeRole(member.uid, newRole)
            onClose()
          },
          onDismiss = { pendingRole = null },
          confirmModifier = Modifier.testTag(EventDetailsScreenTestTags.CHANGE_ROLE_CONFIRM),
          destructive = false,
      )
    }
    confirmingKick ->
        FestivarConfirmDialog(
            title = stringResource(R.string.event_details_kick_confirm_title, name),
            message = stringResource(R.string.event_details_kick_confirm_message, event.title),
            confirmText = stringResource(R.string.event_details_kick),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
              actions.onKick(member.uid)
              onClose()
            },
            onDismiss = { confirmingKick = false },
            confirmModifier = Modifier.testTag(EventDetailsScreenTestTags.KICK_CONFIRM),
        )
    pickingRole ->
        OptionPickerDialog(
            title =
                stringResource(
                    R.string.event_details_change_role_title,
                    member.name.ifEmpty { name },
                ),
            options = allowed.assignableRoles,
            onPick = { pendingRole = it },
            onDismiss = { pickingRole = false },
            current = role,
            currentLabel = stringResource(R.string.event_details_current_role),
            enabled = !state.isUpdating,
        ) {
          RoleBadge(it)
        }
    else -> {
      val changeRole = stringResource(R.string.event_details_change_role)
      val kick = stringResource(R.string.event_details_kick)
      val tasks = event.tasksOf(member.uid)
      MemberDialog(
          user = member,
          onDismiss = onClose,
          badge = { RoleBadge(role) },
          supportingText = pluralStringResource(R.plurals.event_details_member_tasks, tasks, tasks),
          actions =
              buildList {
                if (allowed.canChangeRole) {
                  add(
                      MemberDialogAction(
                          changeRole,
                          { pickingRole = true },
                          enabled = !state.isUpdating,
                      )
                  )
                }
                if (allowed.canKick) {
                  add(
                      MemberDialogAction(
                          kick,
                          { confirmingKick = true },
                          destructive = true,
                          enabled = !state.isUpdating,
                      )
                  )
                }
              },
      )
    }
  }
}
