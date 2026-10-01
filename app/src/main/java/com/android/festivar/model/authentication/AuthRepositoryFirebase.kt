// Co-authored-by: Claude (Anthropic), through Claude Code as the coding agent.
package com.android.festivar.model.authentication

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

/** Firebase Authentication behind [AuthRepository]. The only place Firebase Auth is touched. */
class AuthRepositoryFirebase(
    private val auth: FirebaseAuth = Firebase.auth,
    private val helper: GoogleSignInHelper = DefaultGoogleSignInHelper(),
) : AuthRepository {
  override fun currentUser(): AuthUser? = auth.currentUser?.toAuthUser()

  override suspend fun signInWithGoogle(credential: Credential): Result<AuthUser> = runCatching {
    require(credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      "Unsupported credential type: ${credential.type}"
    }
    val idToken = helper.extractIdToken(credential.data)
    val user = auth.signInWithCredential(helper.toFirebaseCredential(idToken)).await().user
    user?.toAuthUser() ?: error("Firebase returned no user")
  }

  override fun signOut(): Result<Unit> = runCatching { auth.signOut() }

  private fun FirebaseUser.toAuthUser() = AuthUser(uid = uid, displayName = displayName)
}
