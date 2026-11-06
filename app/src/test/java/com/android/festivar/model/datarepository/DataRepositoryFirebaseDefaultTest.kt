// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.datarepository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.festivar.model.authentication.AuthRepositoryFirebase
import com.android.festivar.model.event.EventRepositoryFirebase
import com.android.festivar.model.task.TaskRepositoryFirebase
import com.android.festivar.model.user.UsersRepositoryFirebase
import com.google.firebase.FirebaseApp
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The defaults of [DataRepositoryFirebase], which need a `FirebaseApp`: it is initialised here from
 * the app's own `google-services.json` values, under Robolectric for the `Context`. Nothing talks
 * to the network, Firebase only connects on the first read or write. [DataRepositoryFirebaseTest]
 * checks the other half, that the defaults follow what is assigned to the providers.
 */
@RunWith(RobolectricTestRunner::class)
class DataRepositoryFirebaseDefaultTest {
  @Before
  fun initFirebase() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(context).isEmpty()) FirebaseApp.initializeApp(context)
  }

  @Test
  fun defaults_areTheFirebaseRepositories() {
    val repository = DataRepositoryFirebase()

    assertTrue(repository.authRepository is AuthRepositoryFirebase)
    assertTrue(repository.tasksRepository is TaskRepositoryFirebase)
    assertTrue(repository.eventsRepository is EventRepositoryFirebase)
    assertTrue(repository.usersRepository is UsersRepositoryFirebase)
  }
}
