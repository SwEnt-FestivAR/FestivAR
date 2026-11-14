// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import com.android.festivar.model.event.Event
import com.android.festivar.model.user.User
import java.time.ZoneId
import java.time.ZonedDateTime

/** The people and the event shared by the tests of the members of an event. */
internal object MembersFixture {
  val organizer = User("owner", name = "Olivia", surname = "Roy")
  val lea = User("u1", name = "Léa", surname = "Martin")
  val sara = User("u2", name = "Sara", surname = "Keller")

  /** An event owned by [organizer] with exactly these [members], in this order. */
  fun eventOf(vararg members: User): Event {
    val start = ZonedDateTime.of(2026, 10, 16, 10, 0, 0, 0, ZoneId.of("Europe/Zurich"))
    return Event(
        eventId = "3f9a1c2e-7b4d-4e8a-9c15-d2a6b0f84e37",
        ownerId = organizer.uid,
        title = "Fête de l'Asso 2026",
        members = members.toList(),
        startDate = start,
        endDate = start.plusDays(2),
    )
  }
}
