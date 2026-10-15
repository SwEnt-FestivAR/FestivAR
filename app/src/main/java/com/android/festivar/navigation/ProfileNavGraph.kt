// Co-authored-by: Claude Opus 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.ui.profile.ProfileScreen
import com.android.festivar.ui.profile.ProfileViewModel

/** The destinations of the profile feature. */
fun NavGraphBuilder.profileGraph(navController: NavHostController, authRepository: AuthRepository) {
    composable(route = AppScreens.Profile.name) {
        val navigator = ProfileNavigator(navController)
        ProfileScreen(
            profileViewModel =
                viewModel {
                    ProfileViewModel(
                        authRepository = authRepository,
                        uId = authRepository.currentUser?.uid ?: "",
                    )
                },
            onBack = navigator.onBack,
            onSignedOut = navigator.onSignedOut,
        )
    }
}