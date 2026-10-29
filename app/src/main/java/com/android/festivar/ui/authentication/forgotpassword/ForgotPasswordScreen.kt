// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.ui.authentication.forgotpassword

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.festivar.R
import com.android.festivar.ui.components.FestivarTextField
import com.android.festivar.ui.theme.AppTheme

internal object ForgotPasswordScreenTestTags {
  const val EMAIL_INPUT = "forgot_password_email_input"
  const val SEND_BUTTON = "send_password_reset_email_button"
  const val RETURN_BUTTON = "return_to_login_button"
  const val SUCCESS_MESSAGE = "password_reset_success_message"
  const val ERROR_MESSAGE = "password_reset_error_message"
}

/** Lets users request a password reset email and return to login. */
@Composable
fun ForgotPasswordScreen(
    onReturnToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ForgotPasswordViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()

  ForgotPasswordScreenContent(
      uiState = uiState,
      onEmailChange = viewModel::updateEmail,
      onSendResetEmail = viewModel::sendPasswordResetEmail,
      onReturnToLogin = onReturnToLogin,
      modifier = modifier,
  )
}

@Composable
private fun ForgotPasswordScreenContent(
    uiState: ForgotPasswordUIState,
    onEmailChange: (String) -> Unit,
    onSendResetEmail: () -> Unit,
    onReturnToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
  Scaffold(modifier = modifier) { padding ->
    Column(
        modifier =
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
          text = stringResource(R.string.forgot_password_prompt),
          color = MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.titleLarge,
      )

      Spacer(modifier = Modifier.height(20.dp))

      FestivarTextField(
          value = uiState.email,
          onValueChange = onEmailChange,
          label = stringResource(R.string.signup_email),
          keyboardOptions =
              KeyboardOptions(
                  capitalization = KeyboardCapitalization.None,
                  keyboardType = KeyboardType.Email,
                  imeAction = ImeAction.Done,
              ),
          modifier = Modifier.height(56.dp).testTag(ForgotPasswordScreenTestTags.EMAIL_INPUT),
      )

      Button(
          onClick = onSendResetEmail,
          enabled =
              uiState.email.isNotBlank() && !uiState.isLoading && !uiState.isRequestSuccessful,
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(0.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          modifier =
              Modifier.fillMaxWidth()
                  .height(44.dp)
                  .testTag(ForgotPasswordScreenTestTags.SEND_BUTTON),
      ) {
        if (uiState.isLoading) {
          CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp,
          )
        } else {
          Text(
              text = stringResource(R.string.forgot_password_send),
              color = MaterialTheme.colorScheme.onPrimary,
              style = MaterialTheme.typography.labelLarge,
          )
        }
      }

      if (uiState.isRequestSuccessful) {
        Text(
            text = stringResource(R.string.forgot_password_success),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(ForgotPasswordScreenTestTags.SUCCESS_MESSAGE),
        )
      }
      uiState.error?.let { error ->
        val message =
            when (error) {
              ForgotPasswordError.EMPTY_EMAIL -> R.string.forgot_password_empty_email
              ForgotPasswordError.REQUEST_FAILED -> R.string.forgot_password_request_failed
            }
        Text(
            text = stringResource(message),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(ForgotPasswordScreenTestTags.ERROR_MESSAGE),
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      TextButton(
          onClick = onReturnToLogin,
          modifier = Modifier.testTag(ForgotPasswordScreenTestTags.RETURN_BUTTON),
      ) {
        Text(
            text = stringResource(R.string.forgot_password_return_to_login),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelLarge,
        )
      }
    }
  }
}

@Preview(showBackground = true, widthDp = 290, heightDp = 638)
@Composable
private fun ForgotPasswordScreenPreview() {
  AppTheme(darkTheme = false) {
    ForgotPasswordScreenContent(
        uiState = ForgotPasswordUIState(email = "user@example.com"),
        onEmailChange = {},
        onSendResetEmail = {},
        onReturnToLogin = {},
    )
  }
}
