// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.authentication

import android.os.Bundle
import androidx.credentials.CustomCredential
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthRepositoryFirebaseNullUserTest {
  @Test
  fun signInWithGoogle_returnsFailureWhenFirebaseReturnsNoUser() = runBlocking {
    val auth = mockk<FirebaseAuth>()
    every { auth.signInWithCredential(any<AuthCredential>()) } returns
        Tasks.forResult(noUserResult())
    val repository = AuthRepositoryFirebase(firebaseAuth = auth)
    val credential =
        CustomCredential(
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
            Bundle().apply { putString("id_token", "test-id-token") },
        )

    val result = repository.signInWithGoogle(credential)

    assertNoUserFailure(result)
  }

  @Test
  fun signInWithEmailAndPassword_returnsFailureWhenFirebaseReturnsNoUser() = runBlocking {
    val auth = mockk<FirebaseAuth>()
    every { auth.signInWithEmailAndPassword(EMAIL, PASSWORD) } returns
        Tasks.forResult(noUserResult())
    val repository = AuthRepositoryFirebase(firebaseAuth = auth)

    val result = repository.signInWithEmailAndPassword(EMAIL, PASSWORD)

    assertNoUserFailure(result)
  }

  @Test
  fun signUpWithEmailAndPassword_returnsFailureWhenFirebaseReturnsNoUser() = runBlocking {
    val auth = mockk<FirebaseAuth>()
    every { auth.createUserWithEmailAndPassword(EMAIL, PASSWORD) } returns
        Tasks.forResult(noUserResult())
    val repository = AuthRepositoryFirebase(firebaseAuth = auth)

    val result = repository.signUpWithEmailAndPassword(EMAIL, PASSWORD)

    assertNoUserFailure(result)
  }

  private fun noUserResult(): AuthResult = mockk { every { user } returns null }

  private fun assertNoUserFailure(result: Result<*>) {
    assertTrue(result.isFailure)
    assertEquals("Firebase sign-in returned no user", result.exceptionOrNull()?.message)
  }

  private companion object {
    const val EMAIL = "auth-repository@example.com"
    const val PASSWORD = "Password123!"
  }
}
