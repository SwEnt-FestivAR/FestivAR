package com.android.festivar.model.datarepository

// Co-authored-by: Copilot App

/**
 * Provides a single instance of the [DataRepository] in the app. `repository` is mutable for
 * testing purposes.
 */
object DataRepositoryProvider {
  private val firebaseRepository: DataRepository by lazy { DataRepositoryFirebase() }

  // @Volatile: the repository can be set on one thread (for example a test) and read on another
  // (the main thread). It makes a write visible to every thread right away, so no thread keeps
  // using a stale repository.
  @Volatile private var override: DataRepository? = null

  /**
   * The repository in use. Firestore is only touched if nothing was set: assigning a repository
   * (for example an in-memory one in a test) never builds the Firebase one.
   */
  var repository: DataRepository
    get() = override ?: firebaseRepository
    set(value) {
      override = value
    }

  /** Goes back to the Firestore-backed repository. Tests call it to undo an assignment. */
  fun reset() {
    override = null
  }
}
