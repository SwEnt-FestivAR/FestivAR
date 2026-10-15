// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.user

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The default of the provider, which needs a `FirebaseApp`: it is initialised here from the app's
 * own `google-services.json` values, under Robolectric for the `Context`. Nothing talks to the
 * network, Firestore only connects on the first read or write. [UsersRepositoryProviderTest] checks
 * the other half, that assigning a repository never builds this one, and it runs without
 * Robolectric so that a `FirebaseApp` is guaranteed to be absent there.
 */
@RunWith(RobolectricTestRunner::class)
class UsersRepositoryProviderDefaultTest {
  @Before
  fun initFirebase() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(context).isEmpty()) FirebaseApp.initializeApp(context)
  }

  @After fun tearDown() = UsersRepositoryProvider.reset()

  @Test
  fun repository_isTheFirestoreOneByDefault() {
    val repository = UsersRepositoryProvider.repository

    assertTrue(repository is UserRepositoryFirebase)
  }

  // The Firestore repository is built once: the whole app shares the same instance.
  @Test
  fun repository_isBuiltOnce() {
    assertSame(UsersRepositoryProvider.repository, UsersRepositoryProvider.repository)
  }

  @Test
  fun reset_goesBackToTheFirestoreRepository() {
    UsersRepositoryProvider.repository = UsersRepositoryLocal()

    UsersRepositoryProvider.reset()

    assertTrue(UsersRepositoryProvider.repository is UserRepositoryFirebase)
  }
}
