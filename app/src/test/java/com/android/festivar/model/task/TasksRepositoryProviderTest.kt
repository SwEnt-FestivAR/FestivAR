// Written with the help of an AI coding assistant and reviewed line by line by the author.
package com.android.festivar.model.task

import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Test

class TasksRepositoryProviderTest {
  // The provider is process-wide: undo the assignment so other tests do not inherit it.
  @After fun tearDown() = TasksRepositoryProvider.reset()

  // No FirebaseApp exists in a plain unit test: if setting or reading the repository built the
  // Firestore one, these would throw.
  @Test
  fun repository_canBeSwappedWithoutTouchingFirestore() {
    val local = TasksRepositoryLocal()

    TasksRepositoryProvider.repository = local

    assertSame(local, TasksRepositoryProvider.repository)
  }
}
