// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.task

import org.junit.Assert.assertSame
import org.junit.Test

class TasksRepositoryProviderTest {
  // No FirebaseApp exists in a plain unit test: if setting or reading the repository built the
  // Firestore one, these would throw.
  @Test
  fun repository_canBeSwappedWithoutTouchingFirestore() {
    val local = TasksRepositoryLocal()

    TasksRepositoryProvider.repository = local

    assertSame(local, TasksRepositoryProvider.repository)
  }
}
