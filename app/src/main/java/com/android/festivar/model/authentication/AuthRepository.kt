package com.android.festivar.model.authentication

import androidx.credentials.Credential
import com.google.firebase.auth.FirebaseUser

public interface AuthRepository {
  /** signs in with Google */
  suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser>

  /** signs out of the app */
  suspend fun signOut(): Result<Unit>

  /** Sign in with password and email */
  suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser>

  /** Sign up with password and email */
  suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<FirebaseUser>
}
