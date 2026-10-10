// Written with the help of an AI coding assistant and reviewed line by line by the author.
package com.android.festivar.model.task

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
 * network, Firestore only connects on the first read or write. [TasksRepositoryProviderTest] checks
 * the other half, that assigning a repository never builds this one, and it runs without
 * Robolectric so that a `FirebaseApp` is guaranteed to be absent there.
 */
@RunWith(RobolectricTestRunner::class)
class TasksRepositoryProviderDefaultTest {
  @Before
  fun initFirebase() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(context).isEmpty()) FirebaseApp.initializeApp(context)
  }

  @After fun tearDown() = TasksRepositoryProvider.reset()

  @Test
  fun repository_isTheFirestoreOneByDefault() {
    val repository = TasksRepositoryProvider.repository

    assertTrue(repository is TaskRepositoryFirebase)
  }

  // The Firestore repository is built once: the whole app shares the same instance.
  @Test
  fun repository_isBuiltOnce() {
    assertSame(TasksRepositoryProvider.repository, TasksRepositoryProvider.repository)
  }

  @Test
  fun reset_goesBackToTheFirestoreRepository() {
    TasksRepositoryProvider.repository = TasksRepositoryLocal()

    TasksRepositoryProvider.reset()

    assertTrue(TasksRepositoryProvider.repository is TaskRepositoryFirebase)
  }
}
