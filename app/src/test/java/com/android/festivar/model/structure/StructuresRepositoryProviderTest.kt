// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.structure

import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Test

class StructuresRepositoryProviderTest {
  // The provider is process-wide: undo the assignment so other tests do not inherit it.
  @After fun tearDown() = StructuresRepositoryProvider.reset()

  // No FirebaseApp exists in a plain unit test: if setting or reading the repository built the
  // Firestore one, these would throw.
  @Test
  fun repository_canBeSwappedWithoutTouchingFirestore() {
    val local = StructuresRepositoryLocal()

    StructuresRepositoryProvider.repository = local

    assertSame(local, StructuresRepositoryProvider.repository)
  }
}
