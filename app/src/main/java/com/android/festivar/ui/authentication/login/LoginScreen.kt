// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.authentication.login

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.android.festivar.resources.GoogleLogo
import com.android.festivar.ui.theme.AppTheme

internal object LoginScreenTestTags {
  const val TITLE = "login_title"
  const val EMAIL_INPUT = "login_email_input"
  const val PASSWORD_INPUT = "login_password_input"
  const val LOGIN_BUTTON = "login_button"
  const val ERROR_MESSAGE = "login_error"
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = LoginViewModel(),
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSignedIn: () -> Unit = {},
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
) {
  val stableViewModel = remember { viewModel }
  val context = LocalContext.current
  val uiState by stableViewModel.uiState.collectAsState()
  LaunchedEffect(uiState.user) { if (uiState.user != null) onSignedIn() }

  LoginScreenContent(
      uiState = uiState,
      onEmailChange = stableViewModel::updateEmail,
      onPasswordChange = stableViewModel::updatePassword,
      onLoginClick = stableViewModel::signIn,
      onGoogleSignInClick = { stableViewModel.signInWithGoogle(context, credentialManager) },
      onSignUpClick = onSignUpClick,
      modifier = modifier,
  )
}

@Composable
private fun LoginScreenContent(
    uiState: LoginUIState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onGoogleSignInClick: () -> Unit,
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier =
          modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.surface)
              .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
        text = "FestivAR",
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.testTag(LoginScreenTestTags.TITLE),
    )

    Spacer(modifier = Modifier.height(20.dp))

    LoginInputField(
        value = uiState.email,
        label = "Email",
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
        onValueChange = onEmailChange,
        testTag = LoginScreenTestTags.EMAIL_INPUT,
        modifier = Modifier.fillMaxWidth(),
    )
    LoginInputField(
        value = uiState.password,
        label = "Password",
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
        visualTransformation = PasswordVisualTransformation(),
        onValueChange = onPasswordChange,
        testTag = LoginScreenTestTags.PASSWORD_INPUT,
        modifier = Modifier.fillMaxWidth(),
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
      Text(
          text = "Forgot password?",
          color = MaterialTheme.colorScheme.primary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
      )
    }

    uiState.errorMsg?.let { error ->
      Text(
          text = error,
          color = MaterialTheme.colorScheme.error,
          fontSize = 12.sp,
          maxLines = 1,
          modifier = Modifier.testTag(LoginScreenTestTags.ERROR_MESSAGE),
      )
    }

    Button(
        onClick = onLoginClick,
        enabled = !uiState.isLoading,
        shape = RoundedCornerShape(8.dp),
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
            text = "Sign in",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
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
          text = "or",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 12.sp,
          modifier = Modifier.padding(horizontal = 10.dp),
      )
      HorizontalDivider(
          modifier = Modifier.weight(1f),
          thickness = 1.dp,
          color = MaterialTheme.colorScheme.outlineVariant,
      )
    }

    Button(
        onClick = onGoogleSignInClick,
        enabled = !uiState.isLoading,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().height(44.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            imageVector = GoogleLogo,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "Continue with Google",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 8.dp),
        )
      }
    }

    Spacer(modifier = Modifier.weight(1f))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
    ) {
      Text(
          text = "New here? ",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 12.sp,
      )
      Text(
          text = "Create an account",
          color = MaterialTheme.colorScheme.primary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.clickable(role = Role.Button, onClick = onSignUpClick),
      )
    }
  }
}

@Composable
private fun LoginInputField(
    value: String,
    label: String,
    keyboardOptions: KeyboardOptions,
    onValueChange: (String) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
  OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      singleLine = true,
      keyboardOptions = keyboardOptions,
      visualTransformation = visualTransformation,
      label = { Text(label, fontSize = 12.sp) },
      textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
      shape = RoundedCornerShape(8.dp),
      modifier = modifier.height(56.dp).testTag(testTag),
  )
}

@Preview(showBackground = true, widthDp = 290, heightDp = 638)
@Composable
private fun LoginScreenPreview() {
  AppTheme(darkTheme = false) {
    LoginScreenContent(
        uiState = LoginUIState(),
        onEmailChange = {},
        onPasswordChange = {},
        onLoginClick = {},
        onGoogleSignInClick = {},
        onSignUpClick = {},
    )
  }
}
