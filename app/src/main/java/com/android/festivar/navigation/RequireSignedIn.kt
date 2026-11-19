// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import com.android.festivar.model.datarepository.DataRepository
import com.google.firebase.auth.FirebaseUser

/**
 * Guarantees a signed-in user to the routes that need one, so they never check for `null`
 * themselves.
 *
 * When somebody is signed in, [content] is drawn with that user. When nobody is, nothing is drawn
 * and the login opens with the back stack cleared, so back cannot return to a screen that needs a
 * user.
 *
 * @param navController the controller that opens the login.
 * @param dataRepository gives the signed-in user.
 * @param content the route, which receives the signed-in user.
 */
@Composable
fun RequireSignedIn(
    navController: NavHostController,
    dataRepository: DataRepository,
    content: @Composable (firebaseUser: FirebaseUser) -> Unit,
) {
  val firebaseUser = dataRepository.authRepository.currentUser
  if (firebaseUser == null) {
    LaunchedEffect(Unit) {
      navController.navigate(AppScreens.Login.name) {
        popUpTo(navController.graph.id) { inclusive = true }
      }
    }
  } else {
    content(firebaseUser)
  }
}
