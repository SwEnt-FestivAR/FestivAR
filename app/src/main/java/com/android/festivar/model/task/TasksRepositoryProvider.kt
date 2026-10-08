// Written with the help of an AI coding assistant and reviewed line by line by the author.
package com.android.festivar.model.task

/**
 * Holds the [TasksRepository] the whole app shares, so ViewModels never build one themselves.
 *
 * The default is an in-memory [TasksRepositoryLocal]. The Firestore implementation (issue #78)
 * replaces it at app start, and tests overwrite it with a seeded local repository.
 */
object TasksRepositoryProvider {
  var repository: TasksRepository = TasksRepositoryLocal()
}
