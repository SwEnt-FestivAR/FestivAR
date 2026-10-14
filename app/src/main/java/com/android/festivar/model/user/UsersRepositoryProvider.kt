package com.android.festivar.model.user

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a single instance of the [UsersRepository] in the app. `repository` is mutable for
 * testing purposes.
 */
object UsersRepositoryProvider {
  private val firebaseRepository: UsersRepository by lazy {
    UserRepositoryFirebase(Firebase.firestore)
  }

  // @Volatile: the repository can be set on one thread (for example a test) and read on another
  // (the main thread). It makes a write visible to every thread right away, so no thread keeps
  // using a stale repository.
  @Volatile private var override: UsersRepository? = null

  /**
   * The repository in use. Firestore is only touched if nothing was set: assigning a repository
   * (for example an in-memory one in a test) never builds the Firebase one.
   */
  var repository: UsersRepository
    get() = override ?: firebaseRepository
    set(value) {
      override = value
    }

  /** Goes back to the Firestore-backed repository. Tests call it to undo an assignment. */
  fun reset() {
    override = null
  }
}
