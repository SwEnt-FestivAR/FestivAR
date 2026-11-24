// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.datarepository

import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.authentication.AuthRepositoryFirebase
import com.android.festivar.model.chat.ChatRepository
import com.android.festivar.model.chat.ChatRepositoryProvider
import com.android.festivar.model.event.EventsRepository
import com.android.festivar.model.event.EventsRepositoryProvider
import com.android.festivar.model.images.ImageStorage
import com.android.festivar.model.images.ImageStorageFirebase
import com.android.festivar.model.task.TasksRepository
import com.android.festivar.model.task.TasksRepositoryProvider
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryProvider
import com.google.firebase.Firebase
import com.google.firebase.storage.storage

/**
 * A [DataRepository] implementation using Firebase for the application functionality.
 *
 * By default it uses the providers and the default values that the repositories provide.
 *
 * @param authRepository The repository handling the authentication.
 * @param tasksRepository The repository storing the tasks.
 * @param eventsRepository The repository storing the events.
 * @param usersRepository The repository storing the users.
 * @param imageStorage The storage for uploaded images.
 */
class DataRepositoryFirebase(
    override val authRepository: AuthRepository = AuthRepositoryFirebase(),
    override val tasksRepository: TasksRepository = TasksRepositoryProvider.repository,
    override val eventsRepository: EventsRepository = EventsRepositoryProvider.repository,
    override val usersRepository: UsersRepository = UsersRepositoryProvider.repository,
    override val chatRepository: ChatRepository? = ChatRepositoryProvider.repository,
    imageStorage: ImageStorage? = null,
) : DataRepository {
  private val configuredImageStorage = imageStorage

  override val imageStorage: ImageStorage
    get() = configuredImageStorage ?: ImageStorageFirebase(Firebase.storage.reference)
}
