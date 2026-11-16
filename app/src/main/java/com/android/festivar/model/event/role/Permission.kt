// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event.role

/**
 * An enum class that represents all the permissions that can be attributed to a [Role] within an
 * [com.android.festivar.model.event.Event] in the FestivAR app.
 *
 * A permission ending in `_OWN_` only applies to the objects the member owns, while the matching
 * permission without `_OWN_` applies to every object of the event.
 */
enum class Permission {
  /** See the tasks of the event. */
  CAN_SEE_TASK,
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
  /** Assign a task to a member whose [Role] the member's [Role] dominates. */
  CAN_ASSIGN_TASK,
  /** See the announcements of the event. */
  CAN_SEE_ANNOUNCEMENT,
  /** Edit the announcements the member owns. */
  CAN_EDIT_OWN_ANNOUNCEMENT,
  /** Delete the announcements the member owns. */
  CAN_DELETE_OWN_ANNOUNCEMENT,
  /** Edit any announcement of the event, whoever owns it. */
  CAN_EDIT_ANNOUNCEMENT,
  /** Delete any announcement of the event, whoever owns it. */
  CAN_DELETE_ANNOUNCEMENT,
  /** Publish a new announcement in the event. */
  CAN_CREATE_ANNOUNCEMENT,
  /** Edit the details of the event (title, description, dates, location...). */
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
