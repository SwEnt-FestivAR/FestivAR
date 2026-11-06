// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.model.authentication

import android.os.Bundle
import androidx.credentials.CustomCredential
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.festivar.FirebaseEmulatorSetup
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryFirebase
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
  private val createdUids = mutableSetOf<String>()

  @Before
  fun setUp() {
    FirebaseEmulatorSetup.configure()
    Firebase.auth.signOut()
    createdUids.clear()
    usersRepository = UsersRepositoryFirebase(Firebase.firestore)
    repository = AuthRepositoryFirebase(usersRepository = usersRepository)
  }

  @Test
  fun signUpWithEmailAndPassword_returnsCreatedUserAndCreatesProfile() = runBlocking {
    val email = newEmail()

    val user = signUp(email, PASSWORD)

    assertEquals(email, user.email)
    assertNotNull(repository.currentUser)
    assertEquals(user.uid, repository.currentUser?.uid)
    assertEquals(User(uid = user.uid, email = email), usersRepository.getUser(user.uid))
  }

  @Test
  fun signUpWithEmailAndPassword_returnsFailureForExistingEmail() = runBlocking {
    val email = newEmail()
    signUp(email, PASSWORD)

    val result = repository.signUpWithEmailAndPassword(email, PASSWORD)

    assertTrue(result.isFailure)
  }

  @Test
  fun signInWithEmailAndPassword_returnsUserAndCreatesMissingProfile() = runBlocking {
    val email = newEmail()
    // An account created without its profile, like the ones made before profiles existed.
    createUser(email, PASSWORD)
    Firebase.auth.signOut()

    val user = signIn(email, PASSWORD)

    assertEquals(email, user.email)
    assertEquals(user.uid, repository.currentUser?.uid)
    assertEquals(User(uid = user.uid, email = email), usersRepository.getUser(user.uid))
  }

  @Test
  fun signInWithEmailAndPassword_keepsExistingProfile() = runBlocking {
    val user = signUp(newEmail(), PASSWORD)
    val profile = usersRepository.getUser(user.uid).copy(name = "Ada", surname = "Lovelace")
    usersRepository.editUser(user.uid, profile)
    repository.signOut()

    signIn(user.email!!, PASSWORD)

    assertEquals(profile, usersRepository.getUser(user.uid))
  }

  @Test
  fun signInWithEmailAndPassword_returnsFailureForWrongPassword() = runBlocking {
    val email = newEmail()
    createUser(email, PASSWORD)
    Firebase.auth.signOut()

    val result = repository.signInWithEmailAndPassword(email, "incorrect-password")

    assertTrue(result.isFailure)
    assertNull(repository.currentUser)
  }

  @Test
  fun forgotPassword_returnsSuccessForExistingEmail() = runBlocking {
    val email = newEmail()
    createUser(email, PASSWORD)

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
    signUp(newEmail(), PASSWORD)
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

    val user = signInGoogle(credential)

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

  @Test
  fun signInAnonymous_returnsUserAndCreatesProfile() = runBlocking {
    val user = signInAnon()

    assertNotNull(user)
    assertTrue(user.isAnonymous)
    assertEquals(
        user.uid,
        repository.currentUser?.uid,
    )
    assertEquals(
        User(uid = user.uid, email = ""),
        usersRepository.getUser(user.uid),
    )
  }

  @Test
  fun upgradeAnonymousAccountWithEmailAndPassword_preservesUidAndUpdatesEmail() = runBlocking {
    val anonUser = signInAnon()
    val anonUid = anonUser.uid
    assertTrue(anonUser.isAnonymous)

    val email = newEmail()
    val upgradedUser = signUp(email, PASSWORD)

    assertEquals(anonUid, upgradedUser.uid)
    assertEquals(email, upgradedUser.email)
    assertFalse(upgradedUser.isAnonymous)
    assertEquals(User(uid = anonUid, email = email), usersRepository.getUser(anonUid))
  }

  @Test
  fun upgradeAnonymousAccountWithGoogle_preservesUidAndUpdatesEmail() = runBlocking {
    val anonUser = signInAnon()
    val anonUid = anonUser.uid
    assertTrue(anonUser.isAnonymous)

    val email = newEmail()
    val idToken = """{"sub":"${UUID.randomUUID()}","email":"$email","email_verified":true}"""
    val data = Bundle().apply { putString("id_token", idToken) }
    val credential = CustomCredential(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL, data)

    val upgradedUser = signInGoogle(credential)

    assertEquals(anonUid, upgradedUser.uid)
    assertEquals(email, upgradedUser.email)
    assertFalse(upgradedUser.isAnonymous)
    assertEquals(User(uid = anonUid, email = email), usersRepository.getUser(anonUid))
  }

  @After
  fun tearDown() {
    runBlocking {
      if (Firebase.auth.currentUser == null) {
        runCatching { Firebase.auth.signInAnonymously().await() }
      }
      val uid = Firebase.auth.currentUser?.uid
      if (uid != null) {
        createdUids.add(uid)
      }
      for (u in createdUids) {
        try {
          usersRepository.deleteUser(u)
        } catch (_: Exception) {}
      }
      createdUids.clear()
    }
    Firebase.auth.signOut()
  }

  private fun newEmail() = "auth-repository-${UUID.randomUUID()}@example.com"

  private suspend fun signUp(email: String, password: String) =
      repository.signUpWithEmailAndPassword(email, password).getOrThrow().also {
        createdUids.add(it.uid)
      }

  private suspend fun signIn(email: String, password: String) =
      repository.signInWithEmailAndPassword(email, password).getOrThrow().also {
        createdUids.add(it.uid)
      }

  private suspend fun signInGoogle(credential: CustomCredential) =
      repository.signInWithGoogle(credential).getOrThrow().also { createdUids.add(it.uid) }

  private suspend fun signInAnon() =
      repository.signInAnonymous().getOrThrow().also { createdUids.add(it.uid) }

  private suspend fun createUser(email: String, password: String) =
      Firebase.auth.createUserWithEmailAndPassword(email, password).await().user!!.also {
        createdUids.add(it.uid)
      }

  private companion object {
    @JvmStatic
    @BeforeClass
    fun configureFirebaseEmulators() {
      FirebaseEmulatorSetup.configure()
    }

    const val PASSWORD = "Password123!"
  }
}
