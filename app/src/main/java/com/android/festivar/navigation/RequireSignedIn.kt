// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import com.android.festivar.model.datarepository.DataRepository
import com.google.firebase.auth.FirebaseUser

/**
 * Guarantees a signed-in user to the routes that need one, so they never check for `null`
 * themselves.
 *
 * When somebody is signed in, [content] is drawn with that user. When nobody is, nothing is drawn
 * and the login opens with the back stack cleared, so back cannot return to a screen that needs a
 * user. The gate follows the auth state, so a sign-out while the route is open is noticed too.
 *
 * @param navController the controller that opens the login.
 * @param dataRepository gives the signed-in user and its changes.
 * @param content the route, which receives the signed-in user.
 */
@Composable
fun RequireSignedIn(
    navController: NavHostController,
    dataRepository: DataRepository,
    content: @Composable (firebaseUser: FirebaseUser) -> Unit,
) {
  val authRepository = dataRepository.authRepository
  // Cold flow: collecting starts when the route is drawn and is cancelled when it leaves the
  // composition. Reading the state below makes the gate recompose on each change.
  val currentUser =
      authRepository.currentUserFlow.collectAsState(initial = authRepository.currentUser)
  // One read per composition, into a local so the null check below smart-casts: `.value` could
  // change between two reads. A later change recomposes the gate and takes a new snapshot.
  val firebaseUser = currentUser.value
  if (firebaseUser == null) {
    LaunchedEffect(Unit) {
      navController.navigate(AppScreens.Login.name) {
        popUpTo(navController.graph.id) { inclusive = true }
      }
    }
  } else {
    // Not drawn once the user is null, so the route and its state are disposed on sign-out.
    content(firebaseUser)
  }
}
