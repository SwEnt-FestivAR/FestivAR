// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.event

import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Test

class EventsRepositoryProviderTest {
  // The provider is process-wide: undo the assignment so other tests do not inherit it.
  @After fun tearDown() = EventsRepositoryProvider.reset()

  // No FirebaseApp exists in a plain unit test: if setting or reading the repository built the
  // Firestore one, these would throw.
  @Test
  fun repository_canBeSwappedWithoutTouchingFirestore() {
    val local = EventsRepositoryLocal()

    EventsRepositoryProvider.repository = local

    assertSame(local, EventsRepositoryProvider.repository)
  }

  // A later assignment replaces the earlier one, e.g. when a test sets its own repository.
  @Test
  fun repository_canBeSwappedAgain() {
    val first = EventsRepositoryLocal()
    val second = EventsRepositoryLocal()

    EventsRepositoryProvider.repository = first
    EventsRepositoryProvider.repository = second

    assertSame(second, EventsRepositoryProvider.repository)
  }
}
