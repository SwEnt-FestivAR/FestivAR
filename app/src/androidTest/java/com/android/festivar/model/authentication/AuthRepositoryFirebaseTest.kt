// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.authentication

import android.os.Bundle
import androidx.credentials.CustomCredential
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.FirebaseEmulatorSetup
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UserRepositoryFirebase
import com.android.festivar.model.user.UsersRepository
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthRepositoryFirebaseTest {
  private lateinit var usersRepository: UsersRepository
  private lateinit var repository: AuthRepositoryFirebase

  @Before
  fun setUp() {
    FirebaseEmulatorSetup.configure()
    Firebase.auth.signOut()
    usersRepository = UserRepositoryFirebase(Firebase.firestore)
    repository = AuthRepositoryFirebase(usersRepository = usersRepository)
  }

  @Test
  fun signUpWithEmailAndPassword_returnsCreatedUserAndCreatesProfile() = runBlocking {
    val email = newEmail()

    val user = repository.signUpWithEmailAndPassword(email, PASSWORD).getOrThrow()

    assertEquals(email, user.email)
    assertNotNull(repository.currentUser)
    assertEquals(user.uid, repository.currentUser?.uid)
    assertEquals(User(uid = user.uid, email = email), usersRepository.getUser(user.uid))
  }

  @Test
  fun signUpWithEmailAndPassword_returnsFailureForExistingEmail() = runBlocking {
    val email = newEmail()
    repository.signUpWithEmailAndPassword(email, PASSWORD).getOrThrow()

    val result = repository.signUpWithEmailAndPassword(email, PASSWORD)

    assertTrue(result.isFailure)
  }

  @Test
  fun signInWithEmailAndPassword_returnsUserAndCreatesMissingProfile() = runBlocking {
    val email = newEmail()
    // An account created without its profile, like the ones made before profiles existed.
    Firebase.auth.createUserWithEmailAndPassword(email, PASSWORD).await()
    Firebase.auth.signOut()

    val user = repository.signInWithEmailAndPassword(email, PASSWORD).getOrThrow()

    assertEquals(email, user.email)
    assertEquals(user.uid, repository.currentUser?.uid)
    assertEquals(User(uid = user.uid, email = email), usersRepository.getUser(user.uid))
  }

  @Test
  fun signInWithEmailAndPassword_keepsExistingProfile() = runBlocking {
    val user = repository.signUpWithEmailAndPassword(newEmail(), PASSWORD).getOrThrow()
    val profile = usersRepository.getUser(user.uid).copy(name = "Ada", surname = "Lovelace")
    usersRepository.editUser(user.uid, profile)
    repository.signOut()

    repository.signInWithEmailAndPassword(user.email!!, PASSWORD).getOrThrow()

    assertEquals(profile, usersRepository.getUser(user.uid))
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
  fun forgotPassword_returnsSuccessForExistingEmail() = runBlocking {
    val email = newEmail()
    Firebase.auth.createUserWithEmailAndPassword(email, PASSWORD).await()

    val result = repository.forgotPassword(email)

    assertTrue(result.isSuccess)
  }

  @Test
  fun forgotPassword_returnsSuccessForUnknownEmail() = runBlocking {
    val result = repository.forgotPassword(newEmail())

    assertTrue(result.isSuccess)
  }

  @Test
  fun forgotPassword_returnsFailureForInvalidEmail() = runBlocking {
    val result = repository.forgotPassword("")

    assertTrue(result.isFailure)
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
  fun signInWithGoogle_returnsUserAndCreatesProfile() = runBlocking {
    val email = newEmail()
    val idToken = """{"sub":"${UUID.randomUUID()}","email":"$email","email_verified":true}"""
    val data = Bundle().apply { putString("id_token", idToken) }
    val credential = CustomCredential(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, data)

    val user = repository.signInWithGoogle(credential).getOrThrow()

    assertEquals(email, user.email)
    assertEquals(user.uid, repository.currentUser?.uid)
    assertEquals(User(uid = user.uid, email = email), usersRepository.getUser(user.uid))
  }

  @Test
  fun signInWithGoogle_returnsFailureForNonGoogleCredential() = runBlocking {
    val credential = CustomCredential("unexpected-credential-type", Bundle())

    val result = repository.signInWithGoogle(credential)

    assertTrue(result.isFailure)
  }

  @After
  fun tearDown() {
    Firebase.auth.signOut()
  }

  private fun newEmail() = "auth-repository-${UUID.randomUUID()}@example.com"

  private companion object {
    @JvmStatic
    @BeforeClass
    fun configureFirebaseEmulators() {
      FirebaseEmulatorSetup.configure()
    }

    const val PASSWORD = "Password123!"
  }
}
