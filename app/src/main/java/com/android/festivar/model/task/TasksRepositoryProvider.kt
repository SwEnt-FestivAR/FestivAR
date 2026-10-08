// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.task

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

/**
 * Provides a single instance of the [TasksRepository] in the app. `repository` is mutable for
 * testing purposes.
 */
object TasksRepositoryProvider {
  private val _repository: TasksRepository by lazy { TaskRepositoryFirebase(Firebase.firestore) }

  var repository: TasksRepository = _repository
}
