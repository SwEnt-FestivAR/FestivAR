// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.user.User
import com.android.festivar.ui.components.Avatar
import com.android.festivar.ui.components.FestivarTextField

/** Tags that the UI tests use to find the elements of the profile screen. */
object ProfileScreenTestTags {
  const val TITLE = "profileTitle"
  const val BACK = "profileBack"
  const val LOADING = "profileLoading"
  const val AVATAR = "profileAvatar"
  const val SURNAME_FIELD = "profileSurnameField"
  const val NAME_FIELD = "profileNameField"
  const val EMAIL = "profileEmail"
  const val SAVE_BUTTON = "profileSaveButton"
  const val SIGN_OUT_BUTTON = "profileSignOutButton"
  const val RETRY_BUTTON = "profileRetryButton"
  const val ERROR_MESSAGE = "profileErrorMessage"
}

/**
 * The profile screen, connected to its [profileViewModel]: it reads the UI state, shows the errors
 * and reports the navigation to the caller.
 *
 * The screen is principally composed of:
 * - a back arrow;
 * - the Profile title;
 * - the avatar of the [User];
 * - the email of the [User], which cannot be changed;
 * - a field that shows the surname and can be changed;
 * - a field that shows the name and can be changed;
 * - a button to save the changes typed in the fields;
 * - a button to sign out.
 *
 * @param profileViewModel Holds the profile shown and edited by the screen.
 * @param onBack Called when the user clicks on the back arrow of the top bar.
 * @param onSignedOut Called once the user is signed out, so the caller can go to the login screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    onBack: () -> Unit = {},
    onSignedOut: () -> Unit = {},
) {

  val profileUIState by profileViewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  // Shows each error once in the snackbar, then clears it so it does not come back.
  val errorMsg = profileUIState.errorMsg?.let { stringResource(it) }
  LaunchedEffect(errorMsg) {
    errorMsg?.let { error ->
      snackbarHostState.showSnackbar(error)
      profileViewModel.clearErrorMsg()
    }
  }

  // Leaves the screen once the sign out succeeded.
  LaunchedEffect(profileUIState.signedOut) { if (profileUIState.signedOut) onSignedOut() }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = stringResource(R.string.profile_title),
                  style = MaterialTheme.typography.titleLarge,
                  modifier = Modifier.testTag(ProfileScreenTestTags.TITLE),
              )
            },
            navigationIcon = {
              IconButton(onClick = onBack, Modifier.testTag(ProfileScreenTestTags.BACK)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.profile_back),
                )
              }
            },
        )
      },
      snackbarHost = {
        SnackbarHost(snackbarHostState) { data ->
          Snackbar(modifier = Modifier.testTag(ProfileScreenTestTags.ERROR_MESSAGE)) {
            Text(data.visuals.message)
          }
        }
      },
  ) { padding ->
    ProfileScreenContent(
        uiState = profileUIState,
        onSurnameChange = profileViewModel::updateSurname,
        onNameChange = profileViewModel::updateName,
        onSaveClick = profileViewModel::saveChanges,
        onSignOutClick = profileViewModel::signOut,
        onRetryClick = profileViewModel::getUserInformation,
        modifier = Modifier.padding(padding),
    )
  }
}

/**
 * What the profile screen draws for a given [uiState], without any ViewModel, so that previews and
 * tests can show any state. It covers the body of the screen, not the top bar with the back arrow
 * and the Profile title.
 *
 * @param uiState The state of the UI, taken from the ViewModel.
 * @param onSurnameChange Called with the new surname when the user types in the Surname field.
 * @param onNameChange Called with the new name when the user types in the Name field.
 * @param onSaveClick Called when the user clicks on the Save changes button.
 * @param onSignOutClick Called when the user clicks on the Sign out button.
 * @param modifier The [Modifier] applied to the column that holds the body of the screen.
 */
@Composable
internal fun ProfileScreenContent(
    uiState: ProfileUIState,
    onSurnameChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier = modifier.fillMaxSize().imePadding().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Until the profile is loaded there is nothing to show or edit.
    if (uiState.isLoading) {
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.testTag(ProfileScreenTestTags.LOADING))
      }
      return@Column
    }

    // The load failed: there is nothing to edit, so only offer to try again or to sign out.
    if (uiState.user == null) {
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        Button(
            onClick = onRetryClick,
            modifier = Modifier.testTag(ProfileScreenTestTags.RETRY_BUTTON),
        ) {
          Text(text = stringResource(R.string.profile_retry))
        }
      }
      SignOutButton(
          onClick = onSignOutClick,
          isSigningOut = uiState.isSigningOut,
          signedOut = uiState.signedOut,
      )
      return@Column
    }

    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      // The avatar shows the saved user, so its initials only change once the changes are saved.
      // The email is shown under it and cannot be edited.
      uiState.user.let { user ->
        Box(modifier = Modifier.testTag(ProfileScreenTestTags.AVATAR)) { Avatar(user = user) }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(ProfileScreenTestTags.EMAIL),
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      FestivarTextField(
          value = uiState.surname,
          onValueChange = onSurnameChange,
          label = stringResource(R.string.profile_field_surname),
          modifier = Modifier.testTag(ProfileScreenTestTags.SURNAME_FIELD),
      )
      Spacer(modifier = Modifier.height(12.dp))
      FestivarTextField(
          value = uiState.name,
          onValueChange = onNameChange,
          label = stringResource(R.string.profile_field_name),
          modifier = Modifier.testTag(ProfileScreenTestTags.NAME_FIELD),
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onSaveClick,
        enabled = uiState.canSave,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag(ProfileScreenTestTags.SAVE_BUTTON),
    ) {
      if (uiState.isSaving) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
      } else {
        Text(
            text = stringResource(R.string.profile_save),
            style = MaterialTheme.typography.labelLarge,
        )
      }
    }
    Spacer(modifier = Modifier.height(12.dp))

    SignOutButton(
        onClick = onSignOutClick,
        isSigningOut = uiState.isSigningOut,
        signedOut = uiState.signedOut,
    )
  }
}

@Composable
private fun SignOutButton(onClick: () -> Unit, isSigningOut: Boolean, signedOut: Boolean) {
  OutlinedButton(
      onClick = onClick,
      enabled = !isSigningOut && !signedOut,
      shape = RoundedCornerShape(12.dp),
      modifier =
          Modifier.fillMaxWidth().height(48.dp).testTag(ProfileScreenTestTags.SIGN_OUT_BUTTON),
      colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
  ) {
    if (isSigningOut) {
      CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
    } else {
      Text(text = stringResource(R.string.profile_sign_out))
    }
  }
}
