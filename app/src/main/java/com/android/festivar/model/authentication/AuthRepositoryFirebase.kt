// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.authentication

import androidx.credentials.Credential
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

class AuthRepositoryFirebase(
    private val googleSignInHelper: GoogleSignInHelper = GoogleSignInHelper(),
    private val firebaseAuth: FirebaseAuth = Firebase.auth,
    private val usersRepository: UsersRepository = UsersRepositoryProvider.repository,
) : AuthRepository {
  override val currentUser: FirebaseUser?
    get() = firebaseAuth.currentUser


  override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> =
      runCatching {
          val firebaseCredential = googleSignInHelper.getFirebaseCredential(credential)
          val user = firebaseAuth.signInWithCredential(firebaseCredential).await().user
              ?: throw IllegalStateException("Firebase sign-in returned no user")
          ensureUserExists(user)
          user
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

  /** Creates the [User] document of [firebaseUser] in Firestore if it does not exist yet. */
  private suspend fun ensureUserExists(firebaseUser: FirebaseUser) {
    try {
        usersRepository.getUser(firebaseUser.uid)
    } catch (_: NoSuchElementException) {
        usersRepository.addUser(User(uid = firebaseUser.uid, email = firebaseUser.email.orEmpty()))
    }
  }
}
