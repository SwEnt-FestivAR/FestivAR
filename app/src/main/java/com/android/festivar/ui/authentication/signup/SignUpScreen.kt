// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.ui.authentication.signup

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.resources.GoogleLogo
import com.android.festivar.ui.theme.AppTheme
import com.google.firebase.auth.FirebaseUser

object SignUpScreenTestTags {
  const val CREATE_ACC_TITLE = "createAccTitle"
  const val EMAIL_FIELD = "emailField"
  const val PASS_FIELD = "passField"
  const val PASS_CONFIRM_FIELD = "passConfirmField"
  const val PASSWORD_MISMATCH_ERROR = "passwordMismatchError"
  const val SIGNUP_BUTTON = "signUpButton"
  const val GOOGLE_SIGNUP_BUTTON = "googleSignUpButton"
  const val LOGIN_NOW_BUTTON = "loginNowButton"
}

@Composable
fun SignUpScreen(
    modifier: Modifier = Modifier,
    credentialManager: CredentialManager? = CredentialManager.create(LocalContext.current),
    onSignInClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    viewModel: SignUpViewModel = SignUpViewModel(),
    onSignUpSuccess: () -> Unit = {},
) {
  var isPasswordVisible by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()

  if (uiState.isAuthenticated) {
    onSignUpSuccess()
  }
  Column(
      modifier =
          modifier
              .fillMaxSize()
              .background(color = MaterialTheme.colorScheme.background)
              .padding(horizontal = 24.dp)
              .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.Start,
  ) {
    SignUpTopBar(onBackClick)
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
    Spacer(modifier = Modifier.height(20.dp))
    SignUpButton(
        enabled =
            uiState.email.isNotBlank() &&
                uiState.password.isNotBlank() &&
                uiState.confirmPassword.isNotBlank() &&
                uiState.password == uiState.confirmPassword,
        isLoading = uiState.isLoading,
        onClick = { viewModel.signUp(uiState.email, uiState.password) },
    )
    Spacer(modifier = Modifier.height(24.dp))
    SignUpDivider()
    Spacer(modifier = Modifier.height(24.dp))
    GoogleSignUpButton(
        onClick = { credentialManager?.let { viewModel.googleSignUp(context, it) } },
    )
    Spacer(modifier = Modifier.height(24.dp))
    SignUpTerms()
    Spacer(modifier = Modifier.height(32.dp))
    SignUpFooter(onSignInClick)
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
          contentDescription = "Back",
          tint = MaterialTheme.colorScheme.onSurface,
      )
    }
    IconButton(onClick = {}) {
      Icon(
          imageVector = Icons.Default.MoreVert,
          contentDescription = "More Options",
          tint = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
private fun SignUpHeader() {
  Column {
    Text(
        text = "Create an account",
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag(SignUpScreenTestTags.CREATE_ACC_TITLE),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Your organizer adds you to an event by code once you are in.",
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
        label = "Email",
        tag = SignUpScreenTestTags.EMAIL_FIELD,
        keyboardType = KeyboardType.Email,
    )
    Spacer(modifier = Modifier.height(12.dp))
    PasswordTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = "Password",
        tag = SignUpScreenTestTags.PASS_FIELD,
        isPasswordVisible = isPasswordVisible,
        onPasswordVisibilityChange = onPasswordVisibilityChange,
    )
    Spacer(modifier = Modifier.height(12.dp))
    PasswordTextField(
        value = confirmPassword,
        onValueChange = onConfirmPasswordChange,
        label = "Confirm password",
        tag = SignUpScreenTestTags.PASS_CONFIRM_FIELD,
        isPasswordVisible = isPasswordVisible,
    )
    if (confirmPassword.isNotEmpty() && password != confirmPassword) {
      Text(
          text = "Passwords do not match",
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier =
              Modifier.padding(top = 4.dp).testTag(SignUpScreenTestTags.PASSWORD_MISMATCH_ERROR),
      )
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "8 characters or more.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp),
    )
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
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = Modifier.fillMaxWidth().testTag(tag),
      label = { Text(label) },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
      shape = RoundedCornerShape(12.dp),
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
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = Modifier.fillMaxWidth().testTag(tag),
      label = { Text(label) },
      singleLine = true,
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
                        if (isPasswordVisible) "Hide password" else "Show password",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          },
      shape = RoundedCornerShape(12.dp),
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
        text = if (isLoading) "Signing up..." else "Create account",
        style = MaterialTheme.typography.labelLarge,
    )
  }
}

@Composable
private fun SignUpDivider() {
  Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
  ) {
    HorizontalDivider(
        modifier = Modifier.weight(1f),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
    Text(
        text = "or",
        modifier = Modifier.padding(horizontal = 12.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
    HorizontalDivider(
        modifier = Modifier.weight(1f),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
  }
}

@Composable
private fun GoogleSignUpButton(onClick: () -> Unit) {
  OutlinedButton(
      onClick = onClick,
      modifier =
          Modifier.fillMaxWidth().height(52.dp).testTag(SignUpScreenTestTags.GOOGLE_SIGNUP_BUTTON),
      shape = RoundedCornerShape(16.dp),
  ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
      Image(
          imageVector = GoogleLogo,
          contentDescription = "Google Logo",
          modifier = Modifier.size(24.dp),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Text(
          text = "Continue with Google",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
private fun SignUpTerms() {
  Text(
      text =
          "By continuing you accept the terms and the privacy notice. Your phone number, if you add one later, is only shown to organizers.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        text = "Already have an account?",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.width(4.dp))
    TextButton(
        onClick = onSignInClick,
        modifier = Modifier.testTag(SignUpScreenTestTags.LOGIN_NOW_BUTTON),
    ) {
      Text(
          text = "Sign in",
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
        credentialManager = null,
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
