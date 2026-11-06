package com.android.festivar.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.model.user.User
import com.android.festivar.ui.authentication.login.EmailLoginForm
import com.android.festivar.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun UpgradeBottomSheet(
    onUpgradeCancel: () -> Unit,
    scope: CoroutineScope,
    sheetState: SheetState,
    uiState: ProfileUIState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
) {
  ModalBottomSheet(
      onDismissRequest = onUpgradeCancel,
      sheetState = sheetState,
  ) {
    UpgradeBottomSheetContent(
        onUpgradeCancel = {
          scope
              .launch { sheetState.hide() }
              .invokeOnCompletion {
                if (!sheetState.isVisible) {
                  onUpgradeCancel()
                }
              }
        },
        uiState = uiState,
        onEmailChange = onEmailChange,
        onPasswordChange = onPasswordChange,
        onLoginClick = onLoginClick,
        onTogglePasswordVisibility = onTogglePasswordVisibility,
    )
  }
}

@Composable
fun UpgradeBottomSheetContent(
    onUpgradeCancel: () -> Unit,
    uiState: ProfileUIState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
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
                .imePadding()
                .verticalScroll(rememberScrollState()),
    )
  }
}

@Preview(showBackground = true)
@Composable
fun UpgradeBottomSheetPreview() {
  AppTheme {
    Surface {
      UpgradeBottomSheetContent(
          onUpgradeCancel = {},
          uiState =
              ProfileUIState(
                  user =
                      User(
                          uid = "u1",
                          name = "skibidi",
                          surname = "toilet",
                          email = "skibidi@toilet.ch",
                      ),
                  name = "skibidi",
                  surname = "toilet",
                  isUserAnonymous = true,
              ),
          onEmailChange = {},
          onPasswordChange = {},
          onLoginClick = {},
          onTogglePasswordVisibility = {},
      )
    }
  }
}
