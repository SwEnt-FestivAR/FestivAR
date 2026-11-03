// Co-authored-by: Gemini
package com.android.festivar.model.authentication

import androidx.credentials.Credential
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CompletableDeferred

open class FakeAuthRepository(
    override val currentUser: FirebaseUser? = null,
    var signInResult: Result<FirebaseUser> = Result.failure(UnsupportedOperationException("Not used")),    var signOutResult: Result<Unit> = Result.success(Unit),
    var forgotPasswordResult: Result<Unit> = Result.success(Unit),
    var signUpResult: Result<FirebaseUser> = Result.failure(UnsupportedOperationException("Not used")),
    var signInAnonymousResult: Result<FirebaseUser> = Result.failure(UnsupportedOperationException("Not used")),    var lastResetEmail: String? = null,
    var lastSignUpCredentials: Pair<String, String>? = null,
    var signOutCount: Int = 0,
    var signOutGate: CompletableDeferred<Unit>? = null,
    var pendingResetResult: CompletableDeferred<Result<Unit>>? = null,
    var signInDeferred: CompletableDeferred<Result<FirebaseUser>>? = null,
    var signUpBehavior: (suspend (String, String) -> Result<FirebaseUser>)? = null,
    var signUpErrorMessage: String = "Not configured",
    var signOutBehavior: (suspend () -> Result<Unit>)? = null,
    var signUpCallCount: Int = 0,
) : AuthRepository {

  var resetResult: Result<Unit>
    get() = forgotPasswordResult
    set(value) {
      forgotPasswordResult = value
    }

  override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> = signInResult

  override suspend fun signOut(): Result<Unit> {
    signOutCount++
    signOutGate?.await()
    return signOutBehavior?.invoke() ?: signOutResult
  }

  override suspend fun forgotPassword(email: String): Result<Unit> {
    lastResetEmail = email
    return pendingResetResult?.await() ?: forgotPasswordResult
  }

  override suspend fun signInWithEmailAndPassword(
      email: String,
      password: String,
  ): Result<FirebaseUser> = signInDeferred?.await() ?: signInResult

  override suspend fun signUpWithEmailAndPassword(
      email: String,
      password: String,
  ): Result<FirebaseUser> {
    signUpCallCount++
    lastSignUpCredentials = email to password
    if (
        signUpResult.isFailure &&
            signUpErrorMessage != "Not configured" &&
            signUpResult.exceptionOrNull()?.message == "Not used"
    ) {
      return signUpBehavior?.invoke(email, password)
          ?: Result.failure(UnsupportedOperationException(signUpErrorMessage))
    }
    return signUpBehavior?.invoke(email, password) ?: signUpResult
  }

  override suspend fun signInAnonymous(): Result<FirebaseUser> = signInAnonymousResult
}
