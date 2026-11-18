// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation.profile

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.android.festivar.model.datarepository.DataRepository
import com.android.festivar.navigation.AppScreens
import com.android.festivar.navigation.RequireSignedIn
import com.android.festivar.ui.profile.ProfileScreen
import com.android.festivar.ui.profile.ProfileViewModel

/** The destinations of the profile feature. */
fun NavGraphBuilder.profileGraph(navController: NavHostController, dataRepository: DataRepository) {
  composable(route = AppScreens.Profile.name) {
    val navigator = ProfileNavigator(navController)
    RequireSignedIn(navController, dataRepository) { firebaseUser ->
      ProfileScreen(
          profileViewModel = viewModel { ProfileViewModel(dataRepository, uId = firebaseUser.uid) },
          onBack = navigator.onBack,
          onSignedOut = navigator.onSignedOut,
          onSettingsClick = navigator.onSettingsClick,
      )
    }
  }
}
