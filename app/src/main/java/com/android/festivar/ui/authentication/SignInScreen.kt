// Co-authored-by: Claude (Anthropic), through Claude Code as the coding agent.
package com.android.festivar.ui.authentication

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.festivar.R

object SignInScreenTestTags {
  const val TITLE = "signInTitle"
  const val SIGN_IN_BUTTON = "signInButton"
  const val LOADING = "signInLoading"
  const val ERROR_MESSAGE = "signInError"
}

/**
 * Welcome screen with the Google sign-in button. Once a user is signed in, [onSignedIn] is called
 * so the caller can navigate on.
 */
@Composable
fun SignInScreen(
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    viewModel: SignInViewModel = viewModel(),
    onSignedIn: () -> Unit = {},
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  LaunchedEffect(uiState.user) { if (uiState.user != null) onSignedIn() }
  SignInContent(uiState) { viewModel.signIn(context, credentialManager) }
}

/** The stateless screen: everything it shows comes from [state]. */
@Composable
fun SignInContent(state: SignInUiState, onSignInClick: () -> Unit) {
  Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(modifier = Modifier.weight(2f))
    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.displayMedium,
        modifier = Modifier.testTag(SignInScreenTestTags.TITLE),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.sign_in_welcome),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.weight(2f))
    OutlinedButton(
        onClick = onSignInClick,
        enabled = !state.isLoading,
        shape = CircleShape,
        modifier = Modifier.width(250.dp).testTag(SignInScreenTestTags.SIGN_IN_BUTTON),
    ) {
      Text(stringResource(R.string.sign_in_with_google))
    }
    Spacer(modifier = Modifier.height(16.dp))
    if (state.isLoading) {
      CircularProgressIndicator(modifier = Modifier.testTag(SignInScreenTestTags.LOADING))
    }
    state.errorMsg?.let {
      Text(
          text = it,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.testTag(SignInScreenTestTags.ERROR_MESSAGE),
      )
    }
    Spacer(modifier = Modifier.weight(2f))
  }
}
