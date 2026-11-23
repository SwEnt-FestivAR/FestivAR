// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import com.android.festivar.model.authentication.FakeAuthRepository
import com.android.festivar.model.datarepository.DataRepositoryLocal
import com.android.festivar.model.event.Event
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.role.Role
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepositoryLocal
import java.time.ZoneId
import java.time.ZonedDateTime

/** The people and the event shared by the tests of the members of an event. */
internal object MembersFixture {
  val organizer = User("owner", name = "Olivia", surname = "Roy")
  val lea = User("u1", name = "Léa", surname = "Martin")
  val sara = User("u2", name = "Sara", surname = "Keller")
  val tom = User("u3", name = "Tom", surname = "Weber")

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

  /** An event owned by [organizer], whose other members have the [Role] paired with them. */
  fun eventWithRoles(vararg members: Pair<User, Role>): Event =
      eventOf(organizer, *members.map { it.first }.toTypedArray())
          .copy(roles = members.associate { (user, role) -> user.uid to role } + ownerRole)

  /**
   * A [DataRepositoryLocal] where [eventsRepository] stores [event], and whose users are the
   * members of [event] as having joined it, so that they can be kicked out of it.
   */
  fun dataRepositoryWith(
      event: Event,
      eventsRepository: EventsRepository = EventsRepositoryLocal(listOf(event)),
  ) =
      DataRepositoryLocal(
          authRepository = FakeAuthRepository(),
          eventsRepository = eventsRepository,
          usersRepository =
              UsersRepositoryLocal(
                  event.members.map { it.copy(joinedEvents = listOf(event.eventId)) }
              ),
      )

  /** A repository storing [event] that fails to save any change to it. */
  fun failingToSave(event: Event): EventsRepository =
      object : EventsRepository by EventsRepositoryLocal(listOf(event)) {
        override suspend fun editEvent(eventId: String, newValue: Event) =
            throw IllegalStateException("save failed")
      }

  private val ownerRole = organizer.uid to Role.OWNER
}
