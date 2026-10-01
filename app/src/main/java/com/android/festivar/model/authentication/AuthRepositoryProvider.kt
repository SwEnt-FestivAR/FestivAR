// Co-authored-by: Claude (Anthropic), through Claude Code as the coding agent.
package com.android.festivar.model.authentication

/**
 * Hands the production [AuthRepository] to ViewModels through their default parameters. Tests
 * overwrite [repository] with a fake before composing, so no ViewModel ever imports Firebase.
 */
object AuthRepositoryProvider {
  var repository: AuthRepository = AuthRepositoryFirebase()
}
