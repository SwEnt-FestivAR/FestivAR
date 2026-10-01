// Co-authored-by: Claude (Anthropic), through Claude Code as the coding agent.
package com.android.festivar.model.authentication

import android.os.Bundle
import androidx.credentials.CustomCredential
import androidx.credentials.PasswordCredential
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Firebase and the Google helper are mocked, so nothing here touches a real account. */
class AuthRepositoryFirebaseTest {
  private val auth = mockk<FirebaseAuth>(relaxed = true)
  private val firebaseCredential = mockk<AuthCredential>()
  private val user =
      mockk<FirebaseUser> {
        every { uid } returns "uid-7"
        every { displayName } returns "Ada"
      }

  /** Reads a fixed token and hands back the mocked Firebase credential for it. */
  private val helper =
      object : GoogleSignInHelper {
        override fun extractIdToken(data: Bundle) = "token-123"

        override fun toFirebaseCredential(idToken: String): AuthCredential {
          assertEquals("token-123", idToken)
          return firebaseCredential
        }
      }

  private val repository = AuthRepositoryFirebase(auth, helper)

  private fun googleCredential() =
      CustomCredential(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, mockk<Bundle>(relaxed = true))

  private fun firebaseReturns(signedIn: FirebaseUser?) {
    val result = mockk<AuthResult>()
    every { result.user } returns signedIn
    every { auth.signInWithCredential(firebaseCredential) } returns Tasks.forResult(result)
  }

  @Test
  fun signsInWithTheCredentialBuiltFromTheTokenAndMapsTheUser() = runBlocking {
    firebaseReturns(user)

    val outcome = repository.signInWithGoogle(googleCredential())

    assertEquals(AuthUser("uid-7", "Ada"), outcome.getOrNull())
    verify { auth.signInWithCredential(firebaseCredential) }
  }

  @Test
  fun rejectsACredentialThatIsNotAGoogleIdToken() = runBlocking {
    val outcome = repository.signInWithGoogle(PasswordCredential("id", "secret"))

    assertTrue(outcome.isFailure)
    verify(exactly = 0) { auth.signInWithCredential(any()) }
  }

  @Test
  fun reportsAFirebaseFailureWithItsMessage() = runBlocking {
    every { auth.signInWithCredential(any()) } returns
        Tasks.forException(IllegalStateException("network"))

    val outcome = repository.signInWithGoogle(googleCredential())

    assertTrue(outcome.isFailure)
    assertEquals("network", outcome.exceptionOrNull()?.message)
  }

  @Test
  fun reportsAMissingUserAsAFailure() = runBlocking {
    firebaseReturns(null)

    assertTrue(repository.signInWithGoogle(googleCredential()).isFailure)
  }

  @Test
  fun currentUserComesFromFirebase() {
    every { auth.currentUser } returns null
    assertNull(repository.currentUser())

    every { auth.currentUser } returns user
    assertEquals(AuthUser("uid-7", "Ada"), repository.currentUser())
  }

  @Test
  fun signOutEndsTheFirebaseSession() {
    assertTrue(repository.signOut().isSuccess)
    verify { auth.signOut() }
  }
}
