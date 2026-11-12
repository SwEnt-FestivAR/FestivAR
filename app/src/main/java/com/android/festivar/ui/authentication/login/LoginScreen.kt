// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.authentication.login

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.android.festivar.R
import com.android.festivar.ui.components.CREDENTIAL_MAX_LENGTH
import com.android.festivar.ui.components.FestivarConfirmDialog
import com.android.festivar.ui.components.FestivarTextField
import com.android.festivar.ui.components.NoticeSnackbar
import com.android.festivar.ui.components.OrSeparator
import com.android.festivar.ui.theme.AppTheme

internal object LoginScreenTestTags {
  const val TITLE = "loginTitle"
  const val LOGO = "loginLogo"
  const val CATCHPHRASE = "loginCatchphrase"
  const val EMAIL_INPUT = "loginEmailInput"
  const val PASSWORD_INPUT = "loginPasswordInput"
  const val LOGIN_BUTTON = "loginButton"
  const val GOOGLE_BUTTON = "googleSignInButton"
  const val ANONYMOUS_BUTTON = "anonymousSignInButton"
  const val ERROR_MESSAGE = "loginError"

  const val CREATE_ACC_BUTTON = "createAccountButton"
}

/** Displays login fields and handles authentication events. */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit = {},
    onSignedIn: () -> Unit = {},
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  var confirmAnonPopup by rememberSaveable { mutableStateOf(false) }
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
          NoticeSnackbar(
              message = data.visuals.message,
              actionLabel = stringResource(R.string.signup_snackbar_ok),
              onAction = { data.dismiss() },
              modifier = Modifier.testTag(LoginScreenTestTags.ERROR_MESSAGE),
          )
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
        onAnonSignInClick = { confirmAnonPopup = true },
        onForgotPasswordClick = onForgotPasswordClick,
        onSignUpClick = onSignUpClick,
        modifier = Modifier.padding(padding),
    )

    if (confirmAnonPopup) {
      FestivarConfirmDialog(
          title = stringResource(R.string.login_warning),
          message = stringResource(R.string.login_warning_msg),
          confirmText = stringResource(R.string.action_continue_anyway),
          dismissText = stringResource(R.string.action_cancel),
          onConfirm = {
            viewModel.anonymousAuth()
            confirmAnonPopup = false
          },
          onDismiss = { confirmAnonPopup = false },
          destructive = false,
      )
    }
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
    onAnonSignInClick: () -> Unit,
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
      Icon(
          painter = painterResource(R.drawable.app_logo),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(40.dp).testTag(LoginScreenTestTags.LOGO),
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

    EmailLoginForm(
        email = uiState.email,
        password = uiState.password,
        isLoading = uiState.isLoading,
        isPasswordVisible = uiState.isPasswordVisible,
        onEmailChange = onEmailChange,
        onPasswordChange = onPasswordChange,
        onLoginClick = onLoginClick,
        onTogglePasswordVisibility = onTogglePasswordVisibility,
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
      TextButton(onClick = onForgotPasswordClick) {
        Text(
            text = stringResource(R.string.login_forgot_password),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelSmall,
        )
      }
    }

    OrSeparator()

    // Google
    SignInButton(
        onClick = onGoogleSignInClick,
        enabled = !uiState.isLoading,
        showGoogle = true,
        text = stringResource(R.string.login_continue_with_google),
        modifier = Modifier.testTag(LoginScreenTestTags.GOOGLE_BUTTON),
    )

    // Anonymous
    SignInButton(
        onClick = onAnonSignInClick,
        enabled = !uiState.isLoading,
        showGoogle = false,
        text = stringResource(R.string.login_no_account),
        modifier = Modifier.testTag(LoginScreenTestTags.ANONYMOUS_BUTTON),
    )

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
fun EmailLoginForm(
    email: String,
    password: String,
    isLoading: Boolean,
    isPasswordVisible: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = stringResource(R.string.signup_sign_in),
) {
  Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    FestivarTextField(
        value = email,
        onValueChange = onEmailChange,
        label = stringResource(R.string.signup_email),
        maxLength = CREDENTIAL_MAX_LENGTH,
        modifier = Modifier.fillMaxWidth().testTag(LoginScreenTestTags.EMAIL_INPUT),
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
    )
    FestivarTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = stringResource(R.string.signup_password),
        maxLength = CREDENTIAL_MAX_LENGTH,
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
            if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
          IconButton(onClick = onTogglePasswordVisibility) {
            Icon(
                imageVector =
                    if (isPasswordVisible) Icons.Default.Visibility
                    else Icons.Default.VisibilityOff,
                contentDescription =
                    if (isPasswordVisible) stringResource(R.string.signup_hide_password)
                    else stringResource(R.string.signup_show_password),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
    )

    Button(
        onClick = onLoginClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth().height(44.dp).testTag(LoginScreenTestTags.LOGIN_BUTTON),
    ) {
      if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp,
        )
      } else {
        Text(
            text = buttonText,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}

@Composable
fun SignInButton(
    onClick: () -> Unit,
    enabled: Boolean,
    showGoogle: Boolean,
    text: String,
    modifier: Modifier = Modifier,
) {
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
      modifier = modifier.fillMaxWidth().height(44.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (showGoogle) {
        Icon(
            painter = painterResource(id = R.drawable.ic_google_logo),
            contentDescription = "Google Logo",
            tint = Color.Unspecified,
        )
      }
      Text(
          text = text,
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
    ) {}
  }
}
