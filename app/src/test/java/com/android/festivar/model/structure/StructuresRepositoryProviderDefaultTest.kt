// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.structure

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
 * network, Firestore only connects on the first read or write. [StructuresRepositoryProviderTest]
 * checks the other half, that assigning a repository never builds this one.
 */
@RunWith(RobolectricTestRunner::class)
class StructuresRepositoryProviderDefaultTest {
  @Before
  fun initFirebase() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(context).isEmpty()) FirebaseApp.initializeApp(context)
  }

  @After fun tearDown() = StructuresRepositoryProvider.reset()

  @Test
  fun repository_isTheFirestoreOneByDefault() {
    assertTrue(StructuresRepositoryProvider.repository is StructureRepositoryFirebase)
  }

  // The Firestore repository is built once: the whole app shares the same instance.
  @Test
  fun repository_isBuiltOnce() {
    assertSame(StructuresRepositoryProvider.repository, StructuresRepositoryProvider.repository)
  }

  @Test
  fun reset_goesBackToTheFirestoreRepository() {
    StructuresRepositoryProvider.repository = StructuresRepositoryLocal()

    StructuresRepositoryProvider.reset()

    assertTrue(StructuresRepositoryProvider.repository is StructureRepositoryFirebase)
  }
}
