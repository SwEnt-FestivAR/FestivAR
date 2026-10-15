// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.compose.foundation.clickable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.android.festivar.ScreenName
import com.android.festivar.model.authentication.AuthRepository
import com.android.festivar.model.event.EventRepositoryFirebase
import com.android.festivar.ui.event.CreateEventScreen
import com.android.festivar.ui.event.CreateEventViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

/** The destinations of the event feature. */
fun NavGraphBuilder.eventGraph(navController: NavHostController, authRepository: AuthRepository) {
  composable(route = AppScreens.EventOverview.name) {
    val scope = rememberCoroutineScope()
    ScreenName(
        "event overview, click to logout",
        modifier =
            Modifier.clickable(
                role = Role.Button,
                onClick = {
                  scope.launch {
                    authRepository.signOut().onSuccess {
                      navController.navigate(AppScreens.Login.name) {
                        popUpTo(navController.graph.id) { inclusive = true }
                      }
                    }
                  }
                },
            ),
    )
  }
  composable(route = AppScreens.CreateEvent.name) {
    val navigator = CreateEventNavigator(navController)
    CreateEventScreen(
        viewModel = viewModel { CreateEventViewModel(EventRepositoryFirebase(Firebase.firestore)) },
        onBack = navigator.onBack,
    )
  }
  composable(route = AppScreens.EventDetails.name) { ScreenName("event details") }
}
