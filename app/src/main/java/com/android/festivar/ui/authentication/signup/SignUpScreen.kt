// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.authentication.signup

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.Credential
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.festivar.R
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.ui.components.FestivarTextField
import com.android.festivar.ui.theme.AppTheme
import com.android.festivar.ui.theme.onSurfaceMutedDark
import com.android.festivar.ui.theme.onSurfaceMutedLight
import com.google.firebase.auth.FirebaseUser

object SignUpScreenTestTags {
  const val ERROR_MESSAGE: String = "errorMessage"
  const val CREATE_ACC_TITLE = "createAccTitle"
  const val EMAIL_FIELD = "emailField"
  const val PASS_FIELD = "passField"
  const val PASS_CONFIRM_FIELD = "passConfirmField"
  const val PASSWORD_MISMATCH_ERROR = "passwordMismatchError"
  const val SIGNUP_BUTTON = "signUpButton"
  const val LOGIN_NOW_BUTTON = "loginNowButton"
}

@Composable
fun SignUpScreen(
    modifier: Modifier = Modifier,
    onSignInClick: () -> Unit = {},
    viewModel: SignUpViewModel = viewModel(),
    onSignUpSuccess: () -> Unit = {},
) {
  var isPasswordVisible by remember { mutableStateOf(false) }
  val snackBarHostState = remember { SnackbarHostState() }

  val uiState by viewModel.uiState.collectAsState()

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { error ->
      snackBarHostState.showSnackbar(error)
      viewModel.clearError()
    }
  }

  LaunchedEffect(uiState.isAuthenticated) { if (uiState.isAuthenticated) onSignUpSuccess() }
  Scaffold(
      modifier = modifier,
      bottomBar = { SignUpFooter(onSignInClick) },
      snackbarHost = {
        SnackbarHost(
            hostState = snackBarHostState,
        ) { data ->
          SignUpErrorSnackbar(data.visuals.message) { data.dismiss() }
        }
      },
  ) { paddingValues ->
    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
    ) {
      SignUpTopBar(onSignInClick)
      Spacer(modifier = Modifier.height(16.dp))
      SignUpHeader()
      Spacer(modifier = Modifier.height(24.dp))
      SignUpFields(
          email = uiState.email,
          onEmailChange = { viewModel.updateEmail(it) },
          password = uiState.password,
          onPasswordChange = { viewModel.updatePassword(it) },
          confirmPassword = uiState.confirmPassword,
          onConfirmPasswordChange = { viewModel.updateConfirmPassword(it) },
          isPasswordVisible = isPasswordVisible,
          onPasswordVisibilityChange = { isPasswordVisible = !isPasswordVisible },
      )
      uiState.errorMsg?.let { error ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
      }
      Spacer(modifier = Modifier.height(20.dp))
      SignUpButton(
          enabled = uiState.canSubmit,
          isLoading = uiState.isLoading,
          onClick = { viewModel.signUp(uiState.email, uiState.password) },
      )
      Spacer(modifier = Modifier.height(24.dp))
      SignUpTerms()
      Spacer(modifier = Modifier.height(32.dp))
    }
  }
}

@Composable
private fun SignUpErrorSnackbar(
    message: String,
    onDismiss: () -> Unit,
) {
  Snackbar(
      modifier = Modifier.testTag(SignUpScreenTestTags.ERROR_MESSAGE),
      action = {
        Box(contentAlignment = Alignment.Center) {
          TextButton(onClick = onDismiss) {
            Text(
                text = stringResource(R.string.signup_snackbar_ok),
                color = MaterialTheme.colorScheme.inversePrimary,
            )
          }
        }
      },
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
          painter = painterResource(R.drawable.ic_pennant),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.inverseOnSurface,
      )
      Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}

@Composable
private fun SignUpTopBar(onBackClick: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    IconButton(onClick = onBackClick) {
      Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.signup_back),
          tint = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
private fun SignUpHeader() {
  Column {
    Text(
        text = stringResource(R.string.signup_title),
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag(SignUpScreenTestTags.CREATE_ACC_TITLE),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.signup_description),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun SignUpFields(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onPasswordVisibilityChange: () -> Unit,
) {
  Column {
    SignUpTextField(
        value = email,
        onValueChange = onEmailChange,
        label = stringResource(R.string.signup_email),
        tag = SignUpScreenTestTags.EMAIL_FIELD,
        keyboardType = KeyboardType.Email,
    )
    Spacer(modifier = Modifier.height(12.dp))
    PasswordTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = stringResource(R.string.signup_password),
        tag = SignUpScreenTestTags.PASS_FIELD,
        isPasswordVisible = isPasswordVisible,
        onPasswordVisibilityChange = onPasswordVisibilityChange,
    )
    Spacer(modifier = Modifier.height(12.dp))
    PasswordTextField(
        value = confirmPassword,
        onValueChange = onConfirmPasswordChange,
        label = stringResource(R.string.signup_confirm_password),
        tag = SignUpScreenTestTags.PASS_CONFIRM_FIELD,
        isPasswordVisible = isPasswordVisible,
        onPasswordVisibilityChange = onPasswordVisibilityChange,
        showPasswordDescription = stringResource(R.string.signup_show_confirmation_password),
        hidePasswordDescription = stringResource(R.string.signup_hide_confirmation_password),
    )
    Text(
        text = stringResource(R.string.signup_password_hint),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
    )
    if (confirmPassword.isNotEmpty() && password != confirmPassword) {
      Text(
          text = stringResource(R.string.signup_passwords_do_not_match),
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier =
              Modifier.padding(top = 4.dp).testTag(SignUpScreenTestTags.PASSWORD_MISMATCH_ERROR),
      )
    }
  }
}

@Composable
private fun SignUpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
  FestivarTextField(
      value = value,
      onValueChange = onValueChange,
      label = label,
      modifier = Modifier.testTag(tag),
      keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
  )
}

@Composable
private fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    tag: String,
    isPasswordVisible: Boolean,
    onPasswordVisibilityChange: (() -> Unit)? = null,
    showPasswordDescription: String = stringResource(R.string.signup_show_password),
    hidePasswordDescription: String = stringResource(R.string.signup_hide_password),
) {
  FestivarTextField(
      value = value,
      onValueChange = onValueChange,
      label = label,
      modifier = Modifier.testTag(tag),
      visualTransformation =
          if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
      trailingIcon =
          onPasswordVisibilityChange?.let { onToggle ->
            {
              IconButton(onClick = onToggle) {
                Icon(
                    imageVector =
                        if (isPasswordVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                    contentDescription =
                        if (isPasswordVisible) hidePasswordDescription else showPasswordDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          },
  )
}

@Composable
private fun SignUpButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
  Button(
      onClick = onClick,
      modifier = Modifier.fillMaxWidth().height(52.dp).testTag(SignUpScreenTestTags.SIGNUP_BUTTON),
      enabled = enabled,
      shape = RoundedCornerShape(16.dp),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
              disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
          ),
  ) {
    Text(
        text =
            if (isLoading) stringResource(R.string.signup_signing_up)
            else stringResource(R.string.signup_create_account),
        style = MaterialTheme.typography.labelLarge,
    )
  }
}

@Composable
private fun SignUpTerms() {
  Text(
      text = stringResource(R.string.signup_terms),
      style = MaterialTheme.typography.bodySmall,
      color = if (isSystemInDarkTheme()) onSurfaceMutedDark else onSurfaceMutedLight,
  )
}

@Composable
private fun SignUpFooter(onSignInClick: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
        text = stringResource(R.string.signup_already_have_account),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.width(4.dp))
    TextButton(
        onClick = onSignInClick,
        modifier = Modifier.testTag(SignUpScreenTestTags.LOGIN_NOW_BUTTON),
    ) {
      Text(
          text = stringResource(R.string.signup_sign_in),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }
  }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
  AppTheme {
    SignUpScreen(
        modifier = Modifier.fillMaxSize(),
        onSignInClick = {},
        viewModel = SignUpViewModel(PreviewAuthRepository),
        onSignUpSuccess = {},
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun SignUpErrorSnackbarPreview() {
  AppTheme {
    SignUpErrorSnackbar(
        message = stringResource(R.string.signup_preview_error),
        onDismiss = {},
    )
  }
}

private object PreviewAuthRepository : AuthRepository {
  const val STR: String = "Preview only"

  override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> =
      Result.failure(UnsupportedOperationException(STR))

  override suspend fun signOut(): Result<Unit> = Result.failure(UnsupportedOperationException(STR))

  override suspend fun signInWithEmailAndPassword(
      email: String,
      password: String,
  ): Result<FirebaseUser> = Result.failure(UnsupportedOperationException(STR))

  override suspend fun signUpWithEmailAndPassword(
      email: String,
      password: String,
  ): Result<FirebaseUser> = Result.failure(UnsupportedOperationException(STR))
}
