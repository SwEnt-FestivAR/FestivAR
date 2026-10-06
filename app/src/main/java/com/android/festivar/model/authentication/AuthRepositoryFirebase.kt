// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.authentication

import androidx.credentials.Credential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

class AuthRepositoryFirebase(
    private val googleSignInHelper: GoogleSignInHelper = GoogleSignInHelper(),
    private val firebaseAuth: FirebaseAuth = Firebase.auth,
) : AuthRepository {
  val currentUser: FirebaseUser?
    get() = firebaseAuth.currentUser

  override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> =
      runCatching {
        val firebaseCredential = googleSignInHelper.getFirebaseCredential(credential)
        firebaseAuth.signInWithCredential(firebaseCredential).await().user
            ?: throw IllegalStateException("Firebase sign-in returned no user")
      }

  override suspend fun signOut(): Result<Unit> = runCatching { firebaseAuth.signOut() }

  override suspend fun signInWithEmailAndPassword(
      email: String,
      password: String,
  ): Result<FirebaseUser> {
    return runCatching {
      firebaseAuth.signInWithEmailAndPassword(email, password).await().user
          ?: throw IllegalStateException("Firebase sign-in returned no user")
    }
  }

  override suspend fun signUpWithEmailAndPassword(
      email: String,
      password: String,
  ): Result<FirebaseUser> {
    return runCatching {
      firebaseAuth.createUserWithEmailAndPassword(email, password).await().user
          ?: throw IllegalStateException("Firebase sign-in returned no user")
    }
  }
}
