// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.onboarding.splash

import com.android.festivar.model.authentication.AuthRepository
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SplashViewModelTest {
  private val authRepository = mockk<AuthRepository>()

  // With a signed-in user, the splash knows the user is signed in.
  @Test
  fun signedInUser_isLoggedIn() {
    every { authRepository.currentUser } returns mockk<FirebaseUser>()

    val viewModel = SplashViewModel(authRepository)

    assertEquals(SplashUIState(isLoggedIn = true), viewModel.uiState.value)
  }

  // Without a signed-in user, the splash knows no one is signed in.
  @Test
  fun noSignedInUser_isNotLoggedIn() {
    every { authRepository.currentUser } returns null

    val viewModel = SplashViewModel(authRepository)

    assertEquals(SplashUIState(isLoggedIn = false), viewModel.uiState.value)
  }

  // The signed-in user is read when the ViewModel is created, so a later sign-in does not change
  // the state the splash already decided on.
  @Test
  fun signInAfterCreation_doesNotChangeTheState() {
    var user: FirebaseUser? = null
    every { authRepository.currentUser } answers { user }
    val viewModel = SplashViewModel(authRepository)

    user = mockk()

    assertFalse(viewModel.uiState.value.isLoggedIn)
  }
}
