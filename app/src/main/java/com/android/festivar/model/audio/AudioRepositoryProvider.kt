// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.audio

/**
 * Provides a single instance of the [AudioRepository] in the app. `repository` is mutable for
 * testing purposes.
 */
object AudioRepositoryProvider {
  private val firebaseRepository: AudioRepository by lazy { AudioStorageFirebase() }

  // @Volatile: the repository can be set on one thread (for example a test) and read on another.
  @Volatile private var override: AudioRepository? = null

  /**
   * The repository in use. Firebase Storage is only touched if nothing was set: assigning a
   * repository (for example an in-memory one in a test) never builds the Firebase one.
   */
  var repository: AudioRepository
    get() = override ?: firebaseRepository
    set(value) {
      override = value
    }

  /** Goes back to the Firebase Storage repository. Tests call it to undo an assignment. */
  fun reset() {
    override = null
  }
}
