// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation.profile

import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.navigation.AppScreens
import com.android.festivar.ui.profile.ProfileScreen
import com.android.festivar.ui.profile.ProfileViewModel

/** The destinations of the profile feature. */
fun NavGraphBuilder.profileGraph(navController: NavHostController, dataRepository: DataRepository) {
  composable(route = AppScreens.Profile.name) {
    val navigator = ProfileNavigator(navController)

    // If the uid is null, then no one is connected and consequently you should be at the
    // login screen.
    val uid = dataRepository.authRepository.currentUser?.uid
    if (uid == null) {
      LaunchedEffect(Unit) { navigator.onSignedOut() }
      return@composable
    }

    ProfileScreen(
        profileViewModel =
            viewModel {
              ProfileViewModel(
                  authRepository = dataRepository.authRepository,
                  uId = uid,
              )
            },
        onBack = navigator.onBack,
        onSignedOut = navigator.onSignedOut,
    )
  }
}
