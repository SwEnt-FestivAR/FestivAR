// Co-authored by Copilot
package com.android.festivar.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.ui.authentication.login.EmailLoginForm
import com.android.festivar.ui.authentication.login.LoginScreenTestTags
import com.android.festivar.ui.authentication.login.SignInButton
import com.android.festivar.ui.components.NoticeSnackbar
import com.android.festivar.ui.components.OrSeparator

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun UpgradeBottomSheet(
    onUpgradeCancel: () -> Unit,
    sheetState: SheetState,
    uiState: ProfileUIState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onGoogleUpgrade: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
  ModalBottomSheet(
      onDismissRequest = onUpgradeCancel,
      sheetState = sheetState,
      modifier = Modifier.testTag(ProfileScreenTestTags.UPGRADE_SHEET),
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      UpgradeBottomSheetContent(
          uiState = uiState,
          onEmailChange = onEmailChange,
          onPasswordChange = onPasswordChange,
          onLoginClick = onLoginClick,
          onTogglePasswordVisibility = onTogglePasswordVisibility,
          onGoogleUpgrade = onGoogleUpgrade,
      )
      SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
      ) { data ->
        NoticeSnackbar(message = data.visuals.message, actionLabel = "", onAction = {})
      }
    }
  }
}

@Composable
fun UpgradeBottomSheetContent(
    uiState: ProfileUIState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onGoogleUpgrade: () -> Unit,
) {
  Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    EmailLoginForm(
        email = uiState.email,
        password = uiState.password,
        isLoading = uiState.isLoading,
        isPasswordVisible = uiState.isPasswordVisible,
        onEmailChange = onEmailChange,
        onPasswordChange = onPasswordChange,
        onLoginClick = onLoginClick,
        onTogglePasswordVisibility = onTogglePasswordVisibility,
        modifier =
            Modifier.background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
        buttonText = stringResource(R.string.profile_upgrade),
    )

    OrSeparator(Modifier.padding(horizontal = 12.dp))

    SignInButton(
        onClick = onGoogleUpgrade,
        enabled = !uiState.isLoading,
        showGoogle = true,
        text = stringResource(R.string.login_continue_with_google),
        modifier =
            Modifier.testTag(LoginScreenTestTags.GOOGLE_BUTTON)
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
    )
  }
}
