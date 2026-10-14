// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.onboarding.splash

import androidx.lifecycle.ViewModel
import com.android.festivar.model.authentication.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents the UI state of the splash screen.
 *
 * @property isReady Whether everything the app needs at startup is set up.
 * @property isLoggedIn Whether a user is signed in. Only known once [isReady].
 */
data class SplashUIState(
    val isReady: Boolean = false,
    val isLoggedIn: Boolean = false,
)

/**
 * ViewModel of the splash screen.
 *
 * Sets the app up at startup and tells the screen when it is ready and whether a user is signed in,
 * which is what decides the screen shown next.
 *
 * @property authRepository The repository used to know whether a user is signed in.
 */
class SplashViewModel(private val authRepository: AuthRepository) : ViewModel() {

  // Knowing whether a user is signed in is all the app needs at startup for now, and it takes no
  // time. Startup work that does take time belongs here, with isReady set once it is done.
  private val _uiState =
      MutableStateFlow(
          SplashUIState(isReady = true, isLoggedIn = authRepository.currentUser != null)
      )
  val uiState: StateFlow<SplashUIState> = _uiState.asStateFlow()
}
