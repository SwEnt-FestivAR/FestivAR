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
 * @property isLoggedIn Whether a user is signed in.
 */
data class SplashUIState(
    val isLoggedIn: Boolean = false,
)

/**
 * ViewModel of the splash screen.
 *
 * Decides the first screen shown after the system splash, from whether a user is signed in.
 *
 * @property authRepository The repository used to know whether a user is signed in.
 */
class SplashViewModel(private val authRepository: AuthRepository) : ViewModel() {

  // Knowing whether a user is signed in takes no time. Startup work that does could hold the
  // system splash with setKeepOnScreenCondition.
  private val _uiState =
      MutableStateFlow(SplashUIState(isLoggedIn = authRepository.currentUser != null))
  val uiState: StateFlow<SplashUIState> = _uiState.asStateFlow()
}
