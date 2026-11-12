// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.event

import com.android.festivar.model.user.User
import com.android.festivar.ui.event.details.membersOrganizerFirst
import org.junit.Assert.assertEquals
import org.junit.Test

class EventMembersTest {

  private val organizer = MembersFixture.organizer
  private val lea = MembersFixture.lea
  private val sara = MembersFixture.sara

  private fun eventOf(vararg members: User) = MembersFixture.eventOf(*members)

  /** Breaks if the organizer is not pinned at the top when they joined after the volunteers. */
  @Test
  fun theOrganizerComesFirstEvenWhenTheyAreListedLast() {
    assertEquals(
        listOf(organizer, lea, sara),
        eventOf(lea, sara, organizer).membersOrganizerFirst(),
    )
  }

  /** Breaks if the volunteers are reordered instead of keeping the order they joined in. */
  @Test
  fun theVolunteersKeepTheirOrder() {
    assertEquals(
        listOf(organizer, sara, lea),
        eventOf(sara, organizer, lea).membersOrganizerFirst(),
    )
  }

  /** Breaks if an event with only its organizer loses them or lists them twice. */
  @Test
  fun anEventWithOnlyItsOrganizerListsThemOnce() {
    assertEquals(listOf(organizer), eventOf(organizer).membersOrganizerFirst())
  }
}
