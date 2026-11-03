// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.authentication

import android.util.Log
import androidx.credentials.Credential
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

// Logs tag
private const val TAG = "AuthRepositoryFirebase"

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
        val currentUser = runCatching { firebaseAuth.currentUser }.getOrNull()
        // Upgrade anonymous account with Google if current user is anonymous
        val user =
            if (currentUser != null && currentUser.isAnonymous) {
              currentUser.linkWithCredential(firebaseCredential).await().user
                  ?: throw IllegalStateException("Google account linking returned no user")
            } else {
              firebaseAuth.signInWithCredential(firebaseCredential).await().user
                  ?: throw IllegalStateException("Firebase sign-in returned no user")
            }
        ensureUserExistsOrSignOut(user)
        try {
          val existing = usersRepository.getUser(user.uid)
          usersRepository.editUser(user.uid, existing.copy(email = user.email.orEmpty()))
        } catch (_: Exception) {
          ensureUserExists(user)
        }
        Log.d(TAG, "Successfully signed in/linked user with Google: ${user.uid}")
        user
      }
      .onFailure { e -> Log.e(TAG, "Failed to sign in with Google", e) }

  override suspend fun signOut(): Result<Unit> = runCatching { firebaseAuth.signOut() }

  override suspend fun forgotPassword(email: String): Result<Unit> =
      try {
        firebaseAuth.sendPasswordResetEmail(email).await()
        Log.d(TAG, "Password reset email sent to ${maskEmail(email)}.")
        Result.success(Unit)
      } catch (_: FirebaseAuthInvalidUserException) {
        Log.w(
            TAG,
            "Password reset attempted for non-existent or invalid user email: ${maskEmail(email)}",
        )
        Result.success(Unit)
      } catch (exception: Exception) {
        Log.e(TAG, "Failed to send password reset email", exception)
        Result.failure(exception)
      }

  override suspend fun signInAnonymous(): Result<FirebaseUser> {
    return runCatching {
      val user =
          firebaseAuth.signInAnonymously().await().user
              ?: throw IllegalStateException("Anonymous sign-in returned no user")
      ensureUserExistsOrSignOut(user)
      Log.d(TAG, "Successfully signed in anonymously: ${user.uid}")
      user
    }
        .onFailure { e -> Log.e(TAG, "Failed to sign in anonymously", e) }
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
      Log.d(TAG, "Successfully signed in user with mail and pass: ${user.uid}")
      user
    }
        .onFailure { e -> Log.e(TAG, "Failed to sign in with mail and pass", e) }
  }

  override suspend fun signUpWithEmailAndPassword(
      email: String,
      password: String,
  ): Result<FirebaseUser> {
    return runCatching {
      val currentUser = runCatching { firebaseAuth.currentUser }.getOrNull()
      // Check if current user is anonymous for account upgrade/linking
      val isAnonymousLink = currentUser != null && currentUser.isAnonymous
      val user =
          if (isAnonymousLink) {
            val emailCredential = EmailAuthProvider.getCredential(email, password)
            // Link email credential to upgrade anonymous account, preserving UID
            currentUser.linkWithCredential(emailCredential).await().user
                ?: throw IllegalStateException("Email account linking returned no user")
          } else {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await().user
                ?: throw IllegalStateException("Firebase sign-in returned no user")
          }

      try {
        if (isAnonymousLink) {
          // Ensure profile exists and update email in Firestore for upgraded user
          ensureUserExistsOrSignOut(user)
          try {
            val existing = usersRepository.getUser(user.uid)
            usersRepository.editUser(user.uid, existing.copy(email = user.email.orEmpty()))
          } catch (_: Exception) {
            usersRepository.addUser(
                User(
                    uid = user.uid,
                    email = user.email.orEmpty(),
                )
            )
          }
        } else {
          usersRepository.addUser(
              User(
                  uid = user.uid,
                  email = user.email.orEmpty(),
              )
          )
        }
        Log.d(
            TAG,
            "Successfully created/linked user and Firestore document",
        )
      } catch (e: Exception) {
        Log.e(
            TAG,
            "Failed to create/update Firestore document for user",
            e,
        )
        if (!isAnonymousLink) {
          user.delete().await()
        }
        throw e
      }

      user
    }
        .onFailure { e -> Log.d(TAG, "Sign up/linking failed", e) }
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
