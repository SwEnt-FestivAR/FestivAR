// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.authentication

import android.os.Bundle
import androidx.credentials.CustomCredential
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryLocal
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
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
    val repository = newRepository(auth)
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
    val repository = newRepository(auth)

    val result = repository.signInWithEmailAndPassword(EMAIL, PASSWORD)

    assertNoUserFailure(result)
  }

  @Test
  fun signUpWithEmailAndPassword_returnsFailureWhenFirebaseReturnsNoUser() = runBlocking {
    val auth = mockk<FirebaseAuth>()
    every { auth.createUserWithEmailAndPassword(EMAIL, PASSWORD) } returns
        Tasks.forResult(noUserResult())
    val repository = newRepository(auth)

    val result = repository.signUpWithEmailAndPassword(EMAIL, PASSWORD)

    assertNoUserFailure(result)
  }

  @Test
  fun signUpWithEmailAndPassword_deletesAccountWhenProfileCreationFails() = runBlocking {
    val firebaseUser =
        mockk<FirebaseUser> {
          every { uid } returns UID
          every { email } returns EMAIL
          every { delete() } returns Tasks.forResult<Void>(null)
        }
    val authResult = mockk<AuthResult> { every { user } returns firebaseUser }
    val auth = mockk<FirebaseAuth>()
    every { auth.createUserWithEmailAndPassword(EMAIL, PASSWORD) } returns
        Tasks.forResult(authResult)
    // A profile already using this uid makes addUser fail.
    val repository = newRepository(auth, UsersRepositoryLocal(listOf(User(uid = UID))))

    val result = repository.signUpWithEmailAndPassword(EMAIL, PASSWORD)

    assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    verify(exactly = 1) { firebaseUser.delete() }
  }

  /**
   * Checks that a sign-in whose profile check fails, for example because Firestore is unavailable,
   * does not leave the user signed in without a profile: the user is signed out so they can retry,
   * and the original failure is returned for the caller to report.
   */
  @Test
  fun signInWithEmailAndPassword_signsOutWhenProfileCheckFails() = runBlocking {
    val firebaseUser = mockk<FirebaseUser> { every { uid } returns UID }
    val authResult = mockk<AuthResult> { every { user } returns firebaseUser }
    val auth = mockk<FirebaseAuth>()
    every { auth.signInWithEmailAndPassword(EMAIL, PASSWORD) } returns Tasks.forResult(authResult)
    every { auth.signOut() } just Runs
    // A mock stands in for a Firestore outage, which the local repository cannot simulate.
    val failure = IllegalStateException("Firestore unavailable")
    val usersRepository = mockk<UsersRepository> { coEvery { getUser(UID) } throws failure }
    val repository = newRepository(auth, usersRepository)

    val result = repository.signInWithEmailAndPassword(EMAIL, PASSWORD)

    assertSame(failure, result.exceptionOrNull())
    verify(exactly = 1) { auth.signOut() }
  }

  // A local users repository keeps the default Firestore one, unavailable here, from being built.
  private fun newRepository(
      auth: FirebaseAuth,
      usersRepository: UsersRepository = UsersRepositoryLocal(),
  ) = AuthRepositoryFirebase(firebaseAuth = auth, usersRepository = usersRepository)

  private fun noUserResult(): AuthResult = mockk { every { user } returns null }

  private fun assertNoUserFailure(result: Result<*>) {
    assertTrue(result.isFailure)
    assertEquals("Firebase sign-in returned no user", result.exceptionOrNull()?.message)
  }

  private companion object {
    const val UID = "auth-repository-uid"
    const val EMAIL = "auth-repository@example.com"
    const val PASSWORD = "Password123!"
  }
}
