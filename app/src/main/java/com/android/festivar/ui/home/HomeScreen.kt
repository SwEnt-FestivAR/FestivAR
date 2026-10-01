// Co-authored-by: Claude (Anthropic), through Claude Code as the coding agent.
package com.android.festivar.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

object HomeScreenTestTags {
  const val TITLE = "homeTitle"
  const val USER_NAME = "homeUserName"
  const val SIGN_OUT_BUTTON = "homeSignOutButton"
  const val ERROR_MESSAGE = "homeError"
}

/** First screen after sign-in. Calls [onSignedOut] once the person has signed out. */
@Composable
fun HomeScreen(
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    viewModel: HomeViewModel = viewModel(),
    onSignedOut: () -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsState()
  LaunchedEffect(uiState.signedOut) { if (uiState.signedOut) onSignedOut() }
  HomeContent(uiState) { viewModel.signOut(credentialManager) }
}

/** The stateless screen: everything it shows comes from [state]. */
@Composable
fun HomeContent(state: HomeUiState, onSignOutClick: () -> Unit) {
  Column(
      modifier = Modifier.fillMaxSize().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(modifier = Modifier.weight(1f))
    Text(
        text = stringResource(R.string.app_name),
        style = MaterialTheme.typography.displayMedium,
        modifier = Modifier.testTag(HomeScreenTestTags.TITLE),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.signed_in_as, state.userName),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.testTag(HomeScreenTestTags.USER_NAME),
    )
    Spacer(modifier = Modifier.weight(1f))
    OutlinedButton(
        onClick = onSignOutClick,
        modifier = Modifier.testTag(HomeScreenTestTags.SIGN_OUT_BUTTON),
    ) {
      Text(stringResource(R.string.sign_out))
    }
    state.errorMsg?.let {
      Spacer(modifier = Modifier.height(16.dp))
      Text(
          text = it,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.testTag(HomeScreenTestTags.ERROR_MESSAGE),
      )
    }
    Spacer(modifier = Modifier.weight(1f))
  }
}
