// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.authentication

import androidx.credentials.Credential
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
  val currentUser: FirebaseUser?
    get() = null

  /**
   * Emits the signed-in user now and at each sign-in or sign-out; null when nobody is signed in.
   */
  val currentUserFlow: Flow<FirebaseUser?>

  /**
   * Signs in with Google, or upgrades the current anonymous account if signed in anonymously. A
   * profile created or upgraded this way gets a name and a surname generated from the email.
   */
  suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser>

  /** signs out of the app */
  suspend fun signOut(): Result<Unit>

  /** Sign in with password and email */
  suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser>

  /**
   * Sign up with password and email, or upgrades the current anonymous account if signed in
   * anonymously. The profile gets a name and a surname generated from the email.
   */
  suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<FirebaseUser>

  /** Send a password reset email */
  suspend fun forgotPassword(email: String): Result<Unit>

  /** Signs in anonymously */
  suspend fun signInAnonymous(): Result<FirebaseUser>
}
