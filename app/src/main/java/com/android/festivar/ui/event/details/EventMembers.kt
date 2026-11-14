// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event.details

import com.android.festivar.model.event.Event
import com.android.festivar.model.user.User

/** Whether [user] is the organizer of the event, i.e. its owner. */
internal fun Event.isOrganizer(user: User): Boolean = user.uid == ownerId

/**
 * The members of the event as one list: the organizer first, then the other members in the order of
 * [Event.members].
 */
internal fun Event.membersOrganizerFirst(): List<User> {
  val (organizer, volunteers) = members.partition(::isOrganizer)
  return organizer + volunteers
}
