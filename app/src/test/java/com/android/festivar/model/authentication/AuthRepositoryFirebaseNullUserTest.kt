// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored by Gemini
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
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert
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
  fun signInAnonymous_returnsFailureWhenFirebaseReturnsNoUser() = runBlocking {
    val auth = mockk<FirebaseAuth>()
    every { auth.signInAnonymously() } returns Tasks.forResult(noUserResult())
    val repository = newRepository(auth)

    val result = repository.signInAnonymous()

    assertTrue(result.isFailure)
    assertEquals("Anonymous sign-in returned no user", result.exceptionOrNull()?.message)
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

  @Test
  fun signInWithGoogle_signsOutWhenProfileCheckFails() = runBlocking {
    val firebaseUser = mockk<FirebaseUser> { every { uid } returns UID }
    val authResult = mockk<AuthResult> { every { user } returns firebaseUser }
    val auth = mockk<FirebaseAuth>()
    every { auth.signInWithCredential(any()) } returns Tasks.forResult(authResult)
    every { auth.signOut() } just Runs
    val failure = IllegalStateException("Firestore unavailable")
    val usersRepository = mockk<UsersRepository> { coEvery { getUser(UID) } throws failure }
    val googleSignInHelper =
        mockk<GoogleSignInHelper> { every { getFirebaseCredential(any()) } returns mockk() }
    val repository =
        AuthRepositoryFirebase(
            googleSignInHelper = googleSignInHelper,
            firebaseAuth = auth,
            usersRepository = usersRepository,
        )
    val credential =
        CustomCredential(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, Bundle())

    val result = repository.signInWithGoogle(credential)

    assertSame(failure, result.exceptionOrNull())
    verify(exactly = 1) { auth.signOut() }
  }

  @Test
  fun signInAnonymous_signsOutWhenProfileCheckFails() = runBlocking {
    val firebaseUser = mockk<FirebaseUser> { every { uid } returns UID }
    val authResult = mockk<AuthResult> { every { user } returns firebaseUser }
    val auth = mockk<FirebaseAuth>()
    every { auth.signInAnonymously() } returns Tasks.forResult(authResult)
    every { auth.signOut() } just Runs
    val failure = IllegalStateException("Firestore unavailable")
    val usersRepository = mockk<UsersRepository> { coEvery { getUser(UID) } throws failure }
    val repository = newRepository(auth, usersRepository)

    val result = repository.signInAnonymous()

    assertSame(failure, result.exceptionOrNull())
    verify(exactly = 1) { auth.signOut() }
  }

  @Test
  fun forgotPassword_returnsFailureWhenGenericExceptionThrown() = runBlocking {
    val auth = mockk<FirebaseAuth>()
    val exception = RuntimeException("Network error")
    every { auth.sendPasswordResetEmail(EMAIL) } returns Tasks.forException(exception)
    val repository = newRepository(auth)

    val result = repository.forgotPassword(EMAIL)

    assertTrue(result.isFailure)
    assertSame(exception, result.exceptionOrNull())
  }

  @Test
  fun forgotPassword_returnsSuccessForInvalidUserException() = runBlocking {
    val auth = mockk<FirebaseAuth>()
    val exception = FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "User not found")
    every { auth.sendPasswordResetEmail(EMAIL) } returns Tasks.forException(exception)
    val repository = newRepository(auth)

    val result = repository.forgotPassword(EMAIL)

    assertTrue(result.isSuccess)
  }

  @Test
  fun signInWithGoogle_anonymousUpgradeEditsExistingProfileWhenUserExists() = runBlocking {
    val anonUser = mockk<FirebaseUser> { every { isAnonymous } returns true }
    val firebaseUser =
        mockk<FirebaseUser> {
          every { uid } returns UID
          every { email } returns EMAIL
        }
    val authResult = mockk<AuthResult> { every { user } returns firebaseUser }
    val auth =
        mockk<FirebaseAuth> {
          every { currentUser } returns anonUser
          every { anonUser.linkWithCredential(any()) } returns Tasks.forResult(authResult)
        }
    val googleSignInHelper =
        mockk<GoogleSignInHelper> { every { getFirebaseCredential(any()) } returns mockk() }
    val usersRepository =
        mockk<UsersRepository> {
          coEvery { getUser(UID) } returns User(uid = UID, email = "")
          coEvery { editUser(UID, any()) } just Runs
        }
    val repository =
        AuthRepositoryFirebase(
            googleSignInHelper = googleSignInHelper,
            firebaseAuth = auth,
            usersRepository = usersRepository,
        )
    val credential =
        CustomCredential(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, Bundle())

    val result = repository.signInWithGoogle(credential)

    assertTrue(result.isSuccess)
    assertEquals(firebaseUser, result.getOrNull())
    coVerify { usersRepository.editUser(UID, User(uid = UID, email = EMAIL)) }
  }

  @Test
  fun signInWithGoogle_addsProfileWhenUserDoesNotExist() = runBlocking {
    val firebaseUser =
        mockk<FirebaseUser> {
          every { uid } returns UID
          every { email } returns EMAIL
        }
    val authResult = mockk<AuthResult> { every { user } returns firebaseUser }
    val auth =
        mockk<FirebaseAuth> {
          every { signInWithCredential(any()) } returns Tasks.forResult(authResult)
        }
    val googleSignInHelper =
        mockk<GoogleSignInHelper> { every { getFirebaseCredential(any()) } returns mockk() }
    val usersRepository =
        mockk<UsersRepository> {
          coEvery { getUser(UID) } throws NoSuchElementException("Not found")
          coEvery { addUser(any()) } just Runs
        }
    val repository =
        AuthRepositoryFirebase(
            googleSignInHelper = googleSignInHelper,
            firebaseAuth = auth,
            usersRepository = usersRepository,
        )
    val credential =
        CustomCredential(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, Bundle())

    val result = repository.signInWithGoogle(credential)

    assertTrue(result.isSuccess)
    assertEquals(firebaseUser, result.getOrNull())
  }

  @Test
  fun signUpWithEmailAndPassword_anonymousUpgradeEditsExistingProfileWhenUserExists() =
      runBlocking {
        val anonUser = mockk<FirebaseUser> { every { isAnonymous } returns true }
        val linkedUser =
            mockk<FirebaseUser> {
              every { uid } returns UID
              every { email } returns EMAIL
            }
        val authResult = mockk<AuthResult> { every { user } returns linkedUser }
        val auth =
            mockk<FirebaseAuth> {
              every { currentUser } returns anonUser
              every { anonUser.linkWithCredential(any()) } returns Tasks.forResult(authResult)
            }

        val usersRepository =
            mockk<UsersRepository> {
              coEvery { getUser(UID) } returns User(uid = UID, email = "")
              coEvery { editUser(UID, any()) } just Runs
            }
        val repository =
            AuthRepositoryFirebase(firebaseAuth = auth, usersRepository = usersRepository)

        val result = repository.signUpWithEmailAndPassword(EMAIL, PASSWORD)

        assertTrue(result.isSuccess)
        assertEquals(linkedUser, result.getOrNull())
        coVerify { usersRepository.editUser(UID, User(uid = UID, email = EMAIL)) }
      }

  @Test
  fun signUpWithEmailAndPassword_anonymousUpgradeReturnsFailureWhenEditUserFails() = runBlocking {
    val anonUser = mockk<FirebaseUser> { every { isAnonymous } returns true }
    val linkedUser =
        mockk<FirebaseUser> {
          every { uid } returns UID
          every { email } returns EMAIL
        }
    val authResult = mockk<AuthResult> { every { user } returns linkedUser }
    val auth =
        mockk<FirebaseAuth> {
          every { currentUser } returns anonUser
          every { anonUser.linkWithCredential(any()) } returns Tasks.forResult(authResult)
        }

    val exception = IllegalStateException("Edit failed")
    val usersRepository =
        mockk<UsersRepository> {
          coEvery { getUser(UID) } returns User(uid = UID, email = "")
          coEvery { editUser(UID, any()) } throws exception
        }
    val repository = AuthRepositoryFirebase(firebaseAuth = auth, usersRepository = usersRepository)

    val result = repository.signUpWithEmailAndPassword(EMAIL, PASSWORD)

    assertTrue(result.isFailure)
    assertSame(exception, result.exceptionOrNull())
  }

  @Test
  fun signInWithGoogle_propagatesCancellationException() {
    runBlocking {
      val firebaseUser =
          mockk<FirebaseUser> {
            every { uid } returns UID
            every { email } returns EMAIL
          }
      val authResult = mockk<AuthResult> { every { user } returns firebaseUser }
      val auth =
          mockk<FirebaseAuth> {
            every { signInWithCredential(any()) } returns Tasks.forResult(authResult)
          }
      val googleSignInHelper =
          mockk<GoogleSignInHelper> { every { getFirebaseCredential(any()) } returns mockk() }
      val cancellation = CancellationException("Cancelled")
      val usersRepository = mockk<UsersRepository> { coEvery { getUser(UID) } throws cancellation }
      val repository =
          AuthRepositoryFirebase(
              googleSignInHelper = googleSignInHelper,
              firebaseAuth = auth,
              usersRepository = usersRepository,
          )
      val credential =
          CustomCredential(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, Bundle())

      Assert.assertThrows(CancellationException::class.java) {
        runBlocking { repository.signInWithGoogle(credential) }
      }
    }
  }

  @Test
  fun signUpWithEmailAndPassword_anonymousUpgradePropagatesCancellationException() {
    runBlocking {
      val anonUser = mockk<FirebaseUser> { every { isAnonymous } returns true }
      val linkedUser =
          mockk<FirebaseUser> {
            every { uid } returns UID
            every { email } returns EMAIL
          }
      val authResult = mockk<AuthResult> { every { user } returns linkedUser }
      val auth =
          mockk<FirebaseAuth> {
            every { currentUser } returns anonUser
            every { anonUser.linkWithCredential(any()) } returns Tasks.forResult(authResult)
          }

      val cancellation = CancellationException("Cancelled")
      val usersRepository = mockk<UsersRepository> { coEvery { getUser(UID) } throws cancellation }
      val repository =
          AuthRepositoryFirebase(firebaseAuth = auth, usersRepository = usersRepository)

      Assert.assertThrows(CancellationException::class.java) {
        runBlocking { repository.signUpWithEmailAndPassword(EMAIL, PASSWORD) }
      }
    }
  }

  @Test
  fun signOut_propagatesCancellationException() {
    runBlocking {
      val auth =
          mockk<FirebaseAuth> { every { signOut() } throws CancellationException("Cancelled") }
      val repository = newRepository(auth)
      Assert.assertThrows(CancellationException::class.java) {
        runBlocking { repository.signOut() }
      }
    }
  }

  @Test
  fun forgotPassword_propagatesCancellationException() {
    runBlocking {
      val auth =
          mockk<FirebaseAuth> {
            every { sendPasswordResetEmail(EMAIL) } returns
                Tasks.forException(CancellationException("Cancelled"))
          }
      val repository = newRepository(auth)
      Assert.assertThrows(CancellationException::class.java) {
        runBlocking { repository.forgotPassword(EMAIL) }
      }
    }
  }

  @Test
  fun signInAnonymous_propagatesCancellationException() {
    runBlocking {
      val auth =
          mockk<FirebaseAuth> {
            every { signInAnonymously() } returns
                Tasks.forException(CancellationException("Cancelled"))
          }
      val repository = newRepository(auth)
      Assert.assertThrows(CancellationException::class.java) {
        runBlocking { repository.signInAnonymous() }
      }
    }
  }

  @Test
  fun signInWithEmailAndPassword_propagatesCancellationException() {
    runBlocking {
      val auth =
          mockk<FirebaseAuth> {
            every { signInWithEmailAndPassword(EMAIL, PASSWORD) } returns
                Tasks.forException(CancellationException("Cancelled"))
          }
      val repository = newRepository(auth)
      Assert.assertThrows(CancellationException::class.java) {
        runBlocking { repository.signInWithEmailAndPassword(EMAIL, PASSWORD) }
      }
    }
  }

  @Test
  fun signInWithGoogle_anonymousUpgradeReturnsFailureWhenLinkingFails() = runBlocking {
    val anonUser = mockk<FirebaseUser> { every { isAnonymous } returns true }
    val exception = IllegalStateException("Linking failed")
    val auth =
        mockk<FirebaseAuth> {
          every { currentUser } returns anonUser
          every { anonUser.linkWithCredential(any()) } returns Tasks.forException(exception)
        }
    val googleSignInHelper =
        mockk<GoogleSignInHelper> { every { getFirebaseCredential(any()) } returns mockk() }
    val repository =
        AuthRepositoryFirebase(
            googleSignInHelper = googleSignInHelper,
            firebaseAuth = auth,
            usersRepository = mockk(),
        )
    val credential =
        CustomCredential(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, Bundle())

    val result = repository.signInWithGoogle(credential)

    assertTrue(result.isFailure)
    assertSame(exception, result.exceptionOrNull())
  }

  @Test
  fun signUpWithEmailAndPassword_anonymousUpgradeReturnsFailureWhenLinkingFails() = runBlocking {
    val anonUser = mockk<FirebaseUser> { every { isAnonymous } returns true }
    val exception = IllegalStateException("Linking failed")
    val auth =
        mockk<FirebaseAuth> {
          every { currentUser } returns anonUser
          every { anonUser.linkWithCredential(any()) } returns Tasks.forException(exception)
        }
    val repository = AuthRepositoryFirebase(firebaseAuth = auth, usersRepository = mockk())

    val result = repository.signUpWithEmailAndPassword(EMAIL, PASSWORD)

    assertTrue(result.isFailure)
    assertSame(exception, result.exceptionOrNull())
  }

  @Test
  fun signInWithGoogle_anonymousUpgradePropagatesCancellationExceptionDuringLinking() {
    runBlocking {
      val anonUser = mockk<FirebaseUser> { every { isAnonymous } returns true }
      val auth =
          mockk<FirebaseAuth> {
            every { currentUser } returns anonUser
            every { anonUser.linkWithCredential(any()) } returns
                Tasks.forException(CancellationException("Cancelled"))
          }
      val googleSignInHelper =
          mockk<GoogleSignInHelper> { every { getFirebaseCredential(any()) } returns mockk() }
      val repository =
          AuthRepositoryFirebase(
              googleSignInHelper = googleSignInHelper,
              firebaseAuth = auth,
              usersRepository = mockk(),
          )
      val credential =
          CustomCredential(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, Bundle())

      Assert.assertThrows(CancellationException::class.java) {
        runBlocking { repository.signInWithGoogle(credential) }
      }
    }
  }

  @Test
  fun signUpWithEmailAndPassword_anonymousUpgradePropagatesCancellationExceptionDuringLinking() {
    runBlocking {
      val anonUser = mockk<FirebaseUser> { every { isAnonymous } returns true }
      val auth =
          mockk<FirebaseAuth> {
            every { currentUser } returns anonUser
            every { anonUser.linkWithCredential(any()) } returns
                Tasks.forException(CancellationException("Cancelled"))
          }
      val repository = AuthRepositoryFirebase(firebaseAuth = auth, usersRepository = mockk())

      Assert.assertThrows(CancellationException::class.java) {
        runBlocking { repository.signUpWithEmailAndPassword(EMAIL, PASSWORD) }
      }
    }
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
