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


data class ProfileUIState(
    val user: User? = null,
    val name: String = "",
    val surname: String = "",
    val errorMsg: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val signedOut: Boolean = false,
) {
    /**
     * Whether the Save changes button can be pressed: the profile is loaded, no save is running,
     * no field is blank, and at least one field differs from the saved [user].
     */
    val canSave: Boolean
        get() {
            val original = user ?: return false
            val changed =
                name.trim() != original.name ||
                    surname.trim() != original.surname
            return !isSaving &&
                name.isNotBlank() &&
                surname.isNotBlank() &&
                changed
        }
}

class ProfileViewModel (
    private val repository: UsersRepository, // = UsersRepositoryProvider.repository
    private val authRepository: AuthRepository,
    private val uId: String
    ): ViewModel(){

    private val _uiState = MutableStateFlow(ProfileUIState())
    val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()


    init {
        getUserInformation()
    }

    fun clearErrorMsg() {
        _uiState.update { it.copy(errorMsg = null) }
    }

    /** Sets the name typed in the Name field. The saved [ProfileUIState.user] is not changed. */
    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateSurname(surname: String) {
        _uiState.update { it.copy(surname = surname) }
    }


    /**
     * Saves the typed name and surname in the repository; the email is not editable and is kept.
     * Does nothing unless [ProfileUIState.canSave], so a second tap during a save is ignored.
     */
    fun saveChanges() {
        val state = _uiState.value
        if (!state.canSave) return
        val original = state.user ?: return

        // Built from the fields as they are now, so typing during the save does not change what is
        // saved. copy keeps the email, joined events and assigned tasks of the user.

        val newUser = original.copy(
                name = state.name.trim(),
                surname = state.surname.trim()
            )

        _uiState.update { it.copy(isSaving = true, errorMsg = null) }

        viewModelScope.launch {
            try {
                repository.editUser(uId, newUser)
                // The saved user is the new original, so canSave turns false until the next edit.
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
        viewModelScope.launch {
            authRepository
                .signOut()
                .fold(
                    onSuccess = { _uiState.update { it.copy(signedOut = true) } },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(errorMsg = "Failed to sign out: ${e.message ?: "unknown error"}")
                        }
                    },
                )
        }
    }

    private fun getUserInformation(){
        _uiState.update { it.copy(isLoading = true, errorMsg = null) }
        viewModelScope.launch {
            try {
                val user = repository.getUser(uId)
                _uiState.update {
                    it.copy(
                        user = user,
                        name = user.name,
                        surname = user.surname,
                        isLoading = false
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