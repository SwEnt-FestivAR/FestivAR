package com.android.festivar.ui.authentication.signup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.festivar.resources.GoogleLogo

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
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onCreateAccountClick: () -> Unit = {},
    signUpViewModel: SignUpViewModel = viewModel(),
) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPass by remember { mutableStateOf("") }

  val context = LocalContext.current
  val uiState by signUpViewModel.uiState.collectAsState()

  Column(
      modifier =
          modifier
              .fillMaxSize()
              .background(color = MaterialTheme.colorScheme.background)
              .padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
      Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
            text = "Create account",
            fontSize = 45.sp,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier =
                Modifier.padding(bottom = 30.dp).testTag(SignUpScreenTestTags.CREATE_ACC_TITLE),
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth().testTag(SignUpScreenTestTags.EMAIL_FIELD),
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth().testTag(SignUpScreenTestTags.PASS_FIELD),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPass,
            onValueChange = { confirmPass = it },
            modifier = Modifier.fillMaxWidth().testTag(SignUpScreenTestTags.PASS_CONFIRM_FIELD),
            label = { Text("Confirm password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        if (confirmPass.isNotEmpty() && password != confirmPass) {
          Text(
              text = "Passwords do not match",
              color = Color.Red,
              modifier = Modifier.testTag(SignUpScreenTestTags.PASSWORD_MISMATCH_ERROR),
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
              if (password == confirmPass) {
                signUpViewModel.signUp(email, password)
              }
            },
            modifier = Modifier.fillMaxWidth().testTag(SignUpScreenTestTags.SIGNUP_BUTTON),
            enabled = email.isNotBlank() && password.isNotBlank() && confirmPass.isNotBlank(),
        ) {
          Text(if (uiState.isLoading) "Signing up..." else "Sign up")
        }
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          HorizontalDivider(
              modifier = Modifier.weight(1f),
              color = MaterialTheme.colorScheme.outlineVariant,
          )
          Text(
              text = "Or sign up with",
              modifier = Modifier.padding(horizontal = 12.dp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          HorizontalDivider(
              modifier = Modifier.weight(1f),
              color = MaterialTheme.colorScheme.outlineVariant,
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { signUpViewModel.googleSignUp(context, credentialManager) },
            modifier = Modifier.testTag(SignUpScreenTestTags.GOOGLE_SIGNUP_BUTTON),
            shape = RoundedCornerShape(10.dp),
        ) {
          Image(
              imageVector = GoogleLogo,
              contentDescription = "Google Logo",
              modifier = Modifier.size(30.dp),
          )
        }
      }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      Text("New here?", color = MaterialTheme.colorScheme.onBackground)
      Spacer(modifier = Modifier.width(4.dp))
      TextButton(
          onClick = onCreateAccountClick,
          modifier = Modifier.testTag(SignUpScreenTestTags.LOGIN_NOW_BUTTON),
      ) {
        Text("Create account")
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
  MaterialTheme { SignUpScreen() }
}
