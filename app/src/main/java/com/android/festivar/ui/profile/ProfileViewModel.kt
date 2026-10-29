// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.profile

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.festivar.R
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.user.User
import com.android.festivar.model.user.UsersRepository
import com.android.festivar.model.user.UsersRepositoryProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val LOG_TAG = "ProfileViewModel"

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
 * @property errorMsg The string resource of the last failure, or null.
 * @property isLoading Whether the [User] item is currently being fetched.
 * @property isSaving Whether the [User] item is being saved and updated with the new values.
 * @property signedOut Whether the [User] signs out, keeps the information to block any other
 *   operation when the disconnection is occured.
 * @property isSigningOut Whether the [User] is currently in the process of signing out, it blocks
 *   any other operation while trying to disconnect the [User].
 */
data class ProfileUIState(
    val user: User? = null,
    val name: String = "",
    val surname: String = "",
    @StringRes val errorMsg: Int? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val signedOut: Boolean = false,
    val isSigningOut: Boolean = false,
) {
  /**
   * Whether the Save changes button can be pressed: the profile is loaded, no save is running, no
   * field is blank, and at least one field differs from the saved [user], the surname and name are
   * valid.
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
          !isSigningOut &&
          !nameInvalid &&
          !surnameInvalid
    }

  val nameInvalid: Boolean
    get() = !isValidNameInput(name)

  val surnameInvalid: Boolean
    get() = !isValidNameInput(surname)

  /**
   * Utility function that checks whether the given input is plain text and doesn't contain emojis,
   * numbers, special characters and not wanted punctuation (at the exception of ` `, `-`, `’` and
   * `'`) and at least one letter is necessary. Those restrictions are the ones that determine if a
   * name is valid or not.
   *
   * @param input The input [String] we want to verify.
   * @return True if the given [input] is plain text with some punctuation allowed, false otherwise.
   */
  private fun isValidNameInput(input: String): Boolean {
    return input.all { it.isLetter() || it == ' ' || it == '-' || it == '\'' || it == '’' } &&
        input.any { it.isLetter() }
  }
}

/**
 * ViewModel of the Profile screen.
 *
 * Manages the UI state by fetching the [User] item through the [UsersRepository].
 *
 * @property repository The repository used to fetch the [User] item.
 * @property authRepository The repository used to fetch the [User] account or sign him out.
 * @property uId The current [User] unique identifier.
 */
class ProfileViewModel(
    private val repository: UsersRepository = UsersRepositoryProvider.repository,
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
   * function is stopped/canceled if the user is signing out as he cannot modify anything anymore or
   * if the screen is still loading.
   *
   * @param name The name that is being typed.
   */
  fun updateName(name: String) {
    if (_uiState.value.signedOut || _uiState.value.isSigningOut || uiState.value.isLoading) return
    _uiState.update { it.copy(name = name) }
  }

  /**
   * Sets the surname typed in the Surname field. The saved [ProfileUIState.user] is not changed.
   * The function is stopped/canceled if the user is signing out as he cannot modify anything
   * anymore or if the screen is still loading.
   *
   * @param surname The surname that is being typed.
   */
  fun updateSurname(surname: String) {
    if (_uiState.value.signedOut || _uiState.value.isSigningOut || uiState.value.isLoading) return
    _uiState.update { it.copy(surname = surname) }
  }

  /**
   * Function that saves the typed name and surname in the repository. Does nothing unless
   * [ProfileUIState.canSave], and checks [ProfileUIState.isSaving] so that a second tap will do
   * nothing until the save is completed or fails.
   */
  fun saveChanges() {
    // Does nothing unless the changes can be saved and the profile is loaded.
    if (!_uiState.value.canSave || _uiState.value.isLoading) return

    val state = _uiState.value

    val original = requireNotNull(state.user)

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
        Log.e(LOG_TAG, "Error saving user $uId", e)
        _uiState.update { it.copy(isSaving = false, errorMsg = R.string.profile_save_failed) }
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

    // Sign uot through the authentication repository.
    viewModelScope.launch {
      authRepository
          .signOut()
          .fold(
              onSuccess = { _uiState.update { it.copy(signedOut = true, isSigningOut = false) } },
              onFailure = { e ->
                Log.e(LOG_TAG, "Error signing out", e)
                _uiState.update {
                  it.copy(isSigningOut = false, errorMsg = R.string.profile_sign_out_failed)
                }
              },
          )
    }
  }

  /** Function that fetches the [User] item from the repository and updates the UI state. */
  fun getUserInformation() {
    if (_uiState.value.isLoading) return

    // Sets the state to loading so that the user can't already try to write in the fields
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
        Log.e(LOG_TAG, "Error loading user $uId", e)
        _uiState.update { it.copy(isLoading = false, errorMsg = R.string.profile_load_failed) }
      }
    }
  }
}
