// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event.role

/**
 * An enum class that represents all the permissions that can be attributed to a [Role] within an
 * [com.android.festivar.model.event.Event] in the FestivAR app.
 *
 * A permission containing `_OWN_` only applies to the objects the member owns, while the matching
 * permission without `_OWN_` applies to every object of the event.
 *
 * There is no permission such as `CAN_SEE_EVENT` because the permissions and roles are meant to be
 * for members of the event, so they, by default, have access to the event, and should be able to
 * see its details, otherwise, there is no point to be member of it.
 */
enum class Permission {
  /** See members list. */
  CAN_SEE_MEMBERS,
  /** See the tasks of the event. */
  CAN_SEE_TASK,
  /** Take the tasks of the event. */
  CAN_TAKE_TASK,
  /** Mark the taken task of the event as done. */
  CAN_MARK_TAKEN_TASK_DONE,
  /** Edit the tasks the member owns. */
  CAN_EDIT_OWN_TASK,
  /** Delete the tasks the member owns. */
  CAN_DELETE_OWN_TASK,
  /** Edit any task of the event, whoever owns it. */
  CAN_EDIT_TASK,
  /** Delete any task of the event, whoever owns it. */
  CAN_DELETE_TASK,
  /** Create a new task in the event. */
  CAN_CREATE_TASK,
  /** Assign a task to another member whose [Role] the member's [Role] dominates. */
  CAN_ASSIGN_TASK,
  /** See the announcements of the event. */
  CAN_SEE_ANNOUNCEMENT,
  /** Edit the announcements the member owns. */
  CAN_EDIT_OWN_ANNOUNCEMENT,
  /** Delete the announcements the member owns. */
  CAN_DELETE_OWN_ANNOUNCEMENT,
  /**
   * Edit any announcement of the event, whoever owns it. Pinning or unpinning an announcement
   * counts as editing it, with this permission or [CAN_EDIT_OWN_ANNOUNCEMENT].
   */
  CAN_EDIT_ANNOUNCEMENT,
  /** Delete any announcement of the event, whoever owns it. */
  CAN_DELETE_ANNOUNCEMENT,
  /** Publish a new announcement in the event. */
  CAN_CREATE_ANNOUNCEMENT,
  /**
   * Edit the details of the event (title, description, dates, location...). Closing the event
   * counts as editing it.
   */
  CAN_EDIT_EVENT,
  /** Delete the event. */
  CAN_DELETE_EVENT,
  /** Change the [Role] of a member, only between roles the member's [Role] dominates. */
  CAN_MANAGE_ROLES,
  /** Remove from the event a member whose [Role] the member's [Role] dominates. */
  CAN_KICK_MEMBER,
  /** Give the ownership of the event to another member. */
  CAN_TRANSFER_OWNERSHIP,
}
