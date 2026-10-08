// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.task

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a single instance of the [TasksRepository] in the app. `repository` is mutable for
 * testing purposes.
 */
object TasksRepositoryProvider {
  private val firebaseRepository: TasksRepository by lazy {
    TaskRepositoryFirebase(Firebase.firestore)
  }

  private var override: TasksRepository? = null

  /**
   * The repository in use. Firestore is only touched if nothing was set: assigning a repository
   * (for example an in-memory one in a test) never builds the Firebase one.
   */
  var repository: TasksRepository
    get() = override ?: firebaseRepository
    set(value) {
      override = value
    }
}
