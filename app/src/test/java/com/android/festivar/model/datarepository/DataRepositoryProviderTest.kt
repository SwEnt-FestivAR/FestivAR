package com.android.festivar.model.datarepository

// Co-authored-by: Copilot App

import com.android.festivar.model.authentication.FakeAuthRepository
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Test

class DataRepositoryProviderTest {
  // The provider is process-wide: undo the assignment so other tests do not inherit it.
  @After fun tearDown() = DataRepositoryProvider.reset()

  // No FirebaseApp exists in a plain unit test: if setting or reading the repository built the
  // Firestore one, these would throw.
  @Test
  fun repository_canBeSwappedWithoutTouchingFirestore() {
    val local = DataRepositoryLocal(FakeAuthRepository())

    DataRepositoryProvider.repository = local

    assertSame(local, DataRepositoryProvider.repository)
  }
}
