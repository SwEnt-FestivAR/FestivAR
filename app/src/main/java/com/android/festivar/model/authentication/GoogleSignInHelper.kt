// Co-authored-by: Claude (Anthropic), through Claude Code as the coding agent.
package com.android.festivar.model.authentication

import android.os.Bundle
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider

/**
 * The two static calls into the Google SDKs, behind an interface so that [AuthRepositoryFirebase]
 * can be unit tested without static mocking.
 */
interface GoogleSignInHelper {
  /** Reads the Google ID token out of the credential's data bundle. */
  fun extractIdToken(data: Bundle): String

  /** Wraps a Google ID token into a credential Firebase can sign in with. */
  fun toFirebaseCredential(idToken: String): AuthCredential
}

class DefaultGoogleSignInHelper : GoogleSignInHelper {
  override fun extractIdToken(data: Bundle): String =
      GoogleIdTokenCredential.createFrom(data).idToken

  override fun toFirebaseCredential(idToken: String): AuthCredential =
      GoogleAuthProvider.getCredential(idToken, null)
}
