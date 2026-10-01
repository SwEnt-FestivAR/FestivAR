// Co-authored-by: Claude (Anthropic), through Claude Code as the coding agent.
package com.android.festivar.utils

import android.content.Context
import android.os.Bundle
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.test.core.app.ApplicationProvider
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.authentication.AuthUser
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL

/** In-memory sign-in state: no Google account, no Firebase, no network. */
class FakeAuthRepository(private var user: AuthUser? = null, private val fail: Boolean = false) :
    AuthRepository {
  override fun currentUser(): AuthUser? = user

  override suspend fun signInWithGoogle(credential: Credential): Result<AuthUser> {
    if (fail) return Result.failure(IllegalStateException("rejected"))
    user = ADA
    return Result.success(ADA)
  }

  override fun signOut(): Result<Unit> {
    user = null
    return Result.success(Unit)
  }

  companion object {
    val ADA = AuthUser("uid-ada", "Ada")
  }
}

/**
 * A Credential Manager that hands back a Google credential without showing any picker. Only the two
 * calls the app makes are overridden; the rest delegates to the real manager.
 */
class FakeCredentialManager private constructor(context: Context) :
    CredentialManager by CredentialManager.create(context) {
  override suspend fun getCredential(
      context: Context,
      request: GetCredentialRequest,
  ): GetCredentialResponse =
      GetCredentialResponse(CustomCredential(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, Bundle()))

  override suspend fun clearCredentialState(request: ClearCredentialStateRequest) {}

  companion object {
    fun create(): CredentialManager =
        FakeCredentialManager(ApplicationProvider.getApplicationContext())
  }
}
