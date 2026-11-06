package com.android.festivar.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.festivar.R
import com.android.festivar.model.user.User
import com.android.festivar.ui.authentication.login.LoginScreenTestTags
import com.android.festivar.ui.components.FestivarTextField
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
        uiState = uiState,
        onEmailChange = onEmailChange,
        onPasswordChange = onPasswordChange,
        onLoginClick = onLoginClick,
        onTogglePasswordVisibility = onTogglePasswordVisibility,
    )
  }
}

@Composable
fun EmailLoginForm(
    uiState: ProfileUIState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
) {
  Column(
      modifier =
          Modifier.background(MaterialTheme.colorScheme.surface)
              .padding(horizontal = 12.dp, vertical = 12.dp)
              .imePadding()
              .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    FestivarTextField(
        value = uiState.email,
        onValueChange = onEmailChange,
        label = stringResource(R.string.signup_email),
        modifier = Modifier.fillMaxWidth().testTag(LoginScreenTestTags.EMAIL_INPUT),
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
    )
    FestivarTextField(
        value = uiState.password,
        onValueChange = onPasswordChange,
        label = stringResource(R.string.signup_password),
        modifier = Modifier.fillMaxWidth().testTag(LoginScreenTestTags.PASSWORD_INPUT),
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
        keyboardActions = KeyboardActions(onDone = { onLoginClick() }),
        singleLine = true,
        visualTransformation =
            if (uiState.isPasswordVisible) {
              VisualTransformation.None
            } else {
              PasswordVisualTransformation()
            },
        trailingIcon = {
          IconButton(onClick = onTogglePasswordVisibility) {
            Icon(
                imageVector =
                    if (uiState.isPasswordVisible) Icons.Default.Visibility
                    else Icons.Default.VisibilityOff,
                contentDescription =
                    if (uiState.isPasswordVisible) stringResource(R.string.signup_hide_password)
                    else stringResource(R.string.signup_show_password),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
    )

    Button(
        onClick = onLoginClick,
        enabled = !uiState.isLoading,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth().height(44.dp).testTag(LoginScreenTestTags.LOGIN_BUTTON),
    ) {
      if (uiState.isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
        )
      } else {
        Text(
            text = stringResource(R.string.signup_sign_in),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
        )
      }
    }
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
