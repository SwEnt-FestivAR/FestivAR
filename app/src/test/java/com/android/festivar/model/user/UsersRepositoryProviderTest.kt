// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.user

import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Test

class UsersRepositoryProviderTest {
  // The provider is process-wide: undo the assignment so other tests do not inherit it.
  @After fun tearDown() = UsersRepositoryProvider.reset()

  // No FirebaseApp exists in a plain unit test: if setting or reading the repository built the
  // Firestore one, these would throw.
  @Test
  fun repository_canBeSwappedWithoutTouchingFirestore() {
    val local = UsersRepositoryLocal()

    UsersRepositoryProvider.repository = local

    assertSame(local, UsersRepositoryProvider.repository)
  }
}
