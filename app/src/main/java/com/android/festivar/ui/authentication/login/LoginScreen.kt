// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
//
package com.android.festivar.ui.authentication.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.festivar.R
import com.android.festivar.resources.GoogleLogo
import com.android.festivar.ui.components.FestivarTextField
import com.android.festivar.ui.theme.AppTheme

internal object LoginScreenTestTags {
  const val TITLE = "loginTitle"
  const val LOGO = "loginLogo"
  const val CATCHPHRASE = "loginCatchphrase"
  const val EMAIL_INPUT = "loginEmailInput"
  const val PASSWORD_INPUT = "loginPasswordInput"
  const val LOGIN_BUTTON = "loginButton"
  const val GOOGLE_BUTTON = "googleSignInButton"
  const val ERROR_MESSAGE = "loginError"

  const val CREATE_ACC_BUTTON = "createAccountButton"
}

/** Displays login fields and handles authentication events. */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(),
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit = {},
    onSignedIn: () -> Unit = {},
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(uiState.isSignedIn) { if (uiState.isSignedIn) onSignedIn() }
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { error ->
      snackbarHostState.showSnackbar(error)
      viewModel.clearError()
    }
  }

  Scaffold(
      modifier = modifier,
      snackbarHost = {
        SnackbarHost(snackbarHostState) { data ->
          Snackbar(
              modifier = Modifier.testTag(LoginScreenTestTags.ERROR_MESSAGE),
              action = {
                TextButton(onClick = { data.dismiss() }) {
                  Text(
                      text = stringResource(R.string.signup_snackbar_ok),
                      color = MaterialTheme.colorScheme.primary,
                  )
                }
              },
          ) {
            Text(data.visuals.message)
          }
        }
      },
  ) { padding ->
    LoginScreenContent(
        uiState = uiState,
        onEmailChange = viewModel::updateEmail,
        onPasswordChange = viewModel::updatePassword,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onLoginClick = viewModel::signIn,
        onGoogleSignInClick = { viewModel.signInWithGoogle(context, credentialManager) },
        onForgotPasswordClick = onForgotPasswordClick,
        onSignUpClick = onSignUpClick,
        modifier = Modifier.padding(padding),
    )
  }
}

@Composable
private fun LoginScreenContent(
    uiState: LoginUIState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLoginClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier =
          modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.surface)
              .padding(horizontal = 16.dp, vertical = 12.dp)
              .imePadding()
              .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Image(
          painter = painterResource(R.drawable.app_logo),
          contentDescription = null,
          modifier = Modifier.size(24.dp).testTag(LoginScreenTestTags.LOGO),
      )
      Text(
          text = stringResource(R.string.app_name),
          color = MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(start = 8.dp).testTag(LoginScreenTestTags.TITLE),
      )
    }

    Text(
        text = stringResource(R.string.login_catchphrase),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.displaySmall,
        modifier = Modifier.testTag(LoginScreenTestTags.CATCHPHRASE),
    )
    Text(
        text = stringResource(R.string.login_description),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
    )

    Spacer(modifier = Modifier.height(20.dp))

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

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
      TextButton(onClick = onForgotPasswordClick) {
        Text(
            text = stringResource(R.string.login_forgot_password),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelMedium,
        )
      }
    }

    Button(
        onClick = onLoginClick,
        enabled = !uiState.isLoading,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(0.dp),
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
      HorizontalDivider(
          modifier = Modifier.weight(1f),
          thickness = 1.dp,
          color = MaterialTheme.colorScheme.outlineVariant,
      )
      Text(
          text = stringResource(R.string.login_or),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(horizontal = 10.dp),
      )
      HorizontalDivider(
          modifier = Modifier.weight(1f),
          thickness = 1.dp,
          color = MaterialTheme.colorScheme.outlineVariant,
      )
    }

    GoogleSignInButton(onClick = onGoogleSignInClick, enabled = !uiState.isLoading)

    Spacer(modifier = Modifier.weight(1f))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
    ) {
      Text(
          text = stringResource(R.string.login_new_here),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall,
      )
      TextButton(
          onClick = onSignUpClick,
          modifier = Modifier.testTag(LoginScreenTestTags.CREATE_ACC_BUTTON),
      ) {
        Text(
            text = stringResource(R.string.signup_title),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelMedium,
        )
      }
    }
  }
}

@Composable
private fun GoogleSignInButton(onClick: () -> Unit, enabled: Boolean) {
  Button(
      onClick = onClick,
      enabled = enabled,
      shape = RoundedCornerShape(16.dp),
      contentPadding = PaddingValues(0.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.surface,
              contentColor = MaterialTheme.colorScheme.onSurface,
          ),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.fillMaxWidth().height(44.dp).testTag(LoginScreenTestTags.GOOGLE_BUTTON),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Image(imageVector = GoogleLogo, contentDescription = null, modifier = Modifier.size(18.dp))
      Text(
          text = stringResource(R.string.login_continue_with_google),
          color = MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.labelLarge,
          modifier = Modifier.padding(start = 8.dp),
      )
    }
  }
}

@Preview(showBackground = true, widthDp = 290, heightDp = 638)
@Composable
private fun LoginScreenPreview() {
  AppTheme(darkTheme = false) {
    LoginScreenContent(
        uiState = LoginUIState(),
        onEmailChange = {},
        onPasswordChange = {},
        onTogglePasswordVisibility = {},
        onLoginClick = {},
        onGoogleSignInClick = {},
        onForgotPasswordClick = {},
        onSignUpClick = {},
    )
  }
}
