// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.authentication

import androidx.credentials.Credential
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
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
        val user =
            firebaseAuth.signInWithCredential(firebaseCredential).await().user
                ?: throw IllegalStateException("Firebase sign-in returned no user")
        ensureUserExistsOrSignOut(user)
        user
      }

  override suspend fun signOut(): Result<Unit> = runCatching { firebaseAuth.signOut() }

  override suspend fun forgotPassword(email: String): Result<Unit> =
      try {
        firebaseAuth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
      } catch (_: FirebaseAuthInvalidUserException) {
        Result.success(Unit)
      } catch (exception: Exception) {
        Result.failure(exception)
      }

  override suspend fun signInWithEmailAndPassword(
      email: String,
      password: String,
  ): Result<FirebaseUser> {
    return runCatching {
      val user =
          firebaseAuth.signInWithEmailAndPassword(email, password).await().user
              ?: throw IllegalStateException("Firebase sign-in returned no user")
      ensureUserExistsOrSignOut(user)
      user
    }
  }

  override suspend fun signUpWithEmailAndPassword(
      email: String,
      password: String,
  ): Result<FirebaseUser> {
    return runCatching {
      val user =
          firebaseAuth.createUserWithEmailAndPassword(email, password).await().user
              ?: throw IllegalStateException("Firebase sign-in returned no user")

      try {
        usersRepository.addUser(User(uid = user.uid, email = user.email.orEmpty()))
      } catch (e: Exception) {
        user.delete().await()
        throw e
      }

      user
    }
  }

  /**
   * Creates the [User] document of [firebaseUser] in Firestore if it does not exist yet.
   *
   * @param firebaseUser The [firebaseUser] that has to have its equivalent as a [User].
   */
  private suspend fun ensureUserExists(firebaseUser: FirebaseUser) {
    try {
      usersRepository.getUser(firebaseUser.uid)
    } catch (_: NoSuchElementException) {
      usersRepository.addUser(User(uid = firebaseUser.uid, email = firebaseUser.email.orEmpty()))
    }
  }

  /**
   * Tries to create the [User] document of [firebaseUser] in Firestore if it does not exist yet by
   * calling the `ensureUserExists`. The main difference is that while `ensureUserExists` handles
   * just the working of one exception. While not handling its proper other exceptions, it exposes
   * itself to the same problem as it tries to solve, having a [FirebaseUser] connected while the
   * corresponding [User] does not exist. If such a situation occurs, the user should be signed out
   * so he can try again connecting.
   *
   * @param firebaseUser The [firebaseUser] that has to have its equivalent as a [User].
   */
  private suspend fun ensureUserExistsOrSignOut(firebaseUser: FirebaseUser) {
    try {
      ensureUserExists(firebaseUser)
    } catch (e: Exception) {
      firebaseAuth.signOut()
      throw e
    }
  }
}
