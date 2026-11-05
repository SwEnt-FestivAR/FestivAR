// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.datarepository

import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.event.EventsRepositoryLocal
import com.android.festivar.model.event.EventsRepositoryProvider
import com.android.festivar.model.task.TasksRepositoryLocal
import com.android.festivar.model.task.TasksRepositoryProvider
import com.android.festivar.model.user.UsersRepositoryLocal
import com.android.festivar.model.user.UsersRepositoryProvider
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Tests how [DataRepositoryFirebase] picks its repositories. Its operations come from
 * [DataRepository] and are tested through [DataRepositoryLocalTest].
 *
 * [DataRepositoryFirebaseDefaultTest] checks the defaults when nothing was assigned to the
 * providers. This test runs without Robolectric so that a `FirebaseApp` is guaranteed to be absent.
 */
class DataRepositoryFirebaseTest {
  // The providers are process-wide: undo the assignments so other tests do not inherit them.
  @After
  fun tearDown() {
    TasksRepositoryProvider.reset()
    EventsRepositoryProvider.reset()
    UsersRepositoryProvider.reset()
  }

  // No FirebaseApp exists in a plain unit test: if building the data repository built a Firestore
  // repository, this would throw.
  @Test
  fun defaults_areTheRepositoriesOfTheProvidersWithoutTouchingFirestore() {
    val tasks = TasksRepositoryLocal()
    val events = EventsRepositoryLocal()
    val users = UsersRepositoryLocal()
    TasksRepositoryProvider.repository = tasks
    EventsRepositoryProvider.repository = events
    UsersRepositoryProvider.repository = users
    // The default AuthRepositoryFirebase needs Firebase.auth, which is unavailable here.
    val auth = mockk<AuthRepository>()

    val repository = DataRepositoryFirebase(authRepository = auth)

    assertSame(auth, repository.authRepository)
    assertSame(tasks, repository.tasksRepository)
    assertSame(events, repository.eventsRepository)
    assertSame(users, repository.usersRepository)
  }
}
