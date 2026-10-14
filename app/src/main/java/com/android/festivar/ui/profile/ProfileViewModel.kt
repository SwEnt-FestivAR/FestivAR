// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Represents the UI state of the Profile screen.
 *
 * @property user The signed in [User]. The object is here even if there is the [name] and [surname]
 *   properties because it contains the initial information, it will be updated only if the changes
 *   are saved.
 * @property name The name of the [User], contains the current value that is written, may contain
 *   the modified value that is not saved.
 * @property surname The surname of the [User], contains the current value that is written, may
 *   contain the modified value that is not saved.
 * @property errorMsg An error message to show when an operation fails and contains the exact error
 *   message.
 * @property isLoading Whether the [User] items is currently being fetched.
 * @property isSaving Whether the [User] item is being saved and updated with the new values.
 * @property signedOut Whether the [User] signs out, keeps the information to block any other
 *   operation while the [User] is being disconnected.
 *     @property isSigningOut Whether the [User] is currently in the process of signing out, it
 *       blocks any other operation while trying to disconnect the [User].
 */
data class ProfileUIState(
    val user: User? = null,
    val name: String = "",
    val surname: String = "",
    val errorMsg: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val signedOut: Boolean = false,
    val isSigningOut: Boolean = false,
) {
  /**
   * Whether the Save changes button can be pressed: the profile is loaded, no save is running, no
   * field is blank, and at least one field differs from the saved [user].
   *
   * The use of `.trim()` allows to delete the precedent and final spaces entered.
   */
  val canSave: Boolean
    get() {
      val original = user ?: return false
      val changed = name.trim() != original.name || surname.trim() != original.surname
      return !isSaving &&
          name.isNotBlank() &&
          surname.isNotBlank() &&
          changed &&
          !signedOut &&
          !isSigningOut
    }
}

/**
 * ViewModel of the Profile screen.
 *
 * Manages the UI state by fetching the [User] item through the [UsersRepository].
 *
 * @property repository The repository used to fetch the [User] item.
 * @property authRepository The repository used to fetch the [User] account or log him out.
 * @property uId The current [User] unique identifier.
 */
class ProfileViewModel(
    private val repository: UsersRepository, // = UsersRepositoryProvider.repository
    private val authRepository: AuthRepository,
    private val uId: String,
) : ViewModel() {

  private val _uiState = MutableStateFlow(ProfileUIState())
  val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

  /** Initialisation block that loads all data to be ready for display. */
  init {
    getUserInformation()
  }

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  /**
   * Sets the name typed in the Name field. The saved [ProfileUIState.user] is not changed. The
   * function is stopped/canceled if the user is signing out as he cannot modify anything anymore.
   *
   * @param name The name that is being typed.
   */
  fun updateName(name: String) {
    if (_uiState.value.signedOut || _uiState.value.isSigningOut) return
    _uiState.update { it.copy(name = name) }
  }

  /**
   * Sets the surname typed in the Surname field. The saved [ProfileUIState.user] is not changed.
   * The function is stopped/canceled if the user is signing out as he cannot modify anything
   * anymore.
   *
   * @param surname The surname that is being typed.
   */
  fun updateSurname(surname: String) {
    if (_uiState.value.signedOut || _uiState.value.isSigningOut) return
    _uiState.update { it.copy(surname = surname) }
  }

  /**
   * Function that saves the typed name and surname in the repository. Does nothing unless
   * [ProfileUIState.canSave], and checks [ProfileUIState.isSaving] so that a second tap will do
   * nothing until the save is completed or fails.
   */
  fun saveChanges() {
    // If the user is signing out, no further changes can be made.
    if (_uiState.value.signedOut || _uiState.value.isSigningOut) return

    val state = _uiState.value

    // If saving is disabled, quit the function.
    if (!state.canSave) return

    // If there is no actual user, then this piece of code is not reached due to previous
    // exit if (!state.canSave) return, in that case it exits also.
    val original = state.user ?: return

    // Built from the fields as they are now, so typing during the save does not change what is
    // saved. copy keeps the email, joined events and assigned tasks of the original user.
    val newUser =
        original.copy(
            name = state.name.trim(),
            surname = state.surname.trim(),
        )

    // Set the current state to saving.
    _uiState.update { it.copy(isSaving = true, errorMsg = null) }

    // Save the user modifications into the repository.
    viewModelScope.launch {
      try {

        repository.editUser(uId, newUser)
        _uiState.update { it.copy(user = newUser, isSaving = false) }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              isSaving = false,
              errorMsg = "Failed to update profile: ${e.message ?: "unknown error"}",
          )
        }
      }
    }
  }

  /**
   * Signs the user out. On success [ProfileUIState.signedOut] turns true, so the screen can go back
   * to the login screen; on failure [ProfileUIState.errorMsg] is set.
   */
  fun signOut() {
    // If the user is already signing out, it can't be run twice.
    if (_uiState.value.signedOut || _uiState.value.isSigningOut) return

    // Informs that the user is signing out.
    _uiState.update { it.copy(isSigningOut = true) }

    // Sign uot through the authentification repository.
    viewModelScope.launch {
      authRepository
          .signOut()
          .fold(
              onSuccess = { _uiState.update { it.copy(signedOut = true) } },
              onFailure = { e ->
                _uiState.update {
                  it.copy(
                      isSigningOut = false,
                      errorMsg = "Failed to sign out: ${e.message ?: "unknown error"}",
                  )
                }
              },
          )
    }
  }

  /** Function that fetches the [User] item from the repository and updates the UI state. */
  private fun getUserInformation() {
    // Sets the state to loading so that the user can't already try to write the in the fields
    // while the information are not already set.
    _uiState.update { it.copy(isLoading = true, errorMsg = null) }

    // Fetch the information from the repository.
    viewModelScope.launch {
      try {
        val user = repository.getUser(uId)
        _uiState.update {
          it.copy(
              user = user,
              name = user.name,
              surname = user.surname,
              isLoading = false,
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              isLoading = false,
              errorMsg = "Failed to load profile: ${e.message ?: "unknown error"}",
          )
        }
      }
    }
  }
}
