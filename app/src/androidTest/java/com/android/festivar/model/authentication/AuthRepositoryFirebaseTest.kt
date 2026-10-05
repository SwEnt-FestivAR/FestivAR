// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.model.authentication

import android.os.Bundle
import androidx.credentials.CustomCredential
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AuthRepositoryFirebaseTest {
  private lateinit var repository: AuthRepositoryFirebase

  @Before
  fun setUp() {
    Firebase.auth.useEmulator("10.0.2.2", 9099)
    Firebase.auth.signOut()
    repository = AuthRepositoryFirebase()
  }

  @Test
  fun signUpWithEmailAndPassword_returnsCreatedUserAndSetsCurrentUser() = runBlocking {
    val email = newEmail()

    val user = repository.signUpWithEmailAndPassword(email, PASSWORD).getOrThrow()

    assertEquals(email, user.email)
    assertNotNull(repository.currentUser)
    assertEquals(user.uid, repository.currentUser?.uid)
  }

  @Test
  fun signUpWithEmailAndPassword_returnsFailureForExistingEmail() = runBlocking {
    val email = newEmail()
    repository.signUpWithEmailAndPassword(email, PASSWORD).getOrThrow()

    val result = repository.signUpWithEmailAndPassword(email, PASSWORD)

    assertTrue(result.isFailure)
  }

  @Test
  fun signInWithEmailAndPassword_returnsUserForValidCredentials() = runBlocking {
    val email = newEmail()
    Firebase.auth.createUserWithEmailAndPassword(email, PASSWORD).await()
    Firebase.auth.signOut()

    val user = repository.signInWithEmailAndPassword(email, PASSWORD).getOrThrow()

    assertEquals(email, user.email)
    assertEquals(user.uid, repository.currentUser?.uid)
  }

  @Test
  fun signInWithEmailAndPassword_returnsFailureForWrongPassword() = runBlocking {
    val email = newEmail()
    Firebase.auth.createUserWithEmailAndPassword(email, PASSWORD).await()
    Firebase.auth.signOut()

    val result = repository.signInWithEmailAndPassword(email, "incorrect-password")

    assertTrue(result.isFailure)
    assertNull(repository.currentUser)
  }

  @Test
  fun signOut_clearsCurrentUser() = runBlocking {
    repository.signUpWithEmailAndPassword(newEmail(), PASSWORD).getOrThrow()
    assertNotNull(repository.currentUser)

    val result = repository.signOut()

    assertTrue(result.isSuccess)
    assertNull(repository.currentUser)
  }

  @Test
  fun signInWithGoogle_returnsUserForEmulatorIdToken() = runBlocking {
    val email = newEmail()
    val idToken = """{"sub":"${UUID.randomUUID()}","email":"$email","email_verified":true}"""
    val data = Bundle().apply { putString("id_token", idToken) }
    val credential = CustomCredential(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, data)

    val user = repository.signInWithGoogle(credential).getOrThrow()

    assertEquals(email, user.email)
    assertEquals(user.uid, repository.currentUser?.uid)
  }

  @Test
  fun signInWithGoogle_returnsFailureForNonGoogleCredential() = runBlocking {
    val credential = CustomCredential("unexpected-credential-type", Bundle())

    val result = repository.signInWithGoogle(credential)

    assertTrue(result.isFailure)
  }

  private fun newEmail() = "auth-repository-${UUID.randomUUID()}@example.com"

  private companion object {
    const val PASSWORD = "Password123!"
  }
}
