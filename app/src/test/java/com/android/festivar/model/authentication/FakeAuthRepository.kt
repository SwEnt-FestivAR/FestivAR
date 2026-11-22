// Co-authored-by: Gemini
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.model.authentication

import androidx.credentials.Credential
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

open class FakeAuthRepository(
    currentUser: FirebaseUser? = null,
    var signInResult: Result<FirebaseUser> =
        Result.failure(UnsupportedOperationException("Not used")),
    var signOutResult: Result<Unit> = Result.success(Unit),
    var forgotPasswordResult: Result<Unit> = Result.success(Unit),
    var signUpResult: Result<FirebaseUser> =
        Result.failure(UnsupportedOperationException("Not used")),
    var signInAnonymousResult: Result<FirebaseUser> =
        Result.failure(UnsupportedOperationException("Not used")),
    var lastResetEmail: String? = null,
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

  private val currentUserState = MutableStateFlow(currentUser)

  override val currentUser: FirebaseUser?
    get() = currentUserState.value

  override val currentUserFlow: Flow<FirebaseUser?> = currentUserState

  /** Signs [user] in, or signs out when null, as Firebase does while a screen is open. */
  fun setCurrentUser(user: FirebaseUser?) {
    currentUserState.value = user
  }

  var resetResult: Result<Unit>
    get() = forgotPasswordResult
    set(value) {
      forgotPasswordResult = value
    }

  override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> = signInResult

  override suspend fun signOut(): Result<Unit> {
    signOutCount++
    signOutGate?.await()
    val result = signOutBehavior?.invoke() ?: signOutResult
    // Like Firebase, a successful sign-out leaves nobody signed in.
    if (result.isSuccess) currentUserState.value = null
    return result
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
