// Co-authored-by: Claude (Anthropic), through Claude Code as the coding agent.
package com.android.festivar.model.authentication

import androidx.credentials.Credential

/** The signed-in person as the rest of the app sees them, free of any Firebase type. */
data class AuthUser(val uid: String, val displayName: String?)

/** Signs people in and out. ViewModels depend on this interface, never on Firebase. */
interface AuthRepository {
  /** The signed-in user, or null when nobody is signed in. */
  fun currentUser(): AuthUser?

  /** Turns a credential obtained from the Credential Manager into a signed-in user. */
  suspend fun signInWithGoogle(credential: Credential): Result<AuthUser>

  fun signOut(): Result<Unit>
}
